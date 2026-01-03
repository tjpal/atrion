package dev.tjpal.model

import kotlinx.serialization.Serializable

@Serializable
enum class ReviewStatus {
    PENDING,
    ACCEPTED,
    DECLINED,
    COMMENTED,
}

@Serializable
data class ReviewRecord(
    val reviewId: String,
    val graphInstanceId: String,
    val executionId: String,
    val nodeId: String,
    val reviewInstructions: String,
    val reviewDecisionDescription: String,
    val timestamp: Long,
    var status: ReviewStatus,
    var reviewer: String? = null,
    var comment: String? = null
)
