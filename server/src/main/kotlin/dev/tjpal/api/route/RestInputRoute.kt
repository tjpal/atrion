package dev.tjpal.api.route

import dev.tjpal.graph.hooks.RestInputRegistry
import dev.tjpal.model.RestInputRequest
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.http.HttpStatusCode

fun Route.restInputRoutes(restInputRegistry: RestInputRegistry) {
    post("/rest-input") {
        try {
            val request = call.receive<RestInputRequest>()
            val success = restInputRegistry.handleIncoming(
                request.executionId,
                request.nodeId,
                request.payload
            )

            if (success) {
                call.respond(HttpStatusCode.Accepted, mapOf("accepted" to true))
            } else {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Failed to forward input to active graph"))
            }
        } catch (e: Exception) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "invalid request")))
        }
    }
}
