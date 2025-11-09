package dev.tjpal.model

import kotlinx.serialization.Serializable

@Serializable
enum class NodeState { Waiting, Running, Completed, Error }

@Serializable
data class NodeStatus(
    val graphId: String,
    val graphExecutionId: String,
    val nodeId: String,
    val nodeState: NodeState,
    val message: String,
    val timestamp: Long
)

@Serializable
data class TransferredEdgeData(
    val graphId: String,
    val graphExecutionId: String,
    val fromNodeId: String,
    val toNodeId: String,
    val fromConnectorId: String,
    val toConnectorId: String,
    val data: String,
    val timestamp: Long
)
