package dev.tjpal.nodes.multiplexer

import dev.tjpal.graph.status.StatusRegistry
import dev.tjpal.model.ConnectorDefinition
import dev.tjpal.model.NodeDefinition
import dev.tjpal.model.NodeParameters
import dev.tjpal.model.NodeType
import dev.tjpal.nodes.Node
import dev.tjpal.nodes.NodeActivationContext
import dev.tjpal.nodes.NodeDeactivationContext
import dev.tjpal.nodes.NodeFactory
import dev.tjpal.nodes.NodeInvocationContext
import dev.tjpal.utilities.ImageResourceEncoder
import javax.inject.Inject

class MultiplexerNodeFactory @Inject constructor(
    private val statusRegistry: StatusRegistry
) : NodeFactory {
    override fun definition(): NodeDefinition {
        val encoder = ImageResourceEncoder()

        return NodeDefinition(
            id = "Multiplexer",
            displayedName = "Multiplexer",
            type = NodeType.PROCESSOR,
            category = "Processing",
            description = "Forwards the input payload unchanged to up to three outputs",
            icon = encoder.encodeResourceToBase64("hitl.png"),
            inputConnectors = listOf(ConnectorDefinition(id = "in", label = "In")),
            outputConnectors = listOf(
                ConnectorDefinition(id = "out1", label = "Out 1"),
                ConnectorDefinition(id = "out2", label = "Out 2"),
                ConnectorDefinition(id = "out3", label = "Out 3")
            ),
            toolConnectors = emptyList(),
            debugConnectors = emptyList(),
            parameters = emptyList()
        )
    }

    override fun createNode(parameters: NodeParameters): Node {
        return MultiplexerNode(parameters, statusRegistry)
    }
}
