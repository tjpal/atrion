package dev.tjpal.api.route

import dev.tjpal.graph.ActiveGraphRepository
import dev.tjpal.graph.GraphDefinitionRepository
import dev.tjpal.model.ExecutionStartRequest
import dev.tjpal.logging.logger
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

private object ExecutionsRouteLogTag
private val logger = logger<ExecutionsRouteLogTag>()

fun Routing.executionsRoute(graphRepository: GraphDefinitionRepository, activeGraphRepository: ActiveGraphRepository) {
    route("/executions") {
        get {
            logger.info("GET /executions")
            call.respond(activeGraphRepository.listAll())
        }

        post {
            try {
                val req = call.receive<ExecutionStartRequest>()
                logger.info("POST /executions start request for graphId={}", req.graphId)

                try {
                    graphRepository.get(req.graphId)
                } catch (e: IllegalArgumentException) {
                    logger.warn("Attempt to start execution for unknown graphId={}", req.graphId)
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Graph with id ${req.graphId} does not exist"))
                    return@post
                }

                val graphInstanceId = activeGraphRepository.start(req.graphId)
                logger.info("Started execution graphInstanceId={} graphId={}", graphInstanceId, req.graphId)

                val created = mapOf("graphInstanceId" to graphInstanceId)
                call.respond(HttpStatusCode.Created, created)
            } catch (e: Exception) {
                logger.warn("POST /executions failed: {}", e.message)
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "invalid request")))
            }
        }

        delete("/{id}") {
            val id = call.parameters["id"]

            if (id.isNullOrBlank()) {
                logger.warn("DELETE /executions missing id")
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing execution id"))
                return@delete
            }

            try {
                activeGraphRepository.delete(id)
                logger.info("Deleted execution id={}", id)
                call.respond(HttpStatusCode.NoContent)
            } catch (e: IllegalArgumentException) {
                logger.warn("Failed to delete execution id={}: {}", id, e.message)
                call.respond(HttpStatusCode.NotFound, mapOf("error" to (e.message ?: "not found")))
            }
        }
    }
}
