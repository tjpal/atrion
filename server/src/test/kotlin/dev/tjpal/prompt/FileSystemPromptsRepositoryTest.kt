package dev.tjpal.prompt

import dev.tjpal.config.Config
import dev.tjpal.filesystem.FileSystemService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

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
        val fsMock = mockk<FileSystemService>(relaxed = true)
        val promptsDir = tmpDir.resolve("prompts")

        val config = mockk<Config>(relaxed = true)
        every { config.promptDirectories } returns listOf(promptsDir.toString())

        every { fsMock.getPath(promptsDir.toString())} returns promptsDir

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
        val fsMock = mockk<FileSystemService>(relaxed = true)
        val promptsDir = tmpDir.resolve("prompts")

        val config = mockk<Config>(relaxed = true)
        every { config.promptDirectories } returns listOf(promptsDir.toString())

        every { fsMock.getPath(promptsDir.toString()) } returns promptsDir
        every { fsMock.createDirectories(promptsDir) } answers { Files.createDirectories(promptsDir) }

        val missing = promptsDir.resolve("does_not_exist")
        every { fsMock.exists(missing) } returns false

        val repo = FileSystemPromptsRepository(config, fsMock)

        assertFailsWith<IllegalArgumentException> {
            repo.getPrompt("does_not_exist")
        }
    }
}
