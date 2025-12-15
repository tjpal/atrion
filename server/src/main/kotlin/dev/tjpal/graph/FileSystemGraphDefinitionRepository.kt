package dev.tjpal.graph

import dev.tjpal.config.Config
import dev.tjpal.filesystem.FileSystemService
import dev.tjpal.logging.logger
import dev.tjpal.model.GraphDefinition
import kotlinx.serialization.json.Json
import java.nio.file.Path
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileSystemGraphDefinitionRepository @Inject constructor(
    config: Config,
    private val json: Json,
    private val fileSystemService: FileSystemService
) : GraphDefinitionRepository {
    private val logger = logger<FileSystemGraphDefinitionRepository>()
    private val storagePath: Path = fileSystemService.getPath(config.storageDirectory)

    init {
        try {
            fileSystemService.createDirectories(storagePath)
        } catch (exception: Exception) {
            throw IllegalStateException("Failed to create storage directory at ${config.storageDirectory}", exception)
        }
    }

    private fun filePathForId(identifier: String): Path = storagePath.resolve("$identifier.json")

    override fun add(graph: GraphDefinition): String {
        val generatedId = UUID.randomUUID().toString()
        val toStore = graph.copy(id = generatedId)
        val path = filePathForId(generatedId)
        val serialized = json.encodeToString(toStore)
        fileSystemService.write(path, serialized.toByteArray())
        return generatedId
    }

    override fun get(id: String): GraphDefinition {
        val path = filePathForId(id)
        if (!fileSystemService.exists(path)) {
            throw IllegalArgumentException("No graph with id: $id")
        }
        val text = fileSystemService.readString(path)
        return json.decodeFromString(text)
    }

    override fun getAll(): List<GraphDefinition> {
        if (!fileSystemService.exists(storagePath) || !fileSystemService.isDirectory(storagePath)) {
            return emptyList()
        }
        return fileSystemService.list(storagePath)
            .filter { path -> path.fileName.toString().endsWith(".json") }
            .mapNotNull { path ->
                try {
                    val text = fileSystemService.readString(path)
                    json.decodeFromString<GraphDefinition>(text)
                } catch (exception: Exception) {
                    logger.error("FileSystemGraphDefinitionRepository: failed to read/parse ${path.fileName}", exception)
                    null
                }
            }
    }

    override fun delete(id: String) {
        val path = filePathForId(id)
        try {
            val deleted = fileSystemService.deleteIfExists(path)
            if (!deleted) {
                throw IllegalArgumentException("No graph with id: $id")
            }
        } catch (exception: Exception) {
            throw IllegalStateException("Failed to delete graph file for id $id: ${exception.message}", exception)
        }
    }

    override fun replace(id: String, graph: GraphDefinition) {
        val path = filePathForId(id)
        if (!fileSystemService.exists(path)) {
            throw IllegalArgumentException("No graph with id: $id")
        }
        val serialized = json.encodeToString(graph.copy(id = id))
        fileSystemService.write(path, serialized.toByteArray())
    }
}
