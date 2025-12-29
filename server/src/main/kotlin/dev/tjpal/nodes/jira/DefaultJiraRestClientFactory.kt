package dev.tjpal.nodes.jira

import com.atlassian.jira.rest.client.api.JiraRestClient
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory
import dev.tjpal.logging.logger
import dev.tjpal.secrets.SecretStore
import kotlinx.serialization.json.Json
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default implementation that uses the Atlassian AsynchronousJiraRestClientFactory to create a JiraRestClient.
 * It also loads credentials from the provided SecretStore using the secretId and parses them into username/password.
 *
 * Important: Callers are responsible for closing the returned JiraRestClient.
 */
@Singleton
class DefaultJiraRestClientFactory @Inject constructor(
    private val secretStore: SecretStore,
    private val json: Json
) : JiraRestClientFactory {
    private val logger = logger<DefaultJiraRestClientFactory>()

    override fun create(serverUrl: String, secretId: String): JiraRestClient {
        val credentials = loadCredentials(secretId)

        val uri = try {
            URI(serverUrl)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid Jira serverUrl: ${e.message}")
        }

        return try {
            AsynchronousJiraRestClientFactory().createWithBasicHttpAuthentication(uri, credentials.first, credentials.second)
        } catch (e: Exception) {
            logger.error("Failed to initialize Jira client for serverUrl={}: {}", serverUrl, e.message)
            throw e
        }
    }

    private fun loadCredentials(secretId: String): Pair<String, String> {
        val plaintextSecret = try {
            secretStore.get(secretId)
        } catch (e: Exception) {
            logger.error("DefaultJiraRestClientFactory: failed to read secret id={}: {}", secretId, e.message)
            throw IllegalStateException("Failed to retrieve secret for id: $secretId")
        }

        try {
            val credentials = json.decodeFromString(JiraCredentials.serializer(), plaintextSecret)

            val username = credentials.username ?: throw IllegalStateException("Credentials missing username for secret: $secretId")
            val password = credentials.password ?: throw IllegalStateException("Credentials missing password for secret: $secretId")

            return Pair(username, password)
        } catch (e: Exception) {
            logger.error("DefaultJiraRestClientFactory: failed to parse credentials for secret id={}: {}", secretId, e.message)
            throw IllegalStateException("Failed to parse credentials for secret: $secretId")
        }
    }
}
