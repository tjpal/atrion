package dev.tjpal.repo

import dev.tjpal.client.ReviewsApiClient
import dev.tjpal.model.DecisionResponse
import dev.tjpal.model.ReviewDecisionRequest
import dev.tjpal.model.ReviewRecord

class ReviewRepository(private val api: ReviewsApiClient) {
    private var cached: Map<String, ReviewRecord> = mutableMapOf()

    suspend fun refresh() {
        val fetched = api.listReviews()
        cached = fetched.associateBy { it.reviewId }
    }

    fun listReviews(): List<ReviewRecord> {
        return cached.values.toList()
    }

    fun getReview(id: String): ReviewRecord? {
        return cached[id]
    }

    suspend fun submitDecision(id: String, decision: ReviewDecisionRequest): DecisionResponse {
        return api.submitDecision(id, decision)
    }
}
