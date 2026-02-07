package dev.tjpal.nodes

import dev.tjpal.api.route.RegistrationToken
import dev.tjpal.api.route.RouteRegistrarHolder
import dev.tjpal.graph.ExecutionResponseAwaiter
import dev.tjpal.graph.status.StatusRegistry
import dev.tjpal.logging.logger
import dev.tjpal.model.NodeParameters
import dev.tjpal.model.RESTRequest
import dev.tjpal.model.RESTResponse
import dev.tjpal.nodes.payload.RawPayload
import io.ktor.http.ContentType.Application
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * RestInputNode registers a Ktor route at activation time using the RouteRegistrar.
 * The node exposes a "Path" parameter (e.g. "/api/some-end-point") that defines the HTTP
 * path to register. onActivate registers the route; onStop unregisters it.
 *
 * RestInputNode offers two modes:
 * - Synchronous: The response contains the output of the execution. The node registers a waiter that is notified by the
 *   OutputSinkNode when the output is sent, and the request handler waits for the notification before responding.
 * - Asynchronous: The response is returned immediately after triggering the execution, and the client is expected to
 *   pick up the output from the output sink in a separate call.
 */
class RestInputNode(
    private val parameters: NodeParameters,
    private val statusRegistry: StatusRegistry,
) : Node {
    private val logger = logger<RestInputNode>()
    private var registrationToken: RegistrationToken? = null
    private val globalResponseTimeout = 10L * 60L * 5L // 5 minutes
    private val json = Json { ignoreUnknownKeys = true }

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
            val request = json.decodeFromString<RESTRequest>(body)
            val executionId = request.executionId ?: UUID.randomUUID().toString()

            val invocation = createInvocation(context, body, executionId)
            val output = createNodeOutput(context, executionId)

            if(request.synchronous) {
                // We pick up the response from the output sink and send it back.
                handleSynchronousPath(call, context, invocation, output, body, executionId)
            } else {
                // The client picks up the response from the output sink in a separate call.
                onEvent(invocation, output)
                respondWithSuccess(call, executionId, "request accepted")
            }

        } catch (e: Exception) {
            logger.error("Unexpected error in request handler for dynamic route {}: {}", normalizedPath, e.message)
            respondWithError(call, executionId = "unknown", errorMessage = "internal server error")
        }
    }

    private suspend fun handleSynchronousPath(
        call: ApplicationCall,
        context: NodeActivationContext,
        invocation: NodeInvocationContext,
        output: NodeOutput,
        body: String,
        executionId: String
    ) {
        val awaiter = ExecutionResponseAwaiter.registerAwaiter(executionId)

        onEvent(invocation, output)

        val result = withTimeoutOrNull(globalResponseTimeout) { awaiter.await() }

        if (result != null) {
            respondWithSuccess(call, executionId, result.payload)
        } else {
            ExecutionResponseAwaiter.cancelAwaiter(executionId)
            respondWithError(call, executionId, "timeout")
        }
    }

    private suspend fun respondWithSuccess(call: ApplicationCall, executionId: String, result: String) {
        val response = RESTResponse(
            executionId = executionId,
            error = false,
            payload = result
        )

        val json = Json.encodeToString(response)

        call.respondText(text = json, contentType = Application.Json, status = HttpStatusCode.OK)
    }

    private suspend fun respondWithError(call: ApplicationCall, executionId: String, errorMessage: String) {
        val response = RESTResponse(
            executionId = executionId,
            error = true,
            payload = errorMessage
        )

        val json = Json.encodeToString(response)

        call.respondText(text = json, contentType = Application.Json, status = HttpStatusCode.InternalServerError)
    }

    private fun createInvocation(context: NodeActivationContext, body: String, executionId: String): NodeInvocationContext {
        return NodeInvocationContext(
            graphInstanceId = context.graphInstanceId,
            executionId = executionId,
            nodeId = context.nodeId,
            payload = RawPayload(body),
            graph = context.graph,
            fromNodeId = null,
            fromConnectorId = null
        )
    }

    private fun createNodeOutput(context: NodeActivationContext, executionId: String): NodeOutput {
        return object : NodeOutput {
            override fun send(outputConnectorId: String, payload: dev.tjpal.nodes.payload.NodePayload) {
                context.graph.routeFromNode(context.nodeId, outputConnectorId, payload, executionId)
            }

            override fun reply(payload: dev.tjpal.nodes.payload.NodePayload) {
                logger.warn("Reply attempted on REST input handler for graphInstanceId={} nodeId={} executionId={}", context.graphInstanceId, context.nodeId, executionId)
            }
        }
    }

    override suspend fun onEvent(context: NodeInvocationContext, output: NodeOutput) {
        output.send("out", context.payload)
        sendStatusEntry(context.payload.asString(), context)

        logger.debug(
            "RestInputNode forwarded payload for graphInstanceId={} nodeId={} executionId={} payload={}",
            context.graphInstanceId, context.nodeId, context.executionId, context.payload.asString()
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
