package dev.tjpal.nodes

import dev.tjpal.api.route.RegistrationToken
import dev.tjpal.api.route.RouteRegistrarHolder
import dev.tjpal.graph.status.StatusRegistry
import dev.tjpal.logging.logger
import dev.tjpal.model.NodeParameters
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import java.util.*

/**
 * RestInputNode registers a Ktor route at activation time using the RouteRegistrar.
 * The node exposes a "Path" parameter (e.g. "/api/some-end-point") that defines the HTTP
 * path to register. onActivate registers the route; onStop unregisters it.
 */
class RestInputNode(
    private val parameters: NodeParameters,
    private val statusRegistry: StatusRegistry
) : Node {
    private val logger = logger<RestInputNode>()
    private var registrationToken: RegistrationToken? = null

    override fun onActivate(context: NodeActivationContext) {
        logger.info("RestInputNode.onActivate graphInstanceId={} nodeId={}", context.graphInstanceId, context.nodeId)

        val path = parameters.values["Path"] ?: ""
        if (path.isBlank()) {
            logger.error("RestInputNode activation failed: missing Path parameter for node {}", context.nodeId)
            return
        }

        val registrar = RouteRegistrarHolder.getRegistrar()
        val normalizedPath = if (path.startsWith("/")) path else "/$path"
        val handler = createRequestHandler(context, normalizedPath)

        try {
            val token = registrar.register(normalizedPath, handler = handler)
            registrationToken = token

            logger.info("RestInputNode registered dynamic route {} for graphInstanceId={} nodeId={} token={}", normalizedPath, context.graphInstanceId, context.nodeId, token.id)
        } catch (e: Exception) {
            logger.error("Failed to register dynamic route {} for node {}: {}", normalizedPath, context.nodeId, e.message)
            return
        }
    }

    private fun createRequestHandler(context: NodeActivationContext, normalizedPath: String): suspend (ApplicationCall) -> Unit = { call ->
        try {
            val body = runCatching { call.receiveText() }.getOrElse { "" }
            val executionId = UUID.randomUUID().toString()

            val invocation = NodeInvocationContext(
                graphInstanceId = context.graphInstanceId,
                executionId = executionId,
                nodeId = context.nodeId,
                payload = body,
                graph = context.graph
            )

            val output = object : NodeOutput {
                override fun send(outputConnectorId: String, payload: String) {
                    context.graph.routeFromNode(context.nodeId, outputConnectorId, payload, executionId)
                }
            }

            try {
                this.onEvent(invocation, output)

                call.respond(HttpStatusCode.Accepted, mapOf("executionId" to executionId))
            } catch (e: Exception) {
                logger.error("Error while handling dynamic route {}: {}", normalizedPath, e.message)
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "processing error")))
            }
        } catch (e: Exception) {
            logger.error("Unexpected error in request handler for dynamic route {}: {}", normalizedPath, e.message)
            try { call.respond(HttpStatusCode.InternalServerError) } catch (_: Exception) {}
        }
    }

    override suspend fun onEvent(context: NodeInvocationContext, output: NodeOutput) {
        output.send("out", context.payload)
        sendStatusEntry(context.payload, context)

        logger.debug(
            "RestInputNode forwarded payload for graphInstanceId={} nodeId={} executionId={} payload={}",
            context.graphInstanceId, context.nodeId, context.executionId, context.payload
        )
    }

    private fun sendStatusEntry(payload: String, context: NodeInvocationContext) {
        val statusEntry = dev.tjpal.model.StatusEntry(
            graphInstanceId = context.graphInstanceId,
            executionId = context.executionId,
            nodeId = context.nodeId,
            timestamp = System.currentTimeMillis(),
            nodeStatus = dev.tjpal.model.NodeStatus.FINISHED,
            outputPayload = payload
        )

        statusRegistry.registerStatusEvent(statusEntry)
    }

    override fun onStop(context: NodeDeactivationContext) {
        logger.info("RestInputNode.onStop graphInstanceId={} nodeId={}", context.graphInstanceId, context.nodeId)

        val token = registrationToken
        if (token != null) {
            try {
                val registrar = RouteRegistrarHolder.getRegistrar()
                registrar.unregister(token)

                logger.info("RestInputNode unregistered dynamic route token={} for node {}", token.id, context.nodeId)
            } catch (e: Exception) {
                logger.error("Failed to unregister route for node {}: {}", context.nodeId, e.message)
            }
        }
    }
}
