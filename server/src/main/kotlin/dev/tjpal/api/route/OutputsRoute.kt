package dev.tjpal.api.route

import dev.tjpal.graph.ExecutionOutputStore
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get

fun Routing.outputsRoute(executionOutputStore: ExecutionOutputStore) {
    get("/executions/{id}/outputs") {
        val id = call.parameters["id"]
        if (id.isNullOrBlank()) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing id"))
            return@get
        }

        val outputs = executionOutputStore.getOutputs(id)
        call.respond(HttpStatusCode.OK, outputs)
    }
}
