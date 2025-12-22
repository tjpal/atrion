package dev.tjpal.nodes

import dev.tjpal.graph.ExecutionOutputStore
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
        executionOutputStore.appendOutput(
            executionId = context.executionId,
            nodeId = context.nodeId,
            payload = context.payload
        )

        sendStatusEntry(context.payload, context)
        logger.debug("OutputSinkNode appended output for executionId={} nodeId={} payload={}", context.executionId, context.nodeId, context.payload)
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
