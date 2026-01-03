package dev.tjpal.nodes.hitl

import dev.tjpal.graph.status.StatusRegistry
import dev.tjpal.model.ConnectorDefinition
import dev.tjpal.model.NodeDefinition
import dev.tjpal.model.NodeParameters
import dev.tjpal.model.NodeType
import dev.tjpal.model.ParameterDefinition
import dev.tjpal.model.ParameterType
import dev.tjpal.nodes.Node
import dev.tjpal.nodes.NodeFactory
import dev.tjpal.nodes.payload.PayloadTypeToClass
import dev.tjpal.nodes.payload.ReviewRequestPayload
import dev.tjpal.utilities.ImageResourceEncoder
import kotlinx.serialization.json.Json
import javax.inject.Inject

class HumanInTheLoopNodeFactory @Inject constructor(
    private val reviewRepository: ReviewRepository,
    private val statusRegistry: StatusRegistry,
    private val json: Json
) : NodeFactory {

    override fun definition(): NodeDefinition {
        val encoder = ImageResourceEncoder()

        return NodeDefinition(
            id = "HumanInTheLoop",
            displayedName = "Human-In-The-Loop",
            type = NodeType.PROCESSOR,
            category = "Processing",
            description = "Node that requests human review/decision and forwards the response.",
            icon = encoder.encodeResourceToBase64("hitl.png"),
            inputConnectors = listOf(
                ConnectorDefinition(id = "in", label = "In", preferredInputType = PayloadTypeToClass.toType(ReviewRequestPayload::class))
            ),
            outputConnectors = listOf(
                ConnectorDefinition(id = "out", label = "Out")
            ),
            toolConnectors = emptyList(),
            debugConnectors = emptyList(),
            parameters = listOf(
                ParameterDefinition(
                    name = "ReviewerGroup",
                    type = ParameterType.STRING,
                    required = false,
                    description = "Optional group or identifier for the human reviewer"
                )
            )
        )
    }

    override fun createNode(parameters: NodeParameters): Node {
        return HumanInTheLoopNode(parameters, reviewRepository, statusRegistry, json)
    }
}
