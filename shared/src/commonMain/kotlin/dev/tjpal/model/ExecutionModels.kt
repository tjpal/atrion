package dev.tjpal.model

import kotlinx.serialization.Serializable

@Serializable
data class ExecutionStartRequest(val graphId: String)