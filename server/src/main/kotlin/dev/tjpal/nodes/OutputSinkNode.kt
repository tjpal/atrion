package dev.tjpal.nodes

import dev.tjpal.graph.ExecutionOutputStore
import dev.tjpal.graph.status.StatusRegistry
import dev.tjpal.model.NodeStatus
import dev.tjpal.model.StatusEntry
import javax.inject.Inject

class OutputSinkNode @Inject constructor(
    private val parametersJson: String,
    private val executionOutputStore: ExecutionOutputStore,
    private val statusRegistry: StatusRegistry
) : Node {

    override fun onActivate(context: NodeActivationContext) {
        // No action
    }

    override suspend fun onEvent(context: NodeInvocationContext, output: NodeOutput) {
        executionOutputStore.appendOutput(
            executionId = context.executionId,
            nodeId = context.nodeId,
            payload = context.payload
        )

        sendStatusEntry(context.payload, context)
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
    }
}
