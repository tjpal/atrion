package dev.tjpal.model

import kotlinx.serialization.Serializable

@Serializable
data class DecisionResponse(
    val delivered: Boolean,
    val deliveredMessage: String? = null
)
