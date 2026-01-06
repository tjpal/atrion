package dev.tjpal.nodes

import dev.tjpal.ai.LLM
import dev.tjpal.ai.tools.ToolRegistry
import dev.tjpal.graph.status.StatusRegistry
import dev.tjpal.model.ConnectorDefinition
import dev.tjpal.model.NodeDefinition
import dev.tjpal.model.NodeParameters
import dev.tjpal.model.NodeType
import dev.tjpal.model.ParameterDefinition
import dev.tjpal.model.ParameterType
import dev.tjpal.prompt.PromptsRepository
import dev.tjpal.utilities.ImageResourceEncoder
import kotlinx.serialization.json.Json
import javax.inject.Inject

class LLMProcessingNodeFactory @Inject constructor(
    private val llm: LLM,
    private val statusRegistry: StatusRegistry,
    private val toolRegistry: ToolRegistry,
    private val promptsRepository: PromptsRepository,
    private val json: Json
) : NodeFactory {
    override fun definition(): NodeDefinition {
        val resourceEncoder = ImageResourceEncoder()

        return NodeDefinition(
            id = "LLMProcessor",
            displayedName = "LLM",
            type = NodeType.PROCESSOR,
            category = "Processing",
            description = "Passes input text to a Language Model and returns the response.",
            icon = resourceEncoder.encodeResourceToBase64("llm.png"),
            inputConnectors = listOf(ConnectorDefinition(id = LLMProcessingNode.INPUT_CONNECTOR_ID, label = "In")),
            outputConnectors = listOf(ConnectorDefinition(id = LLMProcessingNode.OUTPUT_CONNECTOR_ID, label = "Out")),
            toolConnectors = listOf(ConnectorDefinition(id = "tool", label = "Tool")),
            debugConnectors = emptyList(),
            parameters = listOf(ParameterDefinition(
                name = "Prompt",
                type = ParameterType.LONG_TEXT,
                required = true,
                description = "The prompt template to use when querying the LLM. Use {{input}} to reference the input text."
            ))
        )
    }

    override fun createNode(parameters: NodeParameters): Node {
        return LLMProcessingNode(
            parameters = parameters,
            llm = llm, statusRegistry,
            promptsRepository = promptsRepository,
            toolRegistry = toolRegistry,
            json = json
        )
    }
}
