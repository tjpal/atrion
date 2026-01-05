package dev.tjpal.tools.jira

import com.atlassian.jira.rest.client.api.IssueRestClient
import com.atlassian.jira.rest.client.api.JiraRestClient
import com.atlassian.jira.rest.client.api.domain.Issue
import dev.tjpal.nodes.jira.JiraRestClientFactory
import io.atlassian.util.concurrent.Promise
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JiraWriteToolTest {
    private val json = Json { ignoreUnknownKeys = true }

    @AfterTest
    fun cleanup() {
        clearAllMocks()
    }

    private fun parseResult(jsonText: String): JsonObject {
        return json.parseToJsonElement(jsonText).jsonObject
    }

    @Test
    fun setSummary_callsJiraUpdate_issueClientUpdateCalled() {
        val mockIssueClient = mockk<IssueRestClient>(relaxed = true)
        val mockJiraClient = mockk<JiraRestClient>(relaxed = true)

        every { mockJiraClient.issueClient } returns mockIssueClient
        every { mockIssueClient.updateIssue(any(), any()) } returns mockk(relaxed = true)

        val mockFactory = mockk<JiraRestClientFactory>(relaxed = true)
        every { mockFactory.create(any(), any()) } returns mockJiraClient

        val tool = JiraWriteTool(issueKey = "PROJ-1", operation = "set_summary", payload = "Updated", dryRun = false)

        tool.serverUrl = "http://jira"
        tool.secretId = "secret"
        tool.clientFactory = mockFactory

        val resultJson = tool.execute()
        val obj = parseResult(resultJson)

        assertEquals("set_summary", obj["operation"]!!.toString().trim('"'))
        assertTrue(obj["success"]!!.toString().contains("true"))
        verify(exactly = 1) { mockIssueClient.updateIssue("PROJ-1", any()) }
    }

    @Test
    fun addComment_callsJiraAddComment_issueClientAddCommentCalled() {
        val mockIssueClient = mockk<IssueRestClient>(relaxed = true)
        val mockJiraClient = mockk<JiraRestClient>(relaxed = true)

        every { mockJiraClient.issueClient } returns mockIssueClient

        val mockIssuePromise = mockk<Promise<Issue>>(relaxed = true)
        val mockIssue = mockk<Issue>(relaxed = true)
        every { mockIssue.commentsUri } returns java.net.URI.create("http://example")
        every { mockIssuePromise.claim() } returns mockIssue
        every { mockIssueClient.getIssue(any()) } returns mockIssuePromise

        every { mockIssueClient.addComment(any(), any()) } returns mockk(relaxed = true)

        val mockFactory = mockk<JiraRestClientFactory>(relaxed = true)
        every { mockFactory.create(any(), any()) } returns mockJiraClient

        val tool = JiraWriteTool(issueKey = "PROJ-2", operation = "add_comment", payload = "a body", dryRun = false)
        tool.serverUrl = "http://jira"
        tool.secretId = "secret"
        tool.clientFactory = mockFactory

        val resultJson = tool.execute()
        val obj = parseResult(resultJson)

        assertEquals("add_comment", obj["operation"]!!.toString().trim('"'))
        assertTrue(obj["success"]!!.toString().contains("true"))

        verify(exactly = 1) { mockIssueClient.getIssue("PROJ-2") }
        verify(exactly = 1) { mockIssueClient.addComment(any(), any()) }
    }

    @Test
    fun invalidPriority_returnsSerializedError_and_noSuccess() {
        val tool = JiraWriteTool(issueKey = "PROJ-1", operation = "set_priority", payload = "unknown-prio", dryRun = false)

        val resultJson = tool.execute()
        val obj = parseResult(resultJson)

        assertTrue(!obj["success"]!!.toString().contains("true"))
        assertEquals("set_priority", obj["operation"]!!.toString().trim('"'))
        assertTrue(obj["error"]!!.toString().contains("Unknown priority"), "error should mention unknown priority")
    }
}
