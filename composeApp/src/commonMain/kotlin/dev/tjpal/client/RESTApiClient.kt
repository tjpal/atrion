package dev.tjpal.client

import dev.tjpal.model.*
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

class RESTApiException(val status: HttpStatusCode, message: String) : RuntimeException(message)

class RESTApiClient(private val client: HttpClient, private val baseUrl: String) {

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

    suspend fun startExecution(graphId: String): String {
        val url = "$baseUrl/executions"
        val response: HttpResponse = client.post(url) {
            contentType(ContentType.Application.Json)
            setBody(ExecutionStartRequest(graphId))
        }
        if (response.status != HttpStatusCode.Created) {
            throw RESTApiException(response.status, "Failed to start execution: ${response.status}")
        }
        val map: Map<String, String> = response.body()
        return map["executionId"] ?: throw RESTApiException(response.status, "Missing executionId in response")
    }

    suspend fun deleteExecution(id: String) {
        val url = "$baseUrl/executions/$id"
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

    suspend fun sendRestInput(executionId: String, nodeId: String, payload: String): Boolean {
        val url = "$baseUrl/rest-input"
        val response: HttpResponse = client.post(url) {
            contentType(ContentType.Application.Json)
            setBody(RestInputRequest(executionId, nodeId, payload))
        }
        return when (response.status) {
            HttpStatusCode.Accepted -> true
            else -> throw RESTApiException(response.status, "Failed to send rest input: ${response.status}")
        }
    }
}
