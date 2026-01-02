package dev.tjpal.nodes

import dev.tjpal.graph.ActiveGraph
import dev.tjpal.model.NodeDefinition
import dev.tjpal.model.NodeParameters
import dev.tjpal.nodes.payload.NodePayload

interface NodeFactory {
    // Provides the static definition that describes the node
    fun definition(): NodeDefinition

    // Creates a runtime node instance backed by the provided typed parameter payload.
    fun createNode(parameters: NodeParameters): Node
}

// Lifecycle and runtime interface for node instances
interface Node {
    // Called when the graph execution becomes active. The node may register hooks/callbacks here.
    fun onActivate(context: NodeActivationContext)

    // Called when a message is delivered to this node for processing.
    suspend fun onEvent(context: NodeInvocationContext, output: NodeOutput)

    // Called when the graph execution stops or when the node is removed; used to unregister hooks and cleanup.
    fun onStop(context: NodeDeactivationContext)
}

// Context passed to nodes during activation
// Includes reference to the ActiveGraph so nodes can create callbacks that forward incoming REST events to the graph.
data class NodeActivationContext(
    val graphInstanceId: String,
    val nodeId: String,
    val parameters: NodeParameters,
    val graph: ActiveGraph
)

data class NodeInvocationContext(
    val graphInstanceId: String,
    val executionId: String,
    val nodeId: String,
    val payload: NodePayload,
    val graph: ActiveGraph,
    val fromNodeId: String? = null,     // Used to send replies back to the origin node (feedback loops like HITL)
    val fromConnectorId: String? = null
)

data class NodeDeactivationContext(
    val graphInstanceId: String,
    val nodeId: String
)

// Node output interface used by nodes to send outputs back to the graph. Will be implemented by the ActiveGraph
// to provide a way back for data.
interface NodeOutput {
    /**
     * Send a payload to the specified output connector of this node.
     */
    fun send(outputConnectorId: String, payload: NodePayload)

    /**
     * Send a reply back to the origin node that sent the input payload to this node.
     * This method is used for feedback loops like Human-in-the-Loop (HITL) where a node needs to reply
     * back to the node that sent the original request.
     */
    fun reply(payload: NodePayload) {
        throw UnsupportedOperationException("reply not implemented")
    }
}
