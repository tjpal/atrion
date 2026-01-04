package dev.tjpal.prompt

import dev.tjpal.config.Config
import dev.tjpal.filesystem.DefaultFileSystemService
import dev.tjpal.filesystem.FileSystemService
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FileSystemPromptsRepositoryTest {
    private lateinit var tmpDir: Path

    @BeforeTest
    fun setup() {
        tmpDir = Files.createTempDirectory("prompts-test")
    }

    @AfterTest
    fun teardown() {
        try {
            tmpDir.toFile().deleteRecursively()
        } catch (_: Exception) {}
    }

    @Test
    fun testGetPromptReadsFreshEachCall() {
        val config = mockk<Config>(relaxed = true)
        every { config.storageDirectory } returns tmpDir.toString()

        val fsMock = mockk<FileSystemService>(relaxed = true)

        val promptsDir = tmpDir.resolve("prompts")

        every { fsMock.getPath(config.storageDirectory, "prompts") } returns promptsDir
        every { fsMock.createDirectories(promptsDir) } answers { Files.createDirectories(promptsDir) }

        val promptFile = promptsDir.resolve("my_prompt")

        every { fsMock.exists(any()) } returns true
        every { fsMock.readString(any()) } returnsMany listOf("first-content", "second-content")
        every { fsMock.isDirectory(promptsDir) } returns true
        every { fsMock.list(promptsDir) } returns listOf(promptFile)

        val repo = FileSystemPromptsRepository(config, fsMock)

        val first = repo.getPrompt("my_prompt")
        assertEquals("first-content", first)

        val second = repo.getPrompt("my_prompt")
        assertEquals("second-content", second)

        verify(exactly = 2) { fsMock.readString(any()) }
    }

    @Test
    fun testGetPromptNotFoundThrows() {
        val config = mockk<Config>(relaxed = true)
        every { config.storageDirectory } returns tmpDir.toString()

        val fsMock = mockk<FileSystemService>(relaxed = true)
        val promptsDir = tmpDir.resolve("prompts")

        every { fsMock.getPath(config.storageDirectory, "prompts") } returns promptsDir
        every { fsMock.createDirectories(promptsDir) } answers { Files.createDirectories(promptsDir) }

        val missing = promptsDir.resolve("does_not_exist")
        every { fsMock.exists(missing) } returns false

        val repo = FileSystemPromptsRepository(config, fsMock)

        assertFailsWith<IllegalArgumentException> {
            repo.getPrompt("does_not_exist")
        }
    }

    @Test
    fun testPutListAndDelete() {
        val config = mockk<Config>(relaxed = true)
        every { config.storageDirectory } returns tmpDir.toString()

        val realFs = DefaultFileSystemService()
        val fsSpy = spyk(realFs)

        val repo = FileSystemPromptsRepository(config, fsSpy)

        val storedName = repo.putPrompt("greeting-1", "Hello World")
        assertTrue(storedName.isNotBlank())

        val promptsDir = Paths.get(config.storageDirectory, "prompts")
        val expectedPath = promptsDir.resolve(storedName)

        assertTrue(Files.exists(expectedPath))

        val names = repo.listPromptNames()
        assertTrue(names.contains(storedName))
        assertTrue(repo.exists(storedName))

        val content = repo.getPrompt(storedName)
        assertEquals("Hello World", content)

        val deleted = repo.deletePrompt(storedName)
        assertTrue(deleted)
        assertFalse(repo.exists(storedName))

        verify { fsSpy.write(expectedPath, any()) }
    }
}
