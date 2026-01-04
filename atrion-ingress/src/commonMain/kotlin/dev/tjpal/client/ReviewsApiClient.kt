package dev.tjpal.client

import dev.tjpal.model.DecisionResponse
import dev.tjpal.model.ReviewDecisionRequest
import dev.tjpal.model.ReviewRecord
import dev.tjpal.model.ReviewStatus
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json

class RESTApiException(val status: HttpStatusCode, message: String) : RuntimeException(message)

class ReviewsApiClient(private val client: HttpClient, private val baseUrl: String) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }

    suspend fun listReviews(graphInstanceId: String? = null, status: ReviewStatus? = null): List<ReviewRecord> {
        val url = buildString {
            append("$baseUrl/reviews")
            var firstQuery = true

        }

        println("ReviewsApiClient.listReviews: url=$url")
        val response: HttpResponse = client.get(url)

        if (!response.status.isSuccess()) {
            throw RESTApiException(response.status, "Failed to fetch reviews: ${response.status}")
        }

        return response.body()
    }

    suspend fun getReview(id: String): ReviewRecord {
        val url = "$baseUrl/reviews/$id"
        val response: HttpResponse = client.get(url)

        if (!response.status.isSuccess()) {
            throw RESTApiException(response.status, "Failed to fetch review: ${response.status}")
        }

        return response.body()
    }

    suspend fun submitDecision(id: String, decision: ReviewDecisionRequest): DecisionResponse {
        val url = "$baseUrl/reviews/$id/decision"

        val response: HttpResponse = client.post(url) {
            contentType(ContentType.Application.Json)
            setBody(decision)
        }

        if (response.status != HttpStatusCode.OK) {
            throw RESTApiException(response.status, "Failed to submit decision: ${response.status}")
        }

        return response.body()
    }
}
