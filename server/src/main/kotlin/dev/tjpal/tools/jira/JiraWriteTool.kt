package dev.tjpal.tools.jira

import com.atlassian.jira.rest.client.api.JiraRestClient
import com.atlassian.jira.rest.client.api.domain.BasicPriority
import com.atlassian.jira.rest.client.api.domain.Comment
import com.atlassian.jira.rest.client.api.domain.Issue
import com.atlassian.jira.rest.client.api.domain.input.IssueInputBuilder
import com.fasterxml.jackson.annotation.JsonClassDescription
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonPropertyDescription
import dev.tjpal.ai.tools.Tool
import dev.tjpal.logging.logger
import dev.tjpal.nodes.jira.JiraRestClientFactory
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import java.security.InvalidParameterException

@Serializable
private data class ResultDto(val success: Boolean, val operation: String, val error: String? = null)

/**
 * Tool used to perform write operations on a Jira issue. This tool intentionally uses a single
 * `payload` string to carry operation-specific parameters so it is compatible with LLM tool
 * invocation APIs that do not support optional fields.
 */
@JsonClassDescription(
    """
    Tool for performing write operations on a Jira issue. Pass operations and their payloads as key-value pairs via 'operations' map.
    
    Supported operations:
    * operation: "add_comment", payload: "Comment body as a string"
    * operation: "set_priority", payload: "Highest", "High", "Medium", "Low", "Lowest"
    * operation: "set_summary", payload: "New summary text"
    * operation: "set_description", payload: "New description text"
    """)
@Serializable
class JiraWriteTool(
    @JsonPropertyDescription("The Jira issue key to operate on, e.g. 'PROJ-123'")
    val issueKey: String,

    @JsonPropertyDescription("The operation that shall be performed on the issue")
    val operation: String,

    @JsonPropertyDescription("The payload for the operation. The content and format depends on the operation type.")
    val payload: String,

    @JsonPropertyDescription("Set it to false if the user requested a dry run (no actual write). Default: false")
    val dryRun: Boolean = false
) : Tool {
    @JsonIgnore
    @Transient
    var serverUrl: String? = null

    @JsonIgnore
    @Transient
    var secretId: String? = null

    @JsonIgnore
    @Transient
    var clientFactory: JiraRestClientFactory? = null

    @JsonIgnore
    private val logger = logger<JiraWriteTool>()

    @JsonIgnore
    @Transient
    private val json = Json { ignoreUnknownKeys = true }

    override fun execute(): String {
        try {
            validateParameters()
        } catch(e: Exception) {
            logger.error("JiraWriteTool.execute parameter validation failed: {}", e.message)
            return serializeError("Parameter validation failed: ${e.message}", "parameter_validation")
        }

        if(operation.startsWith("set_")) {
            val fieldName = operation.removePrefix("set_")
            return handleUpdateFields(fieldName, payload)
        } else {
            return when(operation) {
                "add_comment" -> return handleAddComment(payload)
                else -> return serializeError("Unsupported operation '$operation'", operation)
            }
        }
    }

    private fun validateParameters() {
        if (issueKey.isBlank())  {
            throw InvalidParameterException("issueKey is required and must be non-empty")
        }

        if (!isValidOperation(operation)) {
            throw InvalidParameterException("operation '$operation' is not supported")
        }

        if(payload.isEmpty()) {
            throw InvalidParameterException("payload is required and must be non-empty")
        }
    }

    private fun isValidOperation(operation: String): Boolean {
        return when {
            operation == "add_comment" -> true
            operation.startsWith("set_") -> true
            operation == "assign" -> true
            operation == "add_labels" -> true
            else -> false
        }
    }

    private fun handleUpdateFields(fieldName: String, payload: String): String {
        if (dryRun) {
            return json.encodeToString(ResultDto(success = true, operation = "set_$fieldName"))
        }

        var client: JiraRestClient? = null
        try {
            val inputBuilder = IssueInputBuilder()

            when(fieldName) {
                "summary" -> inputBuilder.setSummary(payload)
                "description" -> inputBuilder.setDescription(payload)
                "priority" -> inputBuilder.setPriority(convertPriority(payload))
                else -> {
                    logger.error("JiraWriteTool.handleUpdateFields: Unsupported field name '{}'", fieldName)
                    return serializeError("Unsupported field name '$fieldName' for update", "set_$fieldName")
                }
            }

            logger.info("Updating issue {} field {} to '{}'", issueKey, fieldName, shorten(payload))
            client = createJiraClient()
            client.issueClient.updateIssue(issueKey, inputBuilder.build()).claim()
            logger.info("Successfully updated issue {}", issueKey)

            return json.encodeToString(ResultDto(success = true, operation = "set_$fieldName"))
        } catch (e: Exception) {
            logger.error("set_$fieldName failed: {}", e.message)
            return serializeError("Failed to set field (validation or write): ${e.message}", "set_$fieldName")
        } finally {
            try { client?.close() } catch (_: Exception) {}
        }
    }

    private fun convertPriority(priorityName: String): BasicPriority {
        return when(priorityName.lowercase()) {
            "highest" -> BasicPriority(null, 1L, "Highest")
            "high" -> BasicPriority(null, 2L, "High")
            "medium", "medium-high", "medium low" -> BasicPriority(null, 3L, "Medium")
            "low" -> BasicPriority(null, 4L, "Low")
            "lowest" -> BasicPriority(null, 5L, "Lowest")
            else -> throw IllegalArgumentException("Unknown priority name: $priorityName")
        }
    }

    private fun handleAddComment(commentBody: String): String {
        logger.info("JiraWriteTool.handleAddComment called for issueKey={}", issueKey)

        if (dryRun) {
            return json.encodeToString(ResultDto(success = true, operation = "add_comment"))
        }

        var client: JiraRestClient? = null
        try {
            client = createJiraClient()
            val issueClient = client.issueClient
            val issue: Issue = issueClient.getIssue(issueKey).claim()

            logger.info("Adding comment: '{}' to issue {}", shorten(commentBody), issueKey)

            val comment = Comment.valueOf(commentBody)
            issueClient.addComment(issue.commentsUri, comment).claim()

            return json.encodeToString(ResultDto(success = true, operation = "add_comment"))
        } catch (e: Exception) {
            logger.error("add_comment failed: {}", e.message)
            return serializeError("Failed to add comment (validation or write): ${e.message}", "add_comment")
        } finally {
            try { client?.close() } catch (_: Exception) {}
        }
    }

    private fun createJiraClient(): JiraRestClient {
        val factory = clientFactory ?: throw IllegalStateException("JiraRestClientFactory not configured on tool instance")
        val server = serverUrl ?: throw IllegalStateException("serverUrl not configured on tool instance")
        val secret = secretId ?: throw IllegalStateException("secretId not configured on tool instance")

        val client = factory.create(server, secret)

        return client
    }

    private fun serializeError(message: String, operation: String): String {
        val dto = ResultDto(success = false, operation = operation, error = message)
        return json.encodeToString(ResultDto.serializer(), dto)
    }

    private fun shorten(s: String, max: Int = 120): String = if (s.length <= max) s else s.substring(0, max) + "..."
}
