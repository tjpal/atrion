package dev.tjpal.nodes.multiplexer

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

class MultiplexerNode(
    private val parameters: NodeParameters,
    private val statusRegistry: StatusRegistry
) : Node {
    private val logger = logger<MultiplexerNode>()

    override fun onActivate(context: NodeActivationContext) {
        logger.debug("MultiplexerNode.onActivate graphInstanceId={} nodeId={}", context.graphInstanceId, context.nodeId)
    }

    override suspend fun onEvent(context: NodeInvocationContext, output: NodeOutput) {
        try {
            val payload = context.payload
            val outputs = listOf("out1", "out2", "out3")

            for (connectorId in outputs) {
                // Check whether the connector has any connected targets in the active graph
                val connected = context.graph.isConnectorConnected(context.nodeId, connectorId)

                if (!connected) {
                    logger.debug("MultiplexerNode: skipping unconnected output connector {} for node {} execution {}", connectorId, context.nodeId, context.executionId)
                    continue
                }

                output.send(connectorId, payload)
            }

            registerStatus(context, NodeStatus.FINISHED, null, null, "Forwarded outputs")
            logger.debug("MultiplexerNode processed payload for graphInstanceId={} nodeId={} executionId={}", context.graphInstanceId, context.nodeId, context.executionId)
        } catch (e: Exception) {
            logger.error("MultiplexerNode: unexpected error during onEvent for node {} execution {}: {}", context.nodeId, context.executionId, e.message)
            registerStatus(context, NodeStatus.ERROR, context.payload.asString(), null, "Unexpected processing error: ${e.message}")
        }
    }

    override fun onStop(context: NodeDeactivationContext) {
        logger.debug("MultiplexerNode.onStop graphInstanceId={} nodeId={}", context.graphInstanceId, context.nodeId)
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
