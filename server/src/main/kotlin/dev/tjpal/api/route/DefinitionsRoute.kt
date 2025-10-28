package dev.tjpal.api.route

import dev.tjpal.nodes.NodeRepository
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.response.respond

fun Routing.definitionsRoute(nodeRepository: NodeRepository) {
    get("/definitions") {
        call.respond(nodeRepository.getAllDefinitions())
    }
}
