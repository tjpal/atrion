package dev.tjpal.api.route

import dev.tjpal.graph.ActiveGraphRepository
import dev.tjpal.graph.GraphDefinitionRepository
import dev.tjpal.model.ExecutionStartRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Routing.executionsRoute(graphRepository: GraphDefinitionRepository, activeGraphRepository: ActiveGraphRepository) {
    route("/executions") {
        get {
            call.respond(activeGraphRepository.listAll())
        }

        post {
            try {
                val req = call.receive<ExecutionStartRequest>()

                try {
                    graphRepository.get(req.graphId)
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Graph with id ${req.graphId} does not exist"))
                    return@post
                }

                val executionId = activeGraphRepository.start(req.graphId)

                val created = mapOf("executionId" to executionId)
                call.respond(HttpStatusCode.Created, created)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "invalid request")))
            }
        }

        delete("/{id}") {
            val id = call.parameters["id"]

            if (id.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing execution id"))
                return@delete
            }

            try {
                activeGraphRepository.delete(id)
                call.respond(HttpStatusCode.NoContent)
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to (e.message ?: "not found")))
            }
        }
    }
}
