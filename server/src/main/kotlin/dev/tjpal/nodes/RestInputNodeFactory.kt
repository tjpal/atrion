package dev.tjpal.nodes

import dev.tjpal.graph.hooks.RestInputRegistry
import dev.tjpal.model.ConnectorDefinition
import dev.tjpal.model.ConnectorSchema
import dev.tjpal.model.NodeDefinition
import dev.tjpal.model.NodeType
import javax.inject.Inject

@NodeFactoryKey(value = "rest_input")
class RestInputNodeFactory @Inject constructor(
    private val restInputRegistry: RestInputRegistry
) : NodeFactory {
    override fun definition(): NodeDefinition {
        return NodeDefinition(
            name = "REST Input",
            type = NodeType.INPUT,
            category = "Input",
            description = "Node that registers for REST inputs",
            icon = "rest_input",
            inputConnectors = emptyList(),
            outputConnectors = listOf(ConnectorDefinition(id = "out", label = "Out", schema = ConnectorSchema.TEXT)),
            toolConnectors = emptyList(),
            debugConnectors = emptyList(),
            parameters = emptyList()
        )
    }

    override fun createNode(parametersJson: String): Node {
        return RestInputNode(restInputRegistry, parametersJson)
    }
}
