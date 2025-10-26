package dev.tjpal.nodes

import dev.tjpal.graph.ExecutionOutputStore
import dev.tjpal.graph.model.ConnectorDefinition
import dev.tjpal.graph.model.ConnectorSchema
import dev.tjpal.graph.model.NodeDefinition
import dev.tjpal.graph.model.NodeType
import javax.inject.Inject

@NodeFactoryKey(value = "sync_output")
class OutputSinkNodeFactory @Inject constructor(private val executionOutputStore: ExecutionOutputStore) : NodeFactory {
    override fun definition(): NodeDefinition {
        return NodeDefinition(
            name = "Sync Output",
            type = NodeType.OUTPUT,
            category = "Output",
            description = "Stores textual outputs in-memory for synchronous retrieval",
            icon = "sync_output",
            inputConnectors = listOf(ConnectorDefinition(id = "in", label = "In", schema = ConnectorSchema.TEXT)),
            outputConnectors = emptyList(),
            toolConnectors = emptyList(),
            debugConnectors = emptyList(),
            parameters = emptyList()
        )
    }

    override fun createNode(parametersJson: String): Node {
        return OutputSinkNode(parametersJson, executionOutputStore)
    }
}
