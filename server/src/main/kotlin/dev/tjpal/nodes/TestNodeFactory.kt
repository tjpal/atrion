package dev.tjpal.nodes

import dev.tjpal.graph.model.NodeDefinition
import dev.tjpal.graph.model.NodeType
import javax.inject.Inject

@NodeFactoryKey(value = "test")
class TestNodeFactory @Inject constructor() : NodeFactory {
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
        return object : Node {
            override fun onActivate(context: NodeActivationContext) {
            }

            override suspend fun onEvent(payload: String, output: NodeOutput) {
                println("TestNode received payload: $payload")
            }

            override fun onStop() {
            }
        }
    }
}
