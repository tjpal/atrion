package dev.tjpal.nodes.memory

import dev.tjpal.ai.tools.ToolRegistry
import dev.tjpal.model.ConnectorDefinition
import dev.tjpal.model.NodeDefinition
import dev.tjpal.model.NodeParameters
import dev.tjpal.model.NodeType
import dev.tjpal.nodes.Node
import dev.tjpal.nodes.NodeActivationContext
import dev.tjpal.nodes.NodeDeactivationContext
import dev.tjpal.nodes.NodeFactory
import dev.tjpal.nodes.NodeInvocationContext
import dev.tjpal.tools.memory.MemoryStore
import dev.tjpal.tools.memory.MemoryToolFactory
import dev.tjpal.utilities.ImageResourceEncoder
import javax.inject.Inject

class MemoryNodeFactory @Inject constructor(
    private val toolRegistry: ToolRegistry,
    private val memoryStore: MemoryStore,
) : NodeFactory {
    private val toolName = "MemoryTool"

    override fun definition(): NodeDefinition {
        val encoder = ImageResourceEncoder()

        return NodeDefinition(
            id = "MemoryTool",
            displayedName = "Memory",
            type = NodeType.TOOL,
            category = "Tool",
            description = "Tool for persisting small text records per graph/node and retrieving them (JSONL format).",
            icon = encoder.encodeResourceToBase64("memory-tool.png"),
            inputConnectors = emptyList(),
            outputConnectors = emptyList(),
            toolConnectors = listOf(ConnectorDefinition(id = "tool", label = "Tool")),
            debugConnectors = emptyList(),
            parameters = emptyList()
        )
    }

    override fun createNode(parameters: NodeParameters): Node {
        return object : Node {
            override fun onActivate(context: NodeActivationContext) {}
            override suspend fun onEvent(context: NodeInvocationContext, output: dev.tjpal.nodes.NodeOutput) {}
            override fun onStop(context: NodeDeactivationContext) {}
        }
    }

    init {
        toolRegistry.registerFactory(toolName, MemoryToolFactory(memoryStore))
    }
}
