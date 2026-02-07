package dev.tjpal.model

import kotlinx.serialization.Serializable

@Serializable
data class RESTRequest(
    val executionId: String? = null,
    val timeout: Long = 300000, // 300 seconds for synchronous calls. Ignored for asynchronous calls.
    val synchronous: Boolean = true,
    val payload: String
)

@Serializable
data class RESTResponse(
    val executionId: String,
    val error: Boolean = false,
    val payload: String
)