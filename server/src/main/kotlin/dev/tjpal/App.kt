package dev.tjpal

import dev.tjpal.config.Config
import io.ktor.server.application.Application
import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.cio.unixConnector
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import java.nio.file.Files
import java.nio.file.Paths
import javax.inject.Inject
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile

class App @Inject constructor(private val config: Config) {
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
            module = Application::module
        )

        return server
    }

    private fun forceInitialSocketState(config: Config) {
        val socketPath = Paths.get(config.udsPath)

        // Cleanup any stale socket file
        if(socketPath.exists() && socketPath.isRegularFile()) {
            Files.delete(socketPath)
        }

        // Ensure parent directories exist
        if(!socketPath.parent.exists()) {
            Files.createDirectories(socketPath.parent)
        }
    }
}

fun Application.module() {
    routing {
        get("/") {
            call.respondText("Ktor on ${System.getenv("SERVER_UNIX_SOCKET")?.let { "UDS: $it" } ?: "TCP: ${System.getenv("HOST") ?: "0.0.0.0"}:${System.getenv("PORT") ?: "8080"}"}")
        }
    }
}