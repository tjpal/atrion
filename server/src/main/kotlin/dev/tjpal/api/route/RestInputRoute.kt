package dev.tjpal.api.route

import dev.tjpal.graph.hooks.RestInputRegistry
import dev.tjpal.model.RestInputRequest
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.http.HttpStatusCode
import java.util.UUID

fun Route.restInputRoutes(restInputRegistry: RestInputRegistry) {
    post("/rest-input") {
        try {
            val request = call.receive<RestInputRequest>()

            val resolvedExecutionId = request.executionId?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()

            val success = restInputRegistry.handleIncoming(
                request.graphInstanceId,
                request.nodeId,
                request.payload,
                resolvedExecutionId
            )

            if (success) {
                call.respond(HttpStatusCode.Accepted, mapOf("accepted" to true, "executionId" to resolvedExecutionId))
            } else {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Failed to forward input to active graph"))
            }
        } catch (e: Exception) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "invalid request")))
        }
    }
}
