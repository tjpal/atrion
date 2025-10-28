package dev.tjpal.nodes

import dev.tjpal.graph.ActiveGraph
import dev.tjpal.model.NodeDefinition

interface NodeFactory {
    // Provides the static definition that describes the node
    fun definition(): NodeDefinition

    // Creates a runtime node instance backed by the provided opaque parameter payload.
    fun createNode(parametersJson: String): Node
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
    val executionId: String,
    val nodeId: String,
    val parametersJson: String,
    val graph: ActiveGraph
)

data class NodeInvocationContext(
    val executionId: String,
    val nodeId: String,
    val payload: String
)

data class NodeDeactivationContext(
    val executionId: String,
    val nodeId: String
)

// Node output interface used by nodes to send outputs back to the graph. Will be implemented by the ActiveGraph
// to provide a way back for data.
interface NodeOutput {
    fun send(outputConnectorId: String, payload: String)
}
