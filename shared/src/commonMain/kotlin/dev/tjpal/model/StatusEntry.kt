package dev.tjpal.model

import kotlinx.serialization.Serializable

@Serializable
enum class NodeStatus {
    PENDING,
    RUNNING,
    FINISHED,
    ERROR
}

@Serializable
data class StatusEntry(
    val graphInstanceId: String,
    val executionId: String,
    val nodeId: String,
    val nodeStatus: NodeStatus,
    val message: String? = null,
    val inputPayload: String? = null,
    val outputPayload: String? = null,
    val timestamp: Long
)
