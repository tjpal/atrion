package dev.tjpal.api.route

import io.ktor.server.routing.Routing
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.Instant
import kotlin.random.Random
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("dev.tjpal.api.route.EventsWebsocketRoute")

fun Routing.eventsWebsocketRoute() {
    webSocket("/events") {
        logger.info("New websocket connection from {}", this.call.request.local.remoteHost)

        try {
            var counter = 0

            while (isActive) {
                val timestamp = Instant.now().toString()
                val message = "$timestamp: Random event #${++counter}"

                try {
                    outgoing.send(Frame.Text(message))
                    logger.info("Sent: {}", message)
                } catch (e: Exception) {
                    logger.error("Failed to send frame", e)
                    break
                }

                val delayMs = Random.nextLong(1000L, 5001L)
                delay(delayMs)
            }
        } catch (e: Exception) {
            logger.error("Sender loop ended", e)
        } finally {
            logger.info("Websocket connection closed for {}", this.call.request.local.remoteHost)
        }
    }
}
