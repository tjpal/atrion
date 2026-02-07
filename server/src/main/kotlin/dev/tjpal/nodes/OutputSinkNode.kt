package dev.tjpal.nodes

import dev.tjpal.graph.ExecutionOutputStore
import dev.tjpal.graph.ExecutionResponseAwaiter
import dev.tjpal.graph.OutputRecord
import dev.tjpal.graph.status.StatusRegistry
import dev.tjpal.logging.logger
import dev.tjpal.model.NodeParameters
import dev.tjpal.model.NodeStatus
import dev.tjpal.model.StatusEntry
import javax.inject.Inject

class OutputSinkNode @Inject constructor(
    private val parameters: NodeParameters,
    private val executionOutputStore: ExecutionOutputStore,
    private val statusRegistry: StatusRegistry
) : Node {

    private val logger = logger<OutputSinkNode>()

    override fun onActivate(context: NodeActivationContext) {
        // No action
        logger.debug("OutputSinkNode.onActivate graphInstanceId={} nodeId={}", context.graphInstanceId, context.nodeId)
    }

    override suspend fun onEvent(context: NodeInvocationContext, output: NodeOutput) {
        val record = OutputRecord(nodeId = context.nodeId, payload = context.payload.asString())

        try {
            val matchedInputCall = ExecutionResponseAwaiter.notifyOutput(context.executionId, record)

            if (matchedInputCall) {
                logger.debug("OutputSinkNode notified awaiter for executionId={} nodeId={}", context.executionId, context.nodeId)
            } else {
                logger.debug("OutputSinkNode no awaiter found for executionId={}, appending output to store", context.executionId)

                executionOutputStore.appendOutput(
                    executionId = context.executionId,
                    nodeId = context.nodeId,
                    payload = context.payload.asString()
                )
            }
        } catch (e: Exception) {
            logger.error("OutputSinkNode: failed to notify awaiter for executionId={}: {}", context.executionId, e.message)
        }

        sendStatusEntry(context.payload.asString(), context)
        logger.debug("OutputSinkNode appended output for executionId={} nodeId={} payload={}", context.executionId, context.nodeId, context.payload.asString())
    }

    private fun sendStatusEntry(payload: String, context: NodeInvocationContext) {
        val entry = StatusEntry(
            graphInstanceId = context.graphInstanceId,
            executionId = context.executionId,
            nodeId = context.nodeId,
            timestamp = System.currentTimeMillis(),
            nodeStatus = NodeStatus.FINISHED,
            outputPayload = payload
        )

        statusRegistry.registerStatusEvent(entry)
    }

    override fun onStop(context: NodeDeactivationContext) {
        // No action
        logger.debug("OutputSinkNode.onStop graphInstanceId={} nodeId={}", context.graphInstanceId, context.nodeId)
    }
}
