package dev.tjpal.nodes

import dev.tjpal.graph.model.ConnectorDefinition
import dev.tjpal.graph.model.ConnectorSchema
import dev.tjpal.graph.model.NodeDefinition
import dev.tjpal.graph.model.NodeType
import javax.inject.Inject

@NodeFactoryKey(value = "llm_processor")
class LLMProcessingNodeFactory @Inject constructor() : NodeFactory {
    override fun definition(): NodeDefinition {
        return NodeDefinition(
            name = "LLM Processor",
            type = NodeType.PROCESSOR,
            category = "Processing",
            description = "Passes input text to a Language Model and returns the response.",
            icon = "",
            inputConnectors = listOf(ConnectorDefinition(id = "in", label = "In", schema = ConnectorSchema.TEXT)),
            outputConnectors = listOf(ConnectorDefinition(id = "text_out", label = "Out", schema = ConnectorSchema.TEXT)),
            toolConnectors = emptyList(),
            debugConnectors = emptyList(),
            parameters = emptyList()
        )
    }

    override fun createNode(parametersJson: String): Node {
        return LLMProcessingNode(parametersJson)
    }
}
