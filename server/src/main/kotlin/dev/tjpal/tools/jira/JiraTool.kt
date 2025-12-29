package dev.tjpal.tools.jira

import com.atlassian.jira.rest.client.api.IssueRestClient
import com.atlassian.jira.rest.client.api.JiraRestClient
import com.atlassian.jira.rest.client.api.domain.Issue
import com.fasterxml.jackson.annotation.JsonClassDescription
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonPropertyDescription
import dev.tjpal.ai.tools.Tool
import dev.tjpal.logging.logger
import dev.tjpal.nodes.jira.JiraRestClientFactory
import dev.tjpal.tools.jira.dto.JiraComment
import dev.tjpal.tools.jira.dto.JiraHistoryChangeItem
import dev.tjpal.tools.jira.dto.JiraHistoryItem
import dev.tjpal.tools.jira.dto.JiraIssueDto
import dev.tjpal.tools.jira.dto.JiraMainAttributes
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json

/**
 * The JiraTool class is both the serializable tool schema examined by the LLM and the runtime Tool implementation.
 */
@JsonClassDescription("Tool for fetching a single Jira issue by key. Use it to retrieve compact issue data for LLM processing. Configure which parts to include using boolean flags and choose the output format.")
@Serializable
class JiraTool(
    @JsonPropertyDescription("The single Jira issue key to retrieve, e.g. 'PROJ-123'. This field is required.")
    val issueKey: String,

    @JsonPropertyDescription("Whether to include the main compact attributes (key, summary, type, timestamps, project, reporter, assignee). Defaults to true.")
    val includeMainAttributes: Boolean = true,

    @JsonPropertyDescription("Whether to include comments. Defaults to false.")
    val includeComments: Boolean = false,

    @JsonPropertyDescription("Whether to include changelog/history entries. Defaults to false. Note: enabling history may increase payload size.")
    val includeHistory: Boolean = false,

    @JsonPropertyDescription("Output format. Accepted value: 'json'. (Only JSON is supported.)")
    val outputFormat: String = "json"
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

    private val logger = logger<JiraTool>()

    @JsonIgnore
    @Transient
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class ErrorDto(val error: String)

    override fun execute(): String {
        val serverUrl = serverUrl ?: throw IllegalStateException("ServerUrl not configured on tool instance")
        val secretId = secretId ?: throw IllegalStateException("SecretId not configured on tool instance")

        val factory = clientFactory ?: throw IllegalStateException("JiraRestClientFactory not configured on JiraTool instance")
        var client: JiraRestClient? = null

        try {
            client = factory.create(serverUrl, secretId)

            val issueClient: IssueRestClient = client.issueClient
            val expand = if (includeHistory) listOf(IssueRestClient.Expandos.CHANGELOG) else emptyList()

            val issue = issueClient.getIssue(issueKey, expand).claim()

            val main = if (includeMainAttributes) mapToMainAttributes(issue) else null
            val comments = if (includeComments) mapToComments(issue) else null
            val history = if (includeHistory) mapToHistory(issue) else null

            if (includeMainAttributes && main == null) {
                return serializeError("Requested main attributes but they could not be extracted")
            }

            val dto = JiraIssueDto(main = main ?: JiraMainAttributes(key = issueKey), comments = comments, history = history)

            return Json { ignoreUnknownKeys = true }.encodeToString(dto)
        } catch (e: Exception) {
            logger.error("JiraTool: error fetching or serializing issue {}: {}", issueKey, e.message)
            return serializeError("Failed to fetch issue: ${e.message}")
        } finally {
            try { client?.close() } catch (_: Exception) {}
        }
    }

    private fun serializeError(message: String): String = json.encodeToString(ErrorDto(message))

    private fun mapToMainAttributes(issue: Issue): JiraMainAttributes {
        return JiraMainAttributes(
            key = issue.key,
            summary = issue?.summary ?: "<no summary available>",
            description = issue?.description ?: "<no description available>",
            issueType = issue?.issueType?.name ?: "<no issue type>",
            created = issue?.creationDate?.toString() ?: "<no creation date>",
            updated = issue?.updateDate?.toString() ?: "<no update date>",
            projectKey = issue?.project?.key ?: "<no project key>",
            reporter = issue?.reporter?.name ?: "<no reporter>",
            assignee = issue?.assignee?.name ?: "Unassigned"
        )
    }

    private fun mapToComments(issue: Issue): List<JiraComment> {
        return issue.comments.map { comment ->
            val author = comment.author?.displayName ?: comment.author?.name ?: "<no name available>"
            val created = comment?.creationDate?.toString() ?: "<no creation date>"
            val body = comment?.body ?: "<comment body is empty>"

            JiraComment(author = author, created = created, body = body)
        }
    }

    private fun mapToHistory(issue: Issue): List<JiraHistoryItem> {
        return issue.changelog.map { logEntry ->
            JiraHistoryItem(
                logEntry.author?.displayName ?: logEntry.author?.name ?: "<no name available>",
                created = logEntry?.created.toString(),
                items = logEntry?.items?.map { changeLogItem ->
                    JiraHistoryChangeItem(
                        field = changeLogItem?.field ?: "<no field specified>",
                        from = changeLogItem?.from ?: "<no from value specified>",
                        to = changeLogItem?.to ?: "<no to value specified>"
                    )
                } ?: emptyList()
            )
        }
    }
}
