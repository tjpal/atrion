package dev.tjpal.nodes

import dev.tjpal.graph.hooks.RestInputRegistry

/**
 * Registers/unregisters HTTP input via the provided RestInputRegistry and forwards it to input nodes via the active
 * graph interface.
 */
class RestInputNode(
    private val restInputRegistry: RestInputRegistry,
    private val parametersJson: String
) : Node {
    private val logger = dev.tjpal.logging.logger<RestInputNode>()
    private var registered = false

    override fun onActivate(context: NodeActivationContext) {
        try {
            // register a mapping such that RestInputRegistry forwards incoming HTTP requests
            // to graph.onInputEvent for this graphInstanceId/node
            restInputRegistry.register(context.graphInstanceId, context.nodeId, context.graph)
            registered = true
        } catch (e: Exception) {
            logger.error("RestInputNode: failed to register HTTP input for node {}", context.nodeId, e)
        }
    }

    override suspend fun onEvent(context: NodeInvocationContext, output: NodeOutput) {
        // Forward payload to default output connector
        output.send("out", context.payload)
    }

    override fun onStop(context: NodeDeactivationContext) {
        if(registered) {
            restInputRegistry.unregister(context.graphInstanceId, context.nodeId)
        }
    }
}
