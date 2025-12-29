package dev.tjpal.nodes.jira

import com.atlassian.jira.rest.client.api.IssueRestClient
import com.atlassian.jira.rest.client.api.JiraRestClient
import com.atlassian.jira.rest.client.api.domain.Issue
import com.atlassian.jira.rest.client.api.domain.SearchResult
import dev.tjpal.graph.status.StatusRegistry
import dev.tjpal.logging.logger
import dev.tjpal.model.NodeParameters
import dev.tjpal.model.NodeStatus
import dev.tjpal.model.StatusEntry
import dev.tjpal.nodes.Node
import dev.tjpal.nodes.NodeActivationContext
import dev.tjpal.nodes.NodeDeactivationContext
import dev.tjpal.nodes.NodeInvocationContext
import dev.tjpal.nodes.NodeOutput
import dev.tjpal.secrets.SecretStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

/**
 * JiraPollingNode implements a polling input node that queries Jira periodically and emits newly created issue keys.
 */
class JiraPollingNode(
    private val parameters: NodeParameters,
    private val secretStore: SecretStore,
    private val statusRegistry: StatusRegistry,
    private val json: Json,
    private val jiraRestClientFactory: JiraRestClientFactory
) : Node {
    private val logger = logger<JiraPollingNode>()

    private val seenIssueKeys: MutableSet<String> = Collections.newSetFromMap(ConcurrentHashMap())

    // Polling job
    private var scopeJob: Job? = null
    private var scope: CoroutineScope? = null

    private var jiraClient: JiraRestClient? = null

    private lateinit var serverUrl: String
    private lateinit var secretId: String
    private var pollIntervalMs: Long = 0
    private var jql: String = ""

    override fun onActivate(context: NodeActivationContext) {
        logger.info("JiraPollingNode.onActivate graphInstanceId={} nodeId={}", context.graphInstanceId, context.nodeId)

        if (!parseParameters(context))  {
            logger.error("JiraPollingNode activation failed due to invalid parameters for node {}", context.nodeId)
            return
        }

        if (!initializeClient(context)) {
            return
        }

        if(!initialSeed(context)) {
            logger.error("JiraPollingNode activation failed during initial seed for node {}", context.nodeId)
            // Rely on the data being polled. An error here should be the absolute exception.
            return
        }

        startPollingLoop(context)
    }

    private fun parseParameters(context: NodeActivationContext): Boolean {
        serverUrl = parameters.values["ServerUrl"] ?: ""
        secretId = parameters.values["SecretId"] ?: ""

        val pollIntervalMs = parameters.values["PollIntervalMs"] ?: ""
        jql = parameters.values["JQL"] ?: ""

        if (serverUrl.isBlank()) {
            logger.error("JiraPollingNode activation failed: missing ServerUrl parameter for node {}", context.nodeId)
            return false
        }

        if (secretId.isBlank()) {
            logger.error("JiraPollingNode activation failed: missing SecretId parameter for node {}", context.nodeId)
            return false
        }

        this@JiraPollingNode.pollIntervalMs = try {
            pollIntervalMs.toLong()
        } catch (e: Exception) {
            logger.error("JiraPollingNode activation failed: invalid PollIntervalMs for node {}: {}", context.nodeId, pollIntervalMs)
            return false
        }

        return true
    }

    private fun initializeClient(context: NodeActivationContext): Boolean {
        return try {
            jiraClient = jiraRestClientFactory.create(serverUrl, secretId)
            true
        } catch (e: Exception) {
            logger.error("Failed to initialize Jira client for node {}: {}", context.nodeId, e.message)
            false
        }
    }

    private fun initialSeed(context: NodeActivationContext): Boolean {
        return try {
            val issues = fetchIssues(jql)
            logger.info("Initial seed is ${issues.size} issues. Keys: ${issues.mapNotNull { safeIssueKey(it) }}")

            for (issue in issues) {
                val key = safeIssueKey(issue)
                if (key != null) {
                    seenIssueKeys.add(key)
                }
            }

            true
        } catch (e: Exception) {
            logger.error("Initial Jira poll failed for node {}: {}", context.nodeId, e.message)
            false
        }
    }

    private fun startPollingLoop(context: NodeActivationContext) {
        scopeJob = Job()
        scope = CoroutineScope(Dispatchers.Default + scopeJob!!)

        scope?.launch {
            while (isActive) {
                try {
                    delay(pollIntervalMs)
                    logger.info("Starting new cycle")

                    val issues = pollOnce(context) ?: continue
                    logger.info("Polled ${issues.size} issues. Keys: ${issues.mapNotNull { safeIssueKey(it) }}")

                    processIssues(issues, context)
                } catch (e: Exception) {
                    logger.error("Unexpected error in polling loop for node {}: {}", context.nodeId, e.message)
                    sendStatus(NodeStatus.ERROR, null, null, "Unexpected polling loop error: ${e.message}", context)
                }
            }
        }
    }

    private fun pollOnce(context: NodeActivationContext): List<Issue>? {
        return try {
            fetchIssues(jql)
        } catch (e: Exception) {
            logger.error("Jira poll failed for node {}: {}", context.nodeId, e.message)
            sendStatus(NodeStatus.ERROR, null, null, "Poll failed: ${e.message}", context)
            null
        }
    }

    private fun processIssues(issues: List<Issue>, context: NodeActivationContext) {
        val newIssueKeys = mutableListOf<String>()

        for (issue in issues) {
            val key = safeIssueKey(issue) ?: continue

            if (!seenIssueKeys.contains(key)) {
                newIssueKeys.add(key)
            }
        }

        if (newIssueKeys.isNotEmpty()) {
            logger.info("JiraPollingNode discovered {} new issues for node {}", newIssueKeys.size, context.nodeId)
            emitIssuesForKeys(newIssueKeys, context)
        } else {
            sendStatus(NodeStatus.RUNNING, null, null, "Poll completed, no new issues", context)
        }
    }

    private fun emitIssuesForKeys(keys: List<String>, context: NodeActivationContext) {
        for (key in keys) {
            try {
                val output = object : NodeOutput {
                    override fun send(outputConnectorId: String, payload: String) {
                        context.graph.routeFromNode(context.nodeId, outputConnectorId, payload, java.util.UUID.randomUUID().toString())
                    }
                }

                output.send("out", key)
                seenIssueKeys.add(key)

                sendStatus(NodeStatus.RUNNING, null, null, "Emitted new issue: $key", context)
            } catch (e: Exception) {
                logger.error("Failed to emit new issue {} for node {}: {}", key, context.nodeId, e.message)
                sendStatus(NodeStatus.ERROR, null, null, "Failed to emit issue: ${e.message}", context)
            }
        }
    }

    private fun fetchIssues(jql: String): List<Issue> {
        val client = jiraClient ?: throw IllegalStateException("Jira client not initialized")

        val searchClient = client.searchClient
        val issueClient: IssueRestClient = client.issueClient

        val fieldSet = mutableSetOf(
            "summary",
            "issuetype",
            "created",
            "updated",
            "project",
            "status"
        )

        val searchResult: SearchResult = searchClient.searchJql(jql, 50, 0, fieldSet).claim()
        return searchResult.issues.toList()
    }

    override suspend fun onEvent(context: NodeInvocationContext, output: NodeOutput) {
        // Input node: no-op for incoming events
    }

    override fun onStop(context: NodeDeactivationContext) {
        logger.info("JiraPollingNode.onStop graphInstanceId={} nodeId={}", context.graphInstanceId, context.nodeId)

        try {
            scopeJob?.cancel()
        } catch (_: Exception) {
            logger.error("Error cancelling polling job for node {}", context.nodeId)
        }

        try {
            jiraClient?.close()
        } catch (_: Exception) {
            logger.error("Error closing Jira client for node {}", context.nodeId)
        }
    }

    private fun sendStatus(
        status: NodeStatus,
        inputPayload: String?,
        outputPayload: String?,
        message: String?,
        context: NodeActivationContext
    ) {
        val entry = StatusEntry(
            graphInstanceId = context.graphInstanceId,
            executionId = "",
            nodeId = context.nodeId,
            timestamp = System.currentTimeMillis(),
            nodeStatus = status,
            inputPayload = inputPayload,
            outputPayload = outputPayload,
            message = message
        )

        statusRegistry.registerStatusEvent(entry)
    }

    private fun safeIssueKey(issue: Issue): String? {
        return try {
            val key = try { issue.key } catch (_: Throwable) { null }
            if (!key.isNullOrBlank()) {
                return key
            }

            val id = try { issue.id } catch (_: Throwable) { null }
            if (id != null) {
                return id.toString()
            }

            null
        } catch (_: Exception) {
            null
        }
    }
}
