package dev.tjpal.api.route

import dev.tjpal.graph.ExecutionOutputStore
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.delete
import io.ktor.server.routing.get

fun Routing.outputsRoute(executionOutputStore: ExecutionOutputStore) {
    get("/executions/{id}/outputs") {
        val id = call.parameters["id"]
        if (id.isNullOrBlank()) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing id"))
            return@get
        }

        val clearParam = call.request.queryParameters["clear"]
        val clear = clearParam?.toBoolean() ?: false

        val outputs = executionOutputStore.getOutputs(id, clear)
        call.respond(HttpStatusCode.OK, outputs)
    }

    delete("/executions/{id}/outputs") {
        val id = call.parameters["id"]
        if (id.isNullOrBlank()) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing id"))
            return@delete
        }

        executionOutputStore.clearOutputs(id)
        call.respond(HttpStatusCode.NoContent)
    }

    // Clears all outputs for all executions
    delete("/executions/outputs") {
        executionOutputStore.clearAll()
        call.respond(HttpStatusCode.NoContent)
    }
}
