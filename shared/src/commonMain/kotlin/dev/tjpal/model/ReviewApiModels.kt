package dev.tjpal.model

import kotlinx.serialization.Serializable

@Serializable
enum class ReviewDecision {
    ACCEPTED,
    DECLINED,
    COMMENTED
}

@Serializable
data class ReviewDecisionRequest(
    val decision: ReviewDecision,
    val reviewer: String? = null,
    val comment: String? = null
)
