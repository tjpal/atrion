package dev.tjpal.api.route

import dev.tjpal.graph.ExecutionOutputStore
import dev.tjpal.logging.logger
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.delete
import io.ktor.server.routing.get

private object OutputsRouteLogTag
private val logger = logger<OutputsRouteLogTag>()

fun Routing.outputsRoute(executionOutputStore: ExecutionOutputStore) {
    get("/executions/{id}/outputs") {
        val id = call.parameters["id"]
        if (id.isNullOrBlank()) {
            logger.warn("GET /executions/{id}/outputs missing id")
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing id"))
            return@get
        }

        val clearParam = call.request.queryParameters["clear"]
        val clear = clearParam?.toBoolean() ?: false

        logger.info("GET /executions/{}/outputs clear={}", id, clear)

        val outputs = executionOutputStore.getOutputs(id, clear)
        logger.debug("Returning {} outputs for executionId={}", outputs.size, id)
        call.respond(HttpStatusCode.OK, outputs)
    }

    delete("/executions/{id}/outputs") {
        val id = call.parameters["id"]
        if (id.isNullOrBlank()) {
            logger.warn("DELETE /executions/{id}/outputs missing id")
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing id"))
            return@delete
        }

        logger.info("DELETE /executions/{}/outputs", id)
        executionOutputStore.clearOutputs(id)
        call.respond(HttpStatusCode.NoContent)
    }

    // Clears all outputs for all executions
    delete("/executions/outputs") {
        logger.info("DELETE /executions/outputs clearing all outputs")
        executionOutputStore.clearAll()
        call.respond(HttpStatusCode.NoContent)
    }
}
