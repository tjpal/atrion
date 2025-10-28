package dev.tjpal.api.route

import dev.tjpal.graph.GraphDefinitionRepository
import dev.tjpal.model.GraphDefinition
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.delete
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Routing.graphsRoute(graphRepository: GraphDefinitionRepository) {
    route("/graphs") {
        post {
            try {
                val incoming = call.receive<GraphDefinition>()
                val id = graphRepository.add(incoming)
                call.respond(HttpStatusCode.Created, id)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "invalid request")))
            }
        }

        put("/{id}") {
            // Replace an existing graph by id. The repository enforces existence and will throw if the id does not exist.
            val id = call.parameters["id"]

            if (id.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing id"))
                return@put
            }

            try {
                val incoming = call.receive<GraphDefinition>()

                try {
                    graphRepository.replace(id, incoming.copy(id = id))
                    call.respond(HttpStatusCode.OK)
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to (e.message ?: "not found")))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "invalid request")))
            }
        }

        delete("/{id}") {
            val id = call.parameters["id"]
            if (id.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing id"))
                return@delete
            }

            try {
                graphRepository.delete(id)
                call.respond(HttpStatusCode.NoContent)
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to (e.message ?: "not found")))
            }
        }
    }
}
