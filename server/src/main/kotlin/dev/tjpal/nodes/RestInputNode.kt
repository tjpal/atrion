package dev.tjpal.nodes

import dev.tjpal.graph.hooks.RestInputRegistry
import dev.tjpal.graph.status.StatusRegistry
import dev.tjpal.model.NodeStatus
import dev.tjpal.model.StatusEntry

/**
 * Registers/unregisters HTTP input via the provided RestInputRegistry and forwards it to input nodes via the active
 * graph interface.
 */
class RestInputNode(
    private val restInputRegistry: RestInputRegistry,
    private val parametersJson: String,
    private val statusRegistry: StatusRegistry
) : Node {
    private val logger = dev.tjpal.logging.logger<RestInputNode>()
    private var registered = false

    override fun onActivate(context: NodeActivationContext) {
        try {
            // register a mapping such that RestInputRegistry forwards incoming HTTP requests
            // to graph.onInputEvent for this graphInstanceId/node
            restInputRegistry.register(context.graphInstanceId, context.nodeId, context.graph)
            registered = true
            logger.info("RestInputNode activated and registered graphInstanceId={} nodeId={}", context.graphInstanceId, context.nodeId)
        } catch (e: Exception) {
            logger.error("RestInputNode: failed to register HTTP input for node {}", context.nodeId, e)
        }
    }

    override suspend fun onEvent(context: NodeInvocationContext, output: NodeOutput) {
        // Forward payload to default output connector
        output.send("out", context.payload)

        sendStatusEntry(context.payload, context)
        logger.debug(
            "RestInputNode forwarded payload for graphInstanceId={} nodeId={} executionId={} payload={}",
            context.graphInstanceId, context.nodeId, context.executionId, context.payload
        )
    }

    private fun sendStatusEntry(payload: String, context: NodeInvocationContext) {
        val statusEntry = StatusEntry(
            graphInstanceId = context.graphInstanceId,
            executionId = context.executionId,
            nodeId = context.nodeId,
            timestamp = System.currentTimeMillis(),
            nodeStatus = NodeStatus.FINISHED,
            outputPayload = payload
        )

        statusRegistry.registerStatusEvent(statusEntry)
    }

    override fun onStop(context: NodeDeactivationContext) {
        if(registered) {
            restInputRegistry.unregister(context.graphInstanceId, context.nodeId)
            logger.info("RestInputNode unregistered graphInstanceId={} nodeId={}", context.graphInstanceId, context.nodeId)
        }
    }
}
