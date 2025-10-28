package dev.tjpal.model

import kotlinx.serialization.Serializable

@Serializable
data class RestInputRequest(val executionId: String, val nodeId: String, val payload: String)