package dev.tjpal.api.route

import dev.tjpal.graph.status.StatusRegistry
import dev.tjpal.logging.logger
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.Json

private object EventsWebsocketRouteLogTag
private val logger = logger<EventsWebsocketRouteLogTag>()

fun Routing.statusesRoute(statusRegistry: StatusRegistry, json: Json) {
    get("/events/statuses") {
        val sinceParam = call.request.queryParameters["since"]

        if (sinceParam.isNullOrBlank()) {
            val allResults = statusRegistry.getStatuses()
            logger.info("GET /events/statuses returning {} total entries", allResults.size)
            call.respond(HttpStatusCode.OK, allResults)
            return@get
        }

        val since = try {
            sinceParam.toLong()
        } catch (e: Exception) {
            logger.warn("GET /events/statuses invalid since param: {}", sinceParam)
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid 'since' parameter"))
            return@get
        }

        val results = statusRegistry.getStatusesSince(since)
        logger.debug("GET /events/statuses since={} returning {} entries", since, results.size)
        call.respond(HttpStatusCode.OK, results)
    }

    webSocket("/events/statuses/stream") {
        logger.info("New websocket connection from {}", this.call.request.local.remoteHost)

        val sinceParam = call.request.queryParameters["since"]
        val initialSince = try {
            sinceParam?.toLong() ?: 0L
        } catch (e: Exception) {
            logger.warn("Websocket invalid since parameter: {}", sinceParam)
            close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Invalid 'since' parameter"))
            return@webSocket
        }

        var currentSince = initialSince

        try {
            val backlog = statusRegistry.getStatusesSince(currentSince)

            for (entry in backlog) {
                val statusAsJson = json.encodeToString(entry)

                try {
                    outgoing.send(Frame.Text(statusAsJson))
                    logger.debug("Websocket sent backlog status timestamp={} to {}", entry.timestamp, this.call.request.local.remoteHost)
                } catch (e: Exception) {
                    logger.error("Failed to send backlog frame", e)
                    close(CloseReason(CloseReason.Codes.INTERNAL_ERROR, "Failed to send backlog"))
                    return@webSocket
                }

                if (entry.timestamp > currentSince) {
                    currentSince = entry.timestamp
                }
            }

            while (isActive) {
                try {
                    val newEntries = statusRegistry.waitForNewStatus(currentSince)

                    for (entry in newEntries) {
                        val statusAsJson = json.encodeToString(entry)

                        try {
                            logger.debug("Sending status entry: $statusAsJson")
                            outgoing.send(Frame.Text(statusAsJson))
                        } catch (e: Exception) {
                            logger.error("Failed to send frame", e)
                            close(CloseReason(CloseReason.Codes.INTERNAL_ERROR, "Failed to send frame"))
                            return@webSocket
                        }

                        if (entry.timestamp > currentSince) {
                            currentSince = entry.timestamp
                        }
                    }
                } catch (e: Exception) {
                    logger.info("Websocket sender loop ended", e)
                    break
                }
            }
        } finally {
            logger.info("Websocket connection closed for {}", this.call.request.local.remoteHost)
        }
    }
}
