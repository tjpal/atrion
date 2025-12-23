package dev.tjpal.nodes

import dev.tjpal.model.ConnectorDefinition
import dev.tjpal.model.ConnectorSchema
import dev.tjpal.model.NodeDefinition
import dev.tjpal.model.NodeParameters
import dev.tjpal.model.NodeType
import dev.tjpal.tools.HelloWorldTool
import dev.tjpal.tools.ToolInfo
import dev.tjpal.tools.ToolRegistry
import dev.tjpal.utilities.ImageResourceEncoder
import javax.inject.Inject

class HelloWorldToolNodeFactory @Inject constructor(
    private val toolRegistry: ToolRegistry
) : NodeFactory {

    private val toolName = "HelloWorld Tool"

    override fun definition(): NodeDefinition {
        val resourceEncoder = ImageResourceEncoder()

        return NodeDefinition(
            name = toolName,
            type = NodeType.TOOL,
            category = "Tool",
            description = "For test purpose",
            icon = resourceEncoder.encodeResourceToBase64("placeholder-1.png"),
            inputConnectors = emptyList(),
            outputConnectors = emptyList(),
            toolConnectors =  listOf(ConnectorDefinition(id = "tool", label = "Tool", schema = ConnectorSchema.JSON)),
            debugConnectors = emptyList(),
            parameters = emptyList()
        )
    }

    override fun createNode(parameters: NodeParameters): Node {
        return object : Node {
            override fun onActivate(context: NodeActivationContext) {}
            override suspend fun onEvent(context: NodeInvocationContext, output: NodeOutput) {}
            override fun onStop(context: NodeDeactivationContext) {}
        }
    }

    init {
        val info = ToolInfo(definitionName = toolName, toolClass = HelloWorldTool::class)

        toolRegistry.register(toolName, info)
    }
}
