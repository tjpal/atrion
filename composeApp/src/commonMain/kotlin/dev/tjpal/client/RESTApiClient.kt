package dev.tjpal.client

import dev.tjpal.model.ConnectorOutput
import dev.tjpal.model.ExecutionStartRequest
import dev.tjpal.model.GraphDefinition
import dev.tjpal.model.GraphExecutionStatus
import dev.tjpal.model.NodeDefinition
import dev.tjpal.model.RestInputRequest
import dev.tjpal.model.StatusEntry
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json

class RESTApiException(val status: HttpStatusCode, message: String) : RuntimeException(message)

class RESTApiClient(private val client: HttpClient, private val baseUrl: String) {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }

    suspend fun getNodeDefinitions(): List<NodeDefinition> {
        val url = "$baseUrl/definitions"
        val response: HttpResponse = client.get(url)
        if (!response.status.isSuccess()) {
            throw RESTApiException(response.status, "Failed to fetch definitions: ${response.status}")
        }
        return response.body()
    }

    suspend fun createGraph(graph: GraphDefinition): String {
        val url = "$baseUrl/graphs"
        val response: HttpResponse = client.post(url) {
            contentType(ContentType.Application.Json)
            setBody(graph)
        }
        if (response.status != HttpStatusCode.Created) {
            throw RESTApiException(response.status, "Failed to create graph: ${response.status}")
        }
        return response.body()
    }

    suspend fun replaceGraph(id: String, graph: GraphDefinition) {
        val url = "$baseUrl/graphs/$id"
        val response: HttpResponse = client.put(url) {
            contentType(ContentType.Application.Json)
            setBody(graph.copy(id = id))
        }
        if (response.status != HttpStatusCode.OK) {
            throw RESTApiException(response.status, "Failed to replace graph: ${response.status}")
        }
    }

    suspend fun getGraphs(): List<GraphDefinition> {
        val url = "$baseUrl/graphs"
        val response: HttpResponse = client.get(url)
        if (!response.status.isSuccess()) {
            throw RESTApiException(response.status, "Failed to fetch graphs: ${response.status}")
        }
        return response.body()
    }

    suspend fun deleteGraph(id: String) {
        val url = "$baseUrl/graphs/$id"
        val response: HttpResponse = client.delete(url)
        if (response.status != HttpStatusCode.NoContent) {
            throw RESTApiException(response.status, "Failed to delete graph: ${response.status}")
        }
    }

    suspend fun listExecutions(): List<GraphExecutionStatus> {
        val url = "$baseUrl/executions"
        val response: HttpResponse = client.get(url)
        if (!response.status.isSuccess()) {
            throw RESTApiException(response.status, "Failed to list executions: ${response.status}")
        }
        return response.body()
    }

    suspend fun createGraphInstance(graphId: String): String {
        val url = "$baseUrl/executions"
        val response: HttpResponse = client.post(url) {
            contentType(ContentType.Application.Json)
            setBody(ExecutionStartRequest(graphId))
        }
        if (response.status != HttpStatusCode.Created) {
            throw RESTApiException(response.status, "Failed to start execution: ${response.status}")
        }
        val map: Map<String, String> = response.body()
        return map["graphInstanceId"] ?: throw RESTApiException(response.status, "Missing graph instance id in response")
    }

    suspend fun deleteGraphInstance(graphId: String) {
        val url = "$baseUrl/executions/$graphId"
        val response: HttpResponse = client.delete(url)
        if (response.status != HttpStatusCode.NoContent) {
            throw RESTApiException(response.status, "Failed to delete execution: ${response.status}")
        }
    }

    suspend fun getExecutionOutputs(executionId: String): List<ConnectorOutput> {
        val url = "$baseUrl/executions/$executionId/outputs"
        val response: HttpResponse = client.get(url)
        if (!response.status.isSuccess()) {
            throw RESTApiException(response.status, "Failed to fetch outputs: ${response.status}")
        }
        return response.body()
    }

    suspend fun sendRestInput(graphInstanceId: String, nodeId: String, payload: String): String {
        val url = "$baseUrl/rest-input"
        val response: HttpResponse = client.post(url) {
            contentType(ContentType.Application.Json)
            setBody(RestInputRequest(graphInstanceId, nodeId, payload))
        }
        return when (response.status) {
            HttpStatusCode.Accepted -> {
                val map: Map<String, Any> = response.body()
                map["executionId"] as? String
                    ?: throw RESTApiException(response.status, "Missing execution id in response")
            }
            else -> throw RESTApiException(response.status, "Failed to send rest input: ${response.status}")
        }
    }

    suspend fun getStatuses(since: Long? = null): List<StatusEntry> {
        val url = if (since != null) "$baseUrl/events/statuses?since=$since" else "$baseUrl/events/statuses"
        val response: HttpResponse = client.get(url)
        if (!response.status.isSuccess()) {
            throw RESTApiException(response.status, "Failed to fetch statuses: ${response.status}")
        }
        return response.body()
    }

    suspend fun streamStatuses(since: Long = 0L, onStatus: suspend (StatusEntry) -> Unit) {
        val streamUrl = "$baseUrl/events/statuses/stream?since=$since"

        client.webSocket(urlString = streamUrl) {
            while (!incoming.isClosedForReceive) {
                val frame = try {
                    incoming.receive()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // If the stream is closed, finish the method.
                    break
                }

                if (frame is Frame.Text) {
                    val jsonPayload = frame.readText()

                    // Pass deserialization exceptions to the caller. They need to decide what to do.
                    val entry = json.decodeFromString(StatusEntry.serializer(), jsonPayload)
                    onStatus(entry)
                }
            }
        }
    }
}
