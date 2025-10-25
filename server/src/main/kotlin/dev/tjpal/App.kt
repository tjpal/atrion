package dev.tjpal

import dev.tjpal.config.Config
import dev.tjpal.graph.GraphDefinitionRepository
import dev.tjpal.nodes.NodeRepository
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.cio.unixConnector
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.delete
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.http.HttpStatusCode
import java.nio.file.Files
import java.nio.file.Paths
import javax.inject.Inject
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlinx.serialization.json.Json
import dev.tjpal.graph.model.GraphDefinition

class App @Inject constructor(
    private val config: Config,
    private val nodeRepository: NodeRepository,
    private val graphRepository: GraphDefinitionRepository
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
            module = { module(nodeRepository, graphRepository) }
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

fun Application.module(nodeRepository: NodeRepository, graphRepository: GraphDefinitionRepository) {
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

        route("/graphs") {
            post {
                try {
                    val incoming = call.receive<GraphDefinition>()
                    val id = graphRepository.add(incoming)
                    val created = incoming.copy(id = id)
                    call.respond(HttpStatusCode.Created, created)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "invalid request")))
                }
            }

            get("/{id}") {
                val id = call.parameters["id"]
                if (id.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing id"))
                    return@get
                }

                try {
                    val graph = graphRepository.get(id)
                    call.respond(graph)
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to (e.message ?: "not found")))
                }
            }

            delete("/{id}") {
                val id = call.parameters["id"]
                if (id.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing id"))
                    return@delete
                }

                try {
                    graphRepository.delete(id)
                    call.respond(HttpStatusCode.NoContent)
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to (e.message ?: "not found")))
                }
            }
        }
    }
}
