package dev.tjpal.api.route

import dev.tjpal.graph.ActiveGraphRepository
import dev.tjpal.logging.logger
import dev.tjpal.model.ReviewDecisionRequest
import dev.tjpal.model.ReviewRecord
import dev.tjpal.model.ReviewStatus
import dev.tjpal.nodes.hitl.ReviewRepository
import dev.tjpal.nodes.payload.ReviewDecisionPayload
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlinx.serialization.json.Json

private object ReviewsRouteLogTag
private val logger = logger<ReviewsRouteLogTag>()

/**
 * Routes to list review records and to submit review decisions.
 */
fun Routing.reviewsRoute(reviewRepository: ReviewRepository, activeGraphRepository: ActiveGraphRepository, json: Json) {
    get("/reviews") {
        try {
            val graphInstanceId = call.request.queryParameters["graphInstanceId"]
            val statusParam = call.request.queryParameters["status"]

            val status: ReviewStatus? = statusParam?.let {
                try {
                    ReviewStatus.valueOf(it.uppercase())
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid status filter: $it"))
                    return@get
                }
            }

            val results: List<ReviewRecord> = if (!graphInstanceId.isNullOrBlank()) {
                reviewRepository.listByGraph(graphInstanceId, status)
            } else {
                reviewRepository.listAll(status)
            }

            logger.info("GET /reviews returning {} entries (graphInstanceId={}, status={})", results.size, graphInstanceId ?: "-", status ?: "-")
            call.respond(HttpStatusCode.OK, results)
        } catch (e: Exception) {
            logger.error("GET /reviews failed: {}", e.message)
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "internal error")))
        }
    }

    get("/reviews/{id}") {
        val id = call.parameters["id"]
        if (id.isNullOrBlank()) {
            logger.warn("GET /reviews/{id} missing id")
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing id"))
            return@get
        }

        val record = reviewRepository.get(id) ?: run {
            logger.warn("GET /reviews/{} not found", id)
            call.respond(HttpStatusCode.NotFound, mapOf("error" to "Review with id $id not found"))
            return@get
        }

        call.respond(HttpStatusCode.OK, record)
    }

    post("/reviews/{id}/decision") {
        val id = call.parameters["id"]
        if (id.isNullOrBlank()) {
            logger.warn("POST /reviews/{id}/decision missing id")
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing id"))
            return@post
        }

        try {
            val decisionApi = call.receive<ReviewDecisionRequest>()
            val reviewRecord = reviewRepository.get(id) ?: run {
                logger.warn("POST /reviews/{}/decision not found", id)
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Review with id $id not found"))
                return@post
            }

            var delivered = false

            try {
                val graph = activeGraphRepository.get(reviewRecord.graphInstanceId)

                val internalDecisionPayload = ReviewDecisionPayload(
                    originalReviewId = id,
                    decision = decisionApi.decision,
                    reviewer = decisionApi.reviewer,
                    comment = decisionApi.comment
                )

                graph.deliverToNode(
                    targetNodeId = reviewRecord.nodeId,
                    toConnectorId = "in",
                    payload = internalDecisionPayload,
                    executionId = reviewRecord.executionId,
                    fromNodeId = null,
                    fromConnectorId = null
                )

                delivered = true
            } catch (e: IllegalArgumentException) {
                logger.info("Decision stored but graph not active for graphInstanceId={}", reviewRecord.graphInstanceId)
            } catch (e: Exception) {
                logger.error("Failed to deliver decision for review {}: {}", id, e.message)
            }

            val response = mapOf("delivered" to delivered)

            call.respond(HttpStatusCode.OK, response)
        } catch (e: Exception) {
            logger.warn("POST /reviews/{}/decision failed: {}", id ?: "<missing>", e.message)
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "invalid request")))
        }
    }
}
