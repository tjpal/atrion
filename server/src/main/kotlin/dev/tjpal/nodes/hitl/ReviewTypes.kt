package dev.tjpal.nodes.hitl

import dev.tjpal.nodes.payload.ReviewRequestPayload

enum class ReviewStatus {
    PENDING,
    ACCEPTED,
    DECLINED
}

data class ReviewRecord(
    val reviewId: String,
    val graphInstanceId: String,
    val executionId: String,
    val nodeId: String,
    val request: ReviewRequestPayload,
    val timestamp: Long,
    var status: ReviewStatus,
    var reviewer: String? = null,
    var comment: String? = null
)
