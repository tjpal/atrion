package dev.tjpal.prompt

import dev.tjpal.config.Config
import dev.tjpal.filesystem.FileSystemService
import dev.tjpal.logging.logger
import java.io.File
import java.nio.file.Paths
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileSystemPromptsRepository @Inject constructor(
    private val config: Config,
    private val fileSystemService: FileSystemService
) : PromptsRepository {
    private val logger = logger<FileSystemPromptsRepository>()

    private fun listPromptFiles(): Map<String, File> {
         return config.promptDirectories
             .asSequence()
             .map { directory -> fileSystemService.list(Paths.get(directory)) }
             .flatten()
             .map { path -> path.toFile() }
             .associateBy { file -> file.name }
    }

    override fun listPromptNames(): List<String> {
        return listPromptFiles().keys.toList()
    }

    override fun getPrompt(name: String): String {
        val promptFiles = listPromptFiles()
        val promptFile = promptFiles[name] ?: throw IllegalArgumentException("No prompt with name: $name")

        return fileSystemService.readString(promptFile.toPath())
    }

    override fun exists(name: String): Boolean {
        val promptFiles = listPromptFiles()

        return promptFiles.containsKey(name)
    }
}
