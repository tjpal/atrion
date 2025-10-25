package dev.tjpal.nodes

import dev.tjpal.graph.model.NodeDefinition
import dev.tjpal.graph.model.NodeType

@NodeFactoryKey(value = "test")
class TestNodeFactory() : NodeFactory {
    override fun definition(): NodeDefinition {
        return NodeDefinition(
            name = "Test Node",
            type = NodeType.INPUT,
            category = "Test",
            description = "A node for testing purposes",
            icon = "test_icon",
            inputConnectors = emptyList(),
            outputConnectors = emptyList(),
            toolConnectors = emptyList(),
            debugConnectors = emptyList(),
            parameters = emptyList()
        )
    }

    override fun createNode(parametersJson: String): Node {
        TODO("Not yet implemented")
    }
}