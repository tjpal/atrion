package dev.tjpal.tools.memory

import dev.tjpal.config.Config
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MemoryStoreTest {
    private lateinit var tempDir: Path
    private val json = Json { ignoreUnknownKeys = true }

    @BeforeTest
    fun setup() {
        tempDir = Files.createTempDirectory("memory-store-test")
    }

    @AfterTest
    fun teardown() {
        try { Files.walk(tempDir).sorted(reverseOrder()).forEach { Files.deleteIfExists(it) } } catch (_: Exception) {}
    }

    @Test
    fun testAppendAndReadReturnsJsonLines() {
        val mockConfig = mockk<Config>()
        every { mockConfig.nodeMemoryDirectory } returns tempDir.toString()

        val store = MemoryStore(mockConfig)

        val graphId = "1234-5678"
        val nodeId = "node1"
        val firstEntry = "first entry"
        val secondEntry = "second entry"

        store.append(graphId, nodeId, firstEntry)
        store.append(graphId, nodeId, secondEntry)

        val contents = store.readAll(graphId, nodeId)
        val lines = contents.split('\n').filter { it.isNotBlank() }
        assertEquals(2, lines.size, "Should contain two JSON lines")

        assertEquals(firstEntry, lines[0])
        assertEquals(secondEntry, lines[1])
    }
}
