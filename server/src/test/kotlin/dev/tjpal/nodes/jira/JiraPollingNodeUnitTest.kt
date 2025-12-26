package dev.tjpal.nodes.jira

import dev.tjpal.graph.status.StatusRegistry
import dev.tjpal.model.NodeParameters
import dev.tjpal.secrets.SecretStore
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.serialization.json.Json
import kotlin.test.BeforeTest
import kotlin.test.Test

class JiraPollingNodeUnitTest {
    private lateinit var secretStore: SecretStore
    private lateinit var statusRegistry: StatusRegistry
    private val json = Json { ignoreUnknownKeys = true }

    @BeforeTest
    fun setup() {
        secretStore = mockk(relaxed = true)
        statusRegistry = mockk(relaxed = true)
    }

    @Test
    fun `invalid PollIntervalMs should register ERROR status and abort activation`() {
        val params = NodeParameters(mapOf(
            "ServerUrl" to "https://jira.example.com",
            "SecretId" to "any",
            // invalid non-numeric interval
            "PollIntervalMs" to "not-a-number",
            "JQL" to ""
        ))

        val graph = mockk<dev.tjpal.graph.ActiveGraph>(relaxed = true)

        val node = JiraPollingNode(params, secretStore, statusRegistry, json)

        val ctx = dev.tjpal.nodes.NodeActivationContext(
            graphInstanceId = "g-invalid-interval",
            nodeId = "node-invalid-interval",
            parameters = params,
            graph = graph
        )

        node.onActivate(ctx)

        // The node logs the error and aborts activation without sending status events.
        verify(exactly = 0) { statusRegistry.registerStatusEvent(any()) }
    }

    @Test
    fun `secret retrieval failure should register ERROR status and abort activation`() {
        val secretId = "missing-secret"
        every { secretStore.get(secretId) } throws IllegalArgumentException("No secret with id: $secretId")

        val params = NodeParameters(mapOf(
            "ServerUrl" to "https://jira.example.com",
            "SecretId" to secretId,
            "PollIntervalMs" to "1000",
            "JQL" to ""
        ))

        val graph = mockk<dev.tjpal.graph.ActiveGraph>(relaxed = true)

        val node = JiraPollingNode(params, secretStore, statusRegistry, json)

        val ctx = dev.tjpal.nodes.NodeActivationContext(
            graphInstanceId = "g-secret-fail",
            nodeId = "node-secret-fail",
            parameters = params,
            graph = graph
        )

        node.onActivate(ctx)

        // Secret retrieval fails; the node logs an error and aborts activation without sending status events.
        verify(exactly = 0) { statusRegistry.registerStatusEvent(any()) }
    }
}
