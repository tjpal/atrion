package dev.tjpal.tools.memory

import com.fasterxml.jackson.annotation.JsonClassDescription
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonPropertyDescription
import dev.tjpal.ai.tools.Tool
import dev.tjpal.logging.logger
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json

/**
 * The MemoryTool is a simple tool that appends text to a per-graph/node JSONL file or reads the
 * entire file. It is created by MemoryToolFactory with decoded arguments.
 */
@JsonClassDescription(
    """
        Tool for persisting text records per graph/node and retrieving them. Supported actions: 'append' to add text, 'read' to retrieve all text.
        Use this tool only when explicitly stated in the prompt. There will be explicit instruction on when and how to use it.
        """)
@Serializable
class MemoryTool(
    @JsonPropertyDescription("Action to perform. Supported values: 'append' to add text, 'read' to retrieve all text.")
    val action: String,
    @JsonPropertyDescription("Text to append when action is 'append'. Ignored for 'read' action.")
    val text: String? = null
) : Tool {
    private val logger = logger<MemoryTool>()

    @JsonIgnore
    @Transient
    var graphInstanceId: String = ""

    @JsonIgnore
    @Transient
    var nodeId: String = ""

    @JsonIgnore
    @Transient
    private val json = Json { prettyPrint = true }

    @JsonIgnore
    @Transient
    var memoryStore: MemoryStore? = null

    override fun execute(): String {
        val localStore = memoryStore ?: throw IllegalStateException("MemoryStore not configured on tool instance")

        return try {
            when (action.lowercase()) {
                "append" -> {
                    val txt = text ?: return "Missing 'text' for append action"
                    localStore.append(graphInstanceId, nodeId, txt)
                    "OK"
                }
                "read" -> {
                    localStore.readAll(graphInstanceId, nodeId)
                }
                else -> {
                    "Unsupported action: $action"
                }
            }
        } catch (e: Exception) {
            logger.error("MemoryTool.execute error: {}", e.message)
            "An exception/error occurred: ${e.message}"
        }
    }
}