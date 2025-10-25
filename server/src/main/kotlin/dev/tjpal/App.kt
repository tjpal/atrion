package dev.tjpal

import dev.tjpal.api.route.definitionsRoute
import dev.tjpal.api.route.graphsRoute
import dev.tjpal.api.route.executionsRoute
import dev.tjpal.config.Config
import dev.tjpal.graph.ActiveGraphRepository
import dev.tjpal.graph.GraphDefinitionRepository
import dev.tjpal.nodes.NodeRepository
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.cio.unixConnector
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.connector
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import java.nio.file.Paths
import javax.inject.Inject
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile


class App @Inject constructor(
    private val config: Config,
    private val nodeRepository: NodeRepository,
    private val graphRepository: GraphDefinitionRepository,
    private val activeGraphRepository: ActiveGraphRepository
) {
    fun run() {
        val server = configureServer(config)
        server.start(wait = true)
    }

    private fun configureServer(config: Config): EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration> {
        val server = embeddedServer(
            CIO,
            configure = {
                if (config.udsPath.isNotEmpty()) {
                    forceInitialSocketState(config)
                    unixConnector(config.udsPath)
                } else {
                    connector {
                        host = config.httpHost
                        port = config.httpPort
                    }
                }
            },
            module = { module(nodeRepository, graphRepository, activeGraphRepository) }
        )

        return server
    }

    private fun forceInitialSocketState(config: Config) {
        val socketPath = Paths.get(config.udsPath)

        // Cleanup any stale socket file
        if (socketPath.exists() && socketPath.isRegularFile()) {
            java.nio.file.Files.delete(socketPath)
        }

        // Ensure parent directories exist
        if (!socketPath.parent.exists()) {
            java.nio.file.Files.createDirectories(socketPath.parent)
        }
    }
}

fun Application.module(nodeRepository: NodeRepository, graphRepository: GraphDefinitionRepository, activeGraphRepository: ActiveGraphRepository) {
    val defaultJson = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    install(ContentNegotiation) {
        json(defaultJson)
    }

    routing {
        definitionsRoute(nodeRepository)
        graphsRoute(graphRepository)
        executionsRoute(graphRepository, activeGraphRepository)
    }
}
