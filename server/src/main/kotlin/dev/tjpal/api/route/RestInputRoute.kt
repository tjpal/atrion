package dev.tjpal.api.route

import dev.tjpal.graph.hooks.RestInputRegistry
import dev.tjpal.logging.logger
import dev.tjpal.model.RestInputRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import java.util.*

private object RestInputRouteLogTag
private val logger = logger<RestInputRouteLogTag>()

fun Route.restInputRoutes(restInputRegistry: RestInputRegistry) {
    post("/rest-input") {
        try {
            val request = call.receive<RestInputRequest>()

            val resolvedExecutionId = request.executionId?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()

            logger.info("/rest-input received graphInstanceId={} nodeId={} executionId={}", request.graphInstanceId, request.nodeId, resolvedExecutionId)
            logger.debug("/rest-input payload {}", request.payload)

            val success = restInputRegistry.handleIncoming(
                request.graphInstanceId,
                request.nodeId,
                request.payload,
                resolvedExecutionId
            )

            if (success) {
                logger.info("/rest-input forwarded to active graph graphInstanceId={} nodeId={} executionId={}", request.graphInstanceId, request.nodeId, resolvedExecutionId)
                call.respond(HttpStatusCode.Accepted, mapOf("accepted" to true, "executionId" to resolvedExecutionId))
            } else {
                logger.warn("/rest-input failed to forward graphInstanceId={} nodeId={}", request.graphInstanceId, request.nodeId)
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Failed to forward input to active graph"))
            }
        } catch (e: Exception) {
            logger.warn("/rest-input invalid request: {}", e.message)
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "invalid request")))
        }
    }
}
