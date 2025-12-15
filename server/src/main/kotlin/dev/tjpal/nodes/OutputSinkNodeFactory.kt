package dev.tjpal.nodes

import dev.tjpal.graph.ExecutionOutputStore
import dev.tjpal.graph.status.StatusRegistry
import dev.tjpal.model.ConnectorDefinition
import dev.tjpal.model.ConnectorSchema
import dev.tjpal.model.NodeDefinition
import dev.tjpal.model.NodeType
import dev.tjpal.utilities.ImageResourceEncoder
import javax.inject.Inject

class OutputSinkNodeFactory @Inject constructor(
    private val executionOutputStore: ExecutionOutputStore,
    private val statusRegistry: StatusRegistry
) : NodeFactory {
    override fun definition(): NodeDefinition {
        val imageResourceEncoder = ImageResourceEncoder()

        return NodeDefinition(
            name = "Sink Output",
            type = NodeType.OUTPUT,
            category = "Output",
            description = "Stores textual outputs in-memory for synchronous retrieval",
            icon = imageResourceEncoder.encodeResourceToBase64("placeholder-3.png"),
            inputConnectors = listOf(ConnectorDefinition(id = "in", label = "In", schema = ConnectorSchema.TEXT)),
            outputConnectors = emptyList(),
            toolConnectors = emptyList(),
            debugConnectors = emptyList(),
            parameters = emptyList()
        )
    }

    override fun createNode(parametersJson: String): Node {
        return OutputSinkNode(parametersJson, executionOutputStore, statusRegistry)
    }
}
