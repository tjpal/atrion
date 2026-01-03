package dev.tjpal.nodes.hitl

import dev.tjpal.logging.logger
import dev.tjpal.model.ReviewRecord
import dev.tjpal.model.ReviewStatus
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 *  In-memory repository for ReviewRecord objects. Thread-safe.
 */
@Singleton
class ReviewRepository @Inject constructor() {
    private val logger = logger<ReviewRepository>()

    private val store: ConcurrentHashMap<String, ReviewRecord> = ConcurrentHashMap()

    fun create(record: ReviewRecord): String {
        store[record.reviewId] = record
        logger.debug("ReviewRepository: created review id={} graphInstanceId={} nodeId={}", record.reviewId, record.graphInstanceId, record.nodeId)

        return record.reviewId
    }

    fun get(reviewId: String): ReviewRecord? {
        return store[reviewId]
    }

    fun listAll(status: ReviewStatus? = null): List<ReviewRecord> =
        store.values.filter { status == null || it.status == status }

    fun listByGraph(graphInstanceId: String, status: ReviewStatus? = null): List<ReviewRecord> =
        store.values.filter { it.graphInstanceId == graphInstanceId && (status == null || it.status == status) }

    fun updateDecision(reviewId: String, status: ReviewStatus, reviewer: String?, comment: String?): ReviewRecord {
        val existing = store[reviewId] ?: throw IllegalArgumentException("No review with id: $reviewId")

        synchronized(existing) {
            if (existing.status != ReviewStatus.PENDING) {
                throw IllegalStateException("Review $reviewId is not in PENDING state and cannot be updated")
            }

            existing.status = status
            existing.reviewer = reviewer
            existing.comment = comment
        }

        logger.debug("ReviewRepository: review id={} updated status={} reviewer={}", reviewId, status, reviewer)
        return existing
    }

    fun clearAll() {
        store.clear()
    }
}
