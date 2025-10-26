package dev.tjpal.nodes

interface NodeFactory {
    // Provides the static definition that describes the node
    fun definition(): dev.tjpal.graph.model.NodeDefinition

    // Creates a runtime node instance backed by the provided opaque parameter payload.
    fun createNode(parametersJson: String): Node
}

// Lifecycle and runtime interface for node instances
interface Node {
    // Called when the graph execution becomes active. The node may register hooks/callbacks here.
    fun onActivate(context: NodeActivationContext)

    // Called when a message is delivered to this node for processing.
    suspend fun onEvent(payload: String, output: NodeOutput)

    // Called when the graph execution stops or when the node is removed; used to unregister hooks and cleanup.
    fun onStop()
}

// Context passed to nodes during activation
data class NodeActivationContext(
    val executionId: String,
    val nodeId: String,
    val parametersJson: String,
)

// Node output interface used by nodes to send outputs back to the graph. Will be implemented by the ActiveGraph
// to provide a way back for data.
interface NodeOutput {
    fun send(outputConnectorId: String, payload: String)
}
