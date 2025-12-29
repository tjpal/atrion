package dev.tjpal.tools.memory

import dev.tjpal.ai.tools.Tool
import dev.tjpal.ai.tools.ToolFactory
import dev.tjpal.logging.logger
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.reflect.KClass

class MemoryToolFactory(private val memoryStore: MemoryStore) : ToolFactory {
    private val logger = logger<MemoryToolFactory>()
    private val json = Json {}

    override val toolClass: KClass<out Tool> = MemoryTool::class

    override fun create(arguments: JsonElement?, nodeParameters: JsonElement?): Tool {
        if(arguments == null) {
            throw IllegalArgumentException("Arguments required")
        }

        val nodeParameters = nodeParameters?.jsonObject ?: throw IllegalArgumentException("nodeParameters required")
        val graphInstanceId = nodeParameters["graphInstanceId"]?.jsonPrimitive?.content ?: throw IllegalArgumentException("graphInstanceId parameter required")
        val nodeId = nodeParameters["nodeId"]?.jsonPrimitive?.content ?: throw IllegalArgumentException("nodeId parameter required")

        val toolInstance = try {
            json.decodeFromJsonElement(MemoryTool.serializer(), arguments)
        } catch (e: Exception) {
            logger.error("Failed to decode JiraTool arguments: {}", e.message)
            throw IllegalArgumentException("Invalid arguments for JiraTool: ${e.message}")
        }

        toolInstance.graphInstanceId = graphInstanceId
        toolInstance.nodeId = nodeId
        toolInstance.memoryStore = memoryStore

        return toolInstance
    }
}