package dev.tjpal.nodes.hitl

import dev.tjpal.graph.status.StatusRegistry
import dev.tjpal.logging.logger
import dev.tjpal.model.NodeParameters
import dev.tjpal.model.NodeStatus
import dev.tjpal.model.StatusEntry
import dev.tjpal.nodes.Node
import dev.tjpal.nodes.NodeActivationContext
import dev.tjpal.nodes.NodeDeactivationContext
import dev.tjpal.nodes.NodeInvocationContext
import dev.tjpal.nodes.NodeOutput
import dev.tjpal.nodes.payload.RawPayload
import dev.tjpal.nodes.payload.ReviewDecisionPayload
import dev.tjpal.nodes.payload.ReviewRequestPayload
import kotlinx.serialization.json.Json
import java.util.UUID

class HumanInTheLoopNode(
    private val parameters: NodeParameters,
    private val reviewRepository: ReviewRepository,
    private val statusRegistry: StatusRegistry,
    private val json: Json
) : Node {
    private val logger = logger<HumanInTheLoopNode>()

    override fun onActivate(context: NodeActivationContext) {
        logger.debug("HumanInTheLoopNode.onActivate graphInstanceId={} nodeId={}", context.graphInstanceId, context.nodeId)
    }

    override suspend fun onEvent(context: NodeInvocationContext, output: NodeOutput) {
        val payload = context.payload

        try {
            when (payload) {
                is ReviewRequestPayload -> handleReviewRequest(context, payload)
                is ReviewDecisionPayload -> handleReviewDecision(context, payload, output)
                is RawPayload -> handleRawPayloadAsDecision(context, payload, output)
                else -> logger.warn("HumanInTheLoopNode received unsupported payload type for node {}: {}", context.nodeId, payload::class.simpleName)
            }
        } catch (e: Exception) {
            logger.error("HumanInTheLoopNode: unexpected error during onEvent for node {} execution {}: {}", context.nodeId, context.executionId, e.message)
            registerStatus(context, NodeStatus.ERROR, context.payload.asString(), null, "Unexpected processing error: ${e.message}")
        }
    }

    override fun onStop(context: NodeDeactivationContext) {
        logger.debug("HumanInTheLoopNode.onStop graphInstanceId={} nodeId={}", context.graphInstanceId, context.nodeId)
    }

    private fun handleReviewRequest(context: NodeInvocationContext, payload: ReviewRequestPayload) {
        val reviewId = UUID.randomUUID().toString()

        val record = ReviewRecord(
            reviewId = reviewId,
            graphInstanceId = context.graphInstanceId,
            executionId = context.executionId,
            nodeId = context.nodeId,
            request = payload,
            timestamp = System.currentTimeMillis(),
            status = ReviewStatus.PENDING
        )

        reviewRepository.create(record)

        registerStatus(
            context = context,
            status = NodeStatus.RUNNING,
            inputPayload = payload.asString(),
            outputPayload = null,
            message = "Review created: $reviewId"
        )

        logger.info("HumanInTheLoopNode created review {} for executionId={} nodeId={}", reviewId, context.executionId, context.nodeId)
    }

    private fun handleReviewDecision(context: NodeInvocationContext, payload: ReviewDecisionPayload, output: NodeOutput) {
        val originalId = payload.originalReviewId

        try {
            val decisionStatus = when (payload.status.uppercase()) {
                "ACCEPTED" -> ReviewStatus.ACCEPTED
                "DECLINED" -> ReviewStatus.DECLINED
                else -> throw IllegalArgumentException("Unknown decision status: ${payload.status}")
            }

            val updated = reviewRepository.updateDecision(originalId, decisionStatus, payload.reviewer, payload.comment)

            registerStatus(
                context = context,
                status = NodeStatus.FINISHED,
                inputPayload = payload.asString(),
                outputPayload = null,
                message = "Review ${updated.reviewId} decided: ${updated.status} by ${updated.reviewer}"
            )

            if (updated.status == ReviewStatus.ACCEPTED) {
                output.send("out", RawPayload(updated.request.instructions))

                logger.info("HumanInTheLoopNode forwarded accepted review {} as RawPayload for executionId={} nodeId={}", originalId, context.executionId, context.nodeId)
            } else {
                logger.debug("HumanInTheLoopNode review {} declined by {}", originalId, payload.reviewer)
            }
        } catch (e: Exception) {
            logger.error("HumanInTheLoopNode: decision processing error for review {}: {}", originalId, e.message)

            registerStatus(context, NodeStatus.ERROR, payload.asString(), null, "Decision processing failed: ${e.message}")
        }
    }

    private fun handleRawPayloadAsDecision(context: NodeInvocationContext, raw: RawPayload, output: NodeOutput) {
        val text = raw.asString().trim()

        // To support a broader number of node types, we act here as if the input is both an instruction and a decision description.
        val reviewRequest = ReviewRequestPayload(
            instructions = text,
            decisionDescription = text
        )

        handleReviewRequest(context, reviewRequest)
    }

    private fun registerStatus(
        context: NodeInvocationContext,
        status: NodeStatus,
        inputPayload: String?,
        outputPayload: String?,
        message: String?
    ) {
        val entry = StatusEntry(
            graphInstanceId = context.graphInstanceId,
            executionId = context.executionId,
            nodeId = context.nodeId,
            timestamp = System.currentTimeMillis(),
            nodeStatus = status,
            inputPayload = inputPayload,
            outputPayload = outputPayload,
            message = message
        )

        statusRegistry.registerStatusEvent(entry)
    }
}
