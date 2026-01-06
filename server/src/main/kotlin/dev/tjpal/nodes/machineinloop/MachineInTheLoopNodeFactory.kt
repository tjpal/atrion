package dev.tjpal.nodes.machineinloop

import dev.tjpal.ai.LLM
import dev.tjpal.graph.status.StatusRegistry
import dev.tjpal.model.ConnectorDefinition
import dev.tjpal.model.NodeDefinition
import dev.tjpal.model.NodeParameters
import dev.tjpal.model.NodeType
import dev.tjpal.model.ParameterDefinition
import dev.tjpal.model.ParameterType
import dev.tjpal.nodes.Node
import dev.tjpal.prompt.PromptsRepository
import dev.tjpal.utilities.ImageResourceEncoder
import kotlinx.serialization.json.Json
import javax.inject.Inject

class MachineInTheLoopNodeFactory @Inject constructor(
    private val llm: LLM,
    private val statusRegistry: StatusRegistry,
    private val promptsRepository: PromptsRepository,
    private val json: Json
) : dev.tjpal.nodes.NodeFactory {

    override fun definition(): NodeDefinition {
        val encoder = ImageResourceEncoder()

        return NodeDefinition(
            id = "MachineInTheLoop",
            displayedName = "Machine-In-The-Loop",
            type = NodeType.PROCESSOR,
            category = "Processing",
            description = "Automatically evaluates the quality of ReviewRequestPayloads using the LLM and emits a textual summary on the out connector.",
            icon = encoder.encodeResourceToBase64("mitl.png"),
            inputConnectors = listOf(ConnectorDefinition(id = "in", label = "In")),
            outputConnectors = listOf(ConnectorDefinition(id = "out", label = "Out")),
            toolConnectors = emptyList(),
            debugConnectors = emptyList(),
            parameters = listOf(
                ParameterDefinition(
                    name = "Prompt",
                    type = ParameterType.LONG_TEXT,
                    required = false,
                    description = "Prompt template for the LLM."
                ),
                ParameterDefinition(
                    name = "Reference",
                    type = ParameterType.LONG_TEXT,
                    required = false,
                    description = "Reference text describing the expected result to be used in the evaluation."
                )
            )
        )
    }

    override fun createNode(parameters: NodeParameters): Node {
        return MachineInTheLoopNode(parameters, llm, statusRegistry, promptsRepository, json)
    }
}
