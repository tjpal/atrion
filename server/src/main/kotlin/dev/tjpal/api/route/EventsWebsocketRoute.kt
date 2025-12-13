package dev.tjpal.api.route

import io.ktor.server.routing.Routing
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.Instant
import kotlin.random.Random

fun Routing.eventsWebsocketRoute() {
    webSocket("/events") {
        println("New websocket connection from ${this.call.request.local.remoteHost}")

        try {
            var counter = 0

            while (isActive) {
                val timestamp = Instant.now().toString()
                val message = "$timestamp: Random event #${++counter}"

                try {
                    outgoing.send(Frame.Text(message))
                    println("Sent: $message")
                } catch (e: Exception) {
                    println("Failed to send frame: ${e.message}")
                    break
                }

                val delayMs = Random.nextLong(1000L, 5001L)
                delay(delayMs)
            }
        } catch (e: Exception) {
            println("Sender loop ended: ${e.message}")
        } finally {
            println("Websocket connection closed for ${this.call.request.local.remoteHost}")
        }
    }
}
