package dev.tjpal.nodes

import dev.tjpal.graph.status.StatusRegistry
import dev.tjpal.model.ConnectorDefinition
import dev.tjpal.model.ConnectorSchema
import dev.tjpal.model.NodeDefinition
import dev.tjpal.model.NodeParameters
import dev.tjpal.model.NodeType
import dev.tjpal.model.ParameterDefinition
import dev.tjpal.model.ParameterType
import dev.tjpal.utilities.ImageResourceEncoder
import javax.inject.Inject

class RestInputNodeFactory @Inject constructor(
    private val statusRegistry: StatusRegistry
) : NodeFactory {
    override fun definition(): NodeDefinition {
        val imageEncoder = ImageResourceEncoder()

        return NodeDefinition(
            id = "RESTEndpointInput",
            displayedName = "REST Input",
            type = NodeType.INPUT,
            category = "Input",
            description = "Node that registers for REST inputs",
            icon = imageEncoder.encodeResourceToBase64("rest-input.png"),
            inputConnectors = emptyList(),
            outputConnectors = listOf(ConnectorDefinition(id = "out", label = "Out", schema = ConnectorSchema.TEXT)),
            toolConnectors = emptyList(),
            debugConnectors = emptyList(),
            parameters = listOf(
                ParameterDefinition(
                    name = "Path",
                    type = ParameterType.STRING,
                    required = true,
                    description = "HTTP path to register for this input node, e.g. /api/order-created"
                )
            )
        )
    }

    override fun createNode(parameters: NodeParameters): Node {
        return RestInputNode(parameters, statusRegistry)
    }
}
