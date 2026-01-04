package dev.tjpal.prompt

import dev.tjpal.config.Config
import dev.tjpal.filesystem.FileSystemService
import dev.tjpal.logging.logger
import java.nio.file.Files
import java.nio.file.Path
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileSystemPromptsRepository @Inject constructor(
    private val config: Config,
    private val fileSystemService: FileSystemService
) : PromptsRepository {
    private val logger = logger<FileSystemPromptsRepository>()

    private val promptsDir: Path = fileSystemService.getPath(config.storageDirectory, "prompts")

    init {
        try {
            fileSystemService.createDirectories(promptsDir)
            trySetPosixPermissions(promptsDir)
        } catch (e: Exception) {
            logger.error("Failed to ensure prompts directory exists: {}", e.message)
            throw IllegalStateException("Failed to ensure prompts directory exists: ${config.storageDirectory}/prompts", e)
        }

        logger.info("FileSystemPromptsRepository initialized promptsDir={}", promptsDir)
    }

    private fun trySetPosixPermissions(path: Path) {
        try {
            val perms = java.nio.file.attribute.PosixFilePermissions.fromString("rwx------")
            Files.setPosixFilePermissions(path, perms)
        } catch (e: UnsupportedOperationException) {
            // ignore on non-posix platforms
        } catch (e: Exception) {
            logger.debug("Could not set POSIX permissions for {}: {}", path, e.message)
        }
    }

    private fun sanitizeName(name: String): String {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "Prompt name must not be empty" }

        val sanitized = trimmed.replace(Regex("[^A-Za-z0-9._-]"), "_").trim { it == '.' }
        require(sanitized.isNotEmpty()) { "Prompt name must contain at least one allowed character" }

        return sanitized
    }

    private fun filePathForName(name: String): Path {
        val sanitized = sanitizeName(name)
        return promptsDir.resolve(sanitized)
    }

    override fun listPromptNames(): List<String> {
        return try {
            if (!fileSystemService.exists(promptsDir) || !fileSystemService.isDirectory(promptsDir)) return emptyList()

            fileSystemService.list(promptsDir).map { it.fileName.toString() }
        } catch (e: Exception) {
            logger.error("Failed to list prompts: {}", e.message)
            emptyList()
        }
    }

    override fun getPrompt(name: String): String {
        val path = filePathForName(name)
        try {
            if (!fileSystemService.exists(path)) {
                throw IllegalArgumentException("No prompt with name: $name")
            }

            return fileSystemService.readString(path)
        } catch (e: IllegalArgumentException) {
            throw e
        } catch (e: Exception) {
            logger.error("Failed to read prompt {}: {}", name, e.message)
            throw IllegalStateException("Failed to read prompt: $name: ${e.message}", e)
        }
    }

    override fun putPrompt(name: String, content: String): String {
        val path = filePathForName(name)

        try {
            val bytes = content.toByteArray(Charsets.UTF_8)
            fileSystemService.write(path, bytes)
            trySetPosixPermissions(path)
            logger.info("Stored prompt {} at {}", name, path)
            return path.fileName.toString()
        } catch (e: Exception) {
            logger.error("Failed to store prompt {}: {}", name, e.message)
            throw IllegalStateException("Failed to store prompt: $name: ${e.message}", e)
        }
    }

    override fun deletePrompt(name: String): Boolean {
        val path = filePathForName(name)
        try {
            return fileSystemService.deleteIfExists(path)
        } catch (e: Exception) {
            logger.error("Failed to delete prompt {}: {}", name, e.message)
            throw IllegalStateException("Failed to delete prompt: $name: ${e.message}", e)
        }
    }

    override fun exists(name: String): Boolean {
        val path = filePathForName(name)
        return try {
            fileSystemService.exists(path)
        } catch (e: Exception) {
            logger.error("Failed to check existence for prompt {}: {}", name, e.message)
            false
        }
    }
}
