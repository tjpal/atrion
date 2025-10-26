package dev.tjpal.nodes

import dev.tjpal.graph.ExecutionOutputStore
import javax.inject.Inject

class OutputSinkNode @Inject constructor(
    private val parametersJson: String,
    private val executionOutputStore: ExecutionOutputStore
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
    }

    override fun onStop(context: NodeDeactivationContext) {
        // No action
    }
}
