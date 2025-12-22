package dev.tjpal.nodes

import dev.tjpal.graph.hooks.RestInputRegistry
import dev.tjpal.graph.status.StatusRegistry
import dev.tjpal.model.ConnectorDefinition
import dev.tjpal.model.ConnectorSchema
import dev.tjpal.model.NodeDefinition
import dev.tjpal.model.NodeType
import dev.tjpal.model.NodeParameters
import dev.tjpal.utilities.ImageResourceEncoder
import javax.inject.Inject

class RestInputNodeFactory @Inject constructor(
    private val restInputRegistry: RestInputRegistry,
    private val statusRegistry: StatusRegistry
) : NodeFactory {
    override fun definition(): NodeDefinition {
        val imageEncoder = ImageResourceEncoder()

        return NodeDefinition(
            name = "REST Input",
            type = NodeType.INPUT,
            category = "Input",
            description = "Node that registers for REST inputs",
            icon = imageEncoder.encodeResourceToBase64("placeholder-1.png"),
            inputConnectors = emptyList(),
            outputConnectors = listOf(ConnectorDefinition(id = "out", label = "Out", schema = ConnectorSchema.TEXT)),
            toolConnectors = emptyList(),
            debugConnectors = emptyList(),
            parameters = emptyList()
        )
    }

    override fun createNode(parameters: NodeParameters): Node {
        return RestInputNode(restInputRegistry, parameters, statusRegistry)
    }
}
