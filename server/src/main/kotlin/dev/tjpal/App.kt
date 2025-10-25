package dev.tjpal

import dev.tjpal.config.Config
import dev.tjpal.nodes.NodeFactory
import dev.tjpal.nodes.NodeRepository
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.cio.unixConnector
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import java.nio.file.Files
import java.nio.file.Paths
import javax.inject.Inject
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlinx.serialization.json.Json

class App @Inject constructor(
    private val config: Config,
    private val nodeRepository: NodeRepository
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
            module = { module(nodeRepository) }
        )

        return server
    }

    private fun forceInitialSocketState(config: Config) {
        val socketPath = Paths.get(config.udsPath)

        // Cleanup any stale socket file
        if (socketPath.exists() && socketPath.isRegularFile()) {
            Files.delete(socketPath)
        }

        // Ensure parent directories exist
        if (!socketPath.parent.exists()) {
            Files.createDirectories(socketPath.parent)
        }
    }
}

fun Application.module(nodeRepository: NodeRepository) {
    val defaultJson = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    install(ContentNegotiation) {
        json(defaultJson)
    }

    routing {
        get("/") {
            call.respondText(
                "Ktor on " + (System.getenv("SERVER_UNIX_SOCKET")?.let { "UDS: $it" }
                    ?: "TCP: ${System.getenv("HOST") ?: "0.0.0.0"}:${System.getenv("PORT") ?: "8080"}")
            )
        }

        get("/definitions") {
            call.respond(nodeRepository.getAllDefinitions())
        }
    }
}