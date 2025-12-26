package dev.tjpal.secrets

import dev.tjpal.config.Config
import dev.tjpal.filesystem.FileSystemService
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Base64
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FileSystemEncryptedSecretStoreTest {
    private lateinit var tempDir: Path
    private lateinit var masterKeyPath: Path

    private val config = mockk<Config>(relaxed = true)
    private val fileSystemService = mockk<FileSystemService>(relaxed = true)
    private val json = Json { encodeDefaults = true }

    @BeforeTest
    fun setup() {
        tempDir = Files.createTempDirectory("secrets-test")
        masterKeyPath = tempDir.resolve("master.key")

        // Create a deterministic 32-byte master key and write it base64-encoded to the master key path
        val masterKeyBytes = ByteArray(32) { i -> (i and 0xFF).toByte() }
        val masterKeyBase64 = Base64.getEncoder().encodeToString(masterKeyBytes)
        Files.writeString(masterKeyPath, masterKeyBase64)

        every { config.secretsDirectory } returns tempDir.toString()
        every { config.secretsMasterKeyPath } returns masterKeyPath.toString()

        every { fileSystemService.getPath(any()) } answers { Paths.get(firstArg<String>()) }

        every { fileSystemService.createDirectories(any()) } answers {
            Files.createDirectories(firstArg<Path>())
            Unit
        }

        every { fileSystemService.exists(any()) } answers { Files.exists(firstArg<Path>()) }
        every { fileSystemService.isDirectory(any()) } answers { Files.isDirectory(firstArg<Path>()) }
        every { fileSystemService.readString(any()) } answers { Files.readString(firstArg<Path>()) }
        every { fileSystemService.write(any(), any()) } answers {
            val path = firstArg<Path>()
            val bytes = secondArg<ByteArray>()
            Files.write(path, bytes)
            Unit
        }
        every { fileSystemService.deleteIfExists(any()) } answers { Files.deleteIfExists(firstArg<Path>()) }
        every { fileSystemService.list(any()) } answers { Files.list(firstArg<Path>()).toList() }
    }

    @AfterTest
    fun tearDown() {
        try {
            Files.walk(tempDir)
                .sorted(java.util.Comparator.reverseOrder())
                .forEach { Files.deleteIfExists(it) }
        } catch (e: Exception) {
            // ignore cleanup errors in tests
        }
    }

    @Test
    fun testPutGetRoundTrip() {
        val store = FileSystemEncryptedSecretStore(config, json, fileSystemService)

        val plaintext = "{\"foo\":\"bar\"}"
        val id = store.put(null, "my-secret", "json", plaintext, listOf("tag1"))

        val got = store.get(id)
        assertEquals(plaintext, got)

        // ensure a file was created for the id
        val filePath = tempDir.resolve("${id}.json")
        assert(Files.exists(filePath))

        val fileContent = Files.readString(filePath)
        // persisted record should contain ciphertext and iv fields (encrypted data)
        assert(fileContent.contains("ciphertext"))
        assert(fileContent.contains("iv"))
        // crude check: file content should not contain the plaintext as-is
        assert(!fileContent.contains(plaintext))
    }

    @Test
    fun testReplaceIncrementsVersion() {
        val store = FileSystemEncryptedSecretStore(config, json, fileSystemService)

        val initial = "{\"value\":1}"
        val id = store.put(null, "counter", "json", initial, emptyList())

        val metadataBefore = store.list().first { it.id == id }
        assertEquals(1, metadataBefore.version)

        val replaced = "{\"value\":2}"
        store.replace(id, replaced)

        // get should return updated plaintext
        val got = store.get(id)
        assertEquals(replaced, got)

        val metadataAfter = store.list().first { it.id == id }
        assertEquals(2, metadataAfter.version)
        assert(metadataAfter.updatedAt >= metadataBefore.updatedAt)
    }

    @Test
    fun testDeleteRemovesSecret() {
        val store = FileSystemEncryptedSecretStore(config, json, fileSystemService)

        val plaintext = "{\"secret\":true}"
        val id = store.put(null, "to-delete", "json", plaintext, emptyList())

        assertEquals(plaintext, store.get(id))

        store.delete(id)

        // subsequent get should fail
        assertFailsWith<IllegalArgumentException> { store.get(id) }

        // delete again should also fail
        assertFailsWith<IllegalArgumentException> { store.delete(id) }
    }

    @Test
    fun testListIgnoresMalformedFiles() {
        // Prepare a valid and some invalid files in the secrets directory
        val store = FileSystemEncryptedSecretStore(config, json, fileSystemService)

        val goodPlain = "{\"ok\":true}"
        val goodId = store.put(null, "good", "json", goodPlain, emptyList())

        // Create a non-json file and a malformed json file
        val stray = tempDir.resolve("notes.txt")
        Files.writeString(stray, "just some notes")

        val malformed = tempDir.resolve("malformed.json")
        Files.writeString(malformed, "{ this is : not valid json }")

        val listed = store.list()
        // only the valid secret should be present
        assert(listed.any { it.id == goodId })
        // malformed and stray files should not produce entries
        assert(listed.none { it.name == "notes" })
    }

    @Test
    fun testGetMissingSecretThrows() {
        val store = FileSystemEncryptedSecretStore(config, json, fileSystemService)
        assertFailsWith<IllegalArgumentException> { store.get("non-existent-id") }
    }
}
