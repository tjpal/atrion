package dev.tjpal.api.route

import dev.tjpal.graph.GraphDefinitionRepository
import dev.tjpal.logging.logger
import dev.tjpal.model.GraphDefinition
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

private object GraphsRouteLogTag
private val logger = logger<GraphsRouteLogTag>()

fun Routing.graphsRoute(graphRepository: GraphDefinitionRepository) {
    route("/graphs") {
        post {
            try {
                logger.info("POST /graphs received")

                val incoming = call.receive<GraphDefinition>()
                logger.debug("Incoming graph definition: {}",incoming)

                val id = graphRepository.add(incoming)
                logger.info("Graph created id={}", id)

                call.respond(HttpStatusCode.Created, id)
            } catch (e: Exception) {
                logger.warn("POST /graphs failed: {}", e.message)
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "invalid request")))
            }
        }

        put("/{id}") {
            val id = call.parameters["id"]

            if (id.isNullOrBlank()) {
                logger.warn("PUT /graphs missing id")
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing id"))
                return@put
            }

            try {
                val incoming = call.receive<GraphDefinition>()
                logger.debug("PUT /graphs/{} incoming {}", id, incoming)

                try {
                    graphRepository.replace(id, incoming.copy(id = id))
                    logger.info("Graph replaced id={}", id)
                    call.respond(HttpStatusCode.OK)
                } catch (e: IllegalArgumentException) {
                    logger.warn("Graph not found for replace id={}", id)
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to (e.message ?: "not found")))
                }
            } catch (e: Exception) {
                logger.warn("PUT /graphs/{} failed: {}", id, e.message)
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "invalid request")))
            }
        }

        get {
            logger.info("GET /graphs")
            val all = graphRepository.getAll()
            call.respond(all)
        }

        delete("/{id}") {
            val id = call.parameters["id"]
            if (id.isNullOrBlank()) {
                logger.warn("DELETE /graphs missing id")
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing id"))
                return@delete
            }

            try {
                graphRepository.delete(id)
                logger.info("Graph deleted id={}", id)
                call.respond(HttpStatusCode.NoContent)
            } catch (e: IllegalArgumentException) {
                logger.warn("Graph delete failed, not found id={}", id)
                call.respond(HttpStatusCode.NotFound, mapOf("error" to (e.message ?: "not found")))
            }
        }
    }
}
