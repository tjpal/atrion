package dev.tjpal.api.route

import dev.tjpal.logging.logger
import dev.tjpal.nodes.NodeRepository
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get

private object DefinitionsRouteLogTag
private val logger = logger<DefinitionsRouteLogTag>()

fun Routing.definitionsRoute(nodeRepository: NodeRepository) {
    get("/definitions") {
        logger.info("GET /definitions called")

        val definitions = nodeRepository.getAllDefinitions()
        logger.debug("Returning {} node definitions", definitions.size)

        call.respond(definitions)
    }
}
