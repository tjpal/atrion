package dev.tjpal.tools.memory

import dev.tjpal.config.Config
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant
import javax.inject.Inject
import kotlin.io.path.exists

/**
 * Simple file-backed JSONL memory store. Files are located under the provided storageDirectory.
 * Each memory file is named: memory_<sanitizedGraphId>_<sanitizedNodeId>.jsonl
 */
class MemoryStore @Inject constructor(private val config: Config) {
    private val json: Json = Json { ignoreUnknownKeys = true }

    init {
        try {
            val directory = Paths.get(config.nodeMemoryDirectory)

            if (!Files.exists(directory)) {
                Files.createDirectories(directory)
            }
        } catch (e: Exception) {
            // Not much we can do. Just prevent a crash.
        }
    }

    private fun resolvePath(graphInstanceId: String, nodeId: String): Path {
        val fileName = "memory_${graphInstanceId}_${nodeId}.json"

        return Paths.get(config.nodeMemoryDirectory, fileName)
    }

    @Serializable
    private data class MemoryRecord(val timestamp: Long, val value: String)

    /**
     * Append a new record to the memory file. The provided text is stored as the `value` field
     * of a small wrapper JSON object with timestamp.
     */
    @Synchronized
    fun append(graphInstanceId: String, nodeId: String, text: String) {
        val path = resolvePath(graphInstanceId, nodeId)

        val record = MemoryRecord(timestamp = Instant.now().toEpochMilli(), value = text)
        val updatedList = readAllEntries(path) + record

        path.toFile().writeText(json.encodeToString(updatedList))
    }

    /**
     * Read the full content of the memory file and return it as a single string.
     * If the file does not exist, returns an empty string.
     */
    @Synchronized
    fun readAll(graphInstanceId: String, nodeId: String): String {
        val path = resolvePath(graphInstanceId, nodeId)

        if (!Files.exists(path)) {
            return ""
        }

        return readAllEntries(path).joinToString("\n") {
            it.value
        }
    }

    private fun readAllEntries(file: Path): List<MemoryRecord> {
        if(file.exists().not()) {
            return emptyList()
        }

        val jsonText = file.toFile().readText()
        val result: List<MemoryRecord> = json.decodeFromString(jsonText)

        return result
    }
}
