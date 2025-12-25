package dev.tjpal.api.route

import dev.tjpal.logging.logger
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.post
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

data class RegistrationToken(
    val id: String = UUID.randomUUID().toString()
)

private data class RegisteredRoute(
    val token: RegistrationToken,
    val handlerRef: AtomicReference<suspend (io.ktor.server.application.ApplicationCall) -> Unit?>
)

/**
 * Ktor-based RouteRegistrar which installs a wrapper Ktor route per registered path which consults an in-memory handler reference.
 * Unregistering clears the handler reference and removes the mapping; the wrapper route will return 404
 * for a missing handler. This approach avoids touching Route.parent/children.
 */
class KtorRouteRegistrar(
    private val routing: Routing,
    private val application: Application
) {
    private val logger = logger<KtorRouteRegistrar>()
    private val map: ConcurrentHashMap<String, RegisteredRoute> = ConcurrentHashMap()

    fun register(path: String, handler: suspend (io.ktor.server.application.ApplicationCall) -> Unit): RegistrationToken {
        val normalized = normalizePath(path)

        val token = RegistrationToken()
        val newReg = RegisteredRoute(token = token, handlerRef = AtomicReference(handler))

        val previous = map.putIfAbsent(normalized, newReg)
        if (previous != null) {
            val existingHandler = previous.handlerRef.get()

            if (existingHandler != null) {
                throw IllegalStateException("Path already registered: $normalized")
            } else {
                if (!previous.handlerRef.compareAndSet(null, handler)) {
                    throw IllegalStateException("Failed to register handler for path (concurrent): $normalized")
                }

                logger.info("Registered handler for existing wrapper route {} token={}", normalized, previous.token.id)
                return previous.token
            }
        }

        try {
            application.launch {
                routing.post(normalized) {
                    val registeredRoute = map[normalized]
                    val handler = registeredRoute?.handlerRef?.get()

                    if (handler == null) {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "no handler registered"))
                        return@post
                    }

                    try {
                        handler(call)
                    } catch (e: Exception) {
                        logger.error("Error while invoking dynamic handler for path $normalized: ${e.message}", e)
                        try { call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "handler error")) } catch (_: Exception) {}
                    }
                }

                logger.info("Wrapper route installed for path {}", normalized)
            }
        } catch (e: Exception) {
            map.remove(normalized)
            throw e
        }

        logger.info("Registered dynamic route {} token={}", normalized, token.id)
        return token
    }

    fun unregister(token: RegistrationToken): Boolean {
        val entry = map.entries.find { it.value.token.id == token.id } ?: return false

        entry.value.handlerRef.set(null)
        map.remove(entry.key)

        logger.info("Unregistered dynamic route {} token={}", entry.key, token.id)
        return true
    }

    private fun normalizePath(path: String): String {
        if (!path.startsWith("/")) {
            return "/$path"
        }

        if (path.length > 1 && path.endsWith("/")) {
            return path.removeSuffix("/")
        }

        return path
    }
}
