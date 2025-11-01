package dev.tjpal.model

import kotlinx.serialization.Serializable

@Serializable
data class RestInputRequest(
    val graphInstanceId: String,
    val nodeId: String,
    val payload: String,
    val executionId: String? = null
)