package dev.tjpal.nodes

import dev.tjpal.ai.LLM
import dev.tjpal.model.ConnectorDefinition
import dev.tjpal.model.ConnectorSchema
import dev.tjpal.model.NodeDefinition
import dev.tjpal.model.NodeType
import dev.tjpal.utilities.ImageResourceEncoder
import javax.inject.Inject

class LLMProcessingNodeFactory @Inject constructor(private val llm: LLM) : NodeFactory {
    override fun definition(): NodeDefinition {
        val resourceEncoder = ImageResourceEncoder()

        return NodeDefinition(
            name = "LLM Processor",
            type = NodeType.PROCESSOR,
            category = "Processing",
            description = "Passes input text to a Language Model and returns the response.",
            icon = resourceEncoder.encodeResourceToBase64("placeholder-2.png"),
            inputConnectors = listOf(ConnectorDefinition(id = "in", label = "In", schema = ConnectorSchema.TEXT)),
            outputConnectors = listOf(ConnectorDefinition(id = "text_out", label = "Out", schema = ConnectorSchema.TEXT)),
            toolConnectors = emptyList(),
            debugConnectors = emptyList(),
            parameters = emptyList()
        )
    }

    override fun createNode(parametersJson: String): Node {
        return LLMProcessingNode(parametersJson, llm)
    }
}
