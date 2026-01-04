package dev.tjpal.client

import dev.tjpal.model.DecisionResponse
import dev.tjpal.model.ReviewDecision
import dev.tjpal.model.ReviewDecisionRequest
import dev.tjpal.model.ReviewRecord
import dev.tjpal.model.ReviewStatus
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }

class ReviewsApiClientMockEngineTest {

    private fun createApi(handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): ReviewsApiClient {
        val engine = MockEngine(handler = handler)
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        return ReviewsApiClient(client, "http://localhost")
    }

    @Test
    fun listReviews_success() = runBlocking {
        val sample = ReviewRecord(
            reviewId = "r1",
            graphInstanceId = "g1",
            executionId = "e1",
            nodeId = "n1",
            reviewInstructions = "Do X",
            reviewDecisionDescription = "",
            timestamp = 0L,
            status = ReviewStatus.PENDING
        )

        val api = createApi { request ->
            when (request.url.encodedPath) {
                "/reviews" -> {
                    val body = json.encodeToString(ListSerializer(ReviewRecord.serializer()), listOf(sample))
                    respond(body, HttpStatusCode.OK, headersOf("Content-Type" to listOf(ContentType.Application.Json.toString())))
                }
                else -> respondError(HttpStatusCode.NotFound)
            }
        }

        val list = api.listReviews()

        assertEquals(1, list.size)
        assertEquals("r1", list[0].reviewId)
    }

    @Test
    fun getReview_success() = runBlocking {
        val sample = ReviewRecord(
            reviewId = "r2",
            graphInstanceId = "g2",
            executionId = "e2",
            nodeId = "n2",
            reviewInstructions = "Check Y",
            reviewDecisionDescription = "",
            timestamp = 1L,
            status = ReviewStatus.PENDING
        )

        val api = createApi { request ->
            when (request.url.encodedPath) {
                "/reviews/r2" -> {
                    val body = json.encodeToString(ReviewRecord.serializer(), sample)
                    respond(body, HttpStatusCode.OK, headersOf("Content-Type" to listOf(ContentType.Application.Json.toString())))
                }
                else -> respondError(HttpStatusCode.NotFound)
            }
        }

        val result = api.getReview("r2")

        assertEquals("r2", result.reviewId)
    }

    @Test
    fun getReview_notFound_throws() = runBlocking {
        val api = createApi { request -> respondError(HttpStatusCode.NotFound) }

        val exception = assertFailsWith<RESTApiException> { api.getReview("missing") }
        assertEquals(HttpStatusCode.NotFound, exception.status)
    }

    @Test
    fun submitDecision_success() = runBlocking {
        val api = createApi { request ->
            when (request.url.encodedPath) {
                "/reviews/r3/decision" -> {
                    val body = json.encodeToString(DecisionResponse.serializer(), DecisionResponse(delivered = true, deliveredMessage = "Delivered"))
                    respond(body, HttpStatusCode.OK, headersOf("Content-Type" to listOf(ContentType.Application.Json.toString())))
                }
                else -> respondError(HttpStatusCode.NotFound)
            }
        }

        val decision = ReviewDecisionRequest(decision = ReviewDecision.ACCEPTED)
        val result = api.submitDecision("r3", decision)

        assertTrue(result.delivered)
        assertEquals("Delivered", result.deliveredMessage)
    }
}
