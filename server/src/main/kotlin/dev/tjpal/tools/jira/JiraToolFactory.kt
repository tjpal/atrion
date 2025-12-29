package dev.tjpal.tools.jira

import dev.tjpal.ai.tools.Tool
import dev.tjpal.ai.tools.ToolFactory
import dev.tjpal.logging.logger
import dev.tjpal.nodes.jira.JiraRestClientFactory
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.reflect.KClass

class JiraToolFactory(
    private val json: Json,
    private val jiraRestClientFactory: JiraRestClientFactory
) : ToolFactory {
    private val logger = logger<JiraToolFactory>()

    override val toolClass: KClass<out Tool> = JiraTool::class

    override fun create(arguments: JsonElement?, nodeParameters: JsonElement?): Tool {
        val nodeParameters = nodeParameters?.jsonObject ?: throw IllegalArgumentException("nodeParameters required")

        val serverUrl = nodeParameters["ServerUrl"]?.jsonPrimitive?.content ?: throw IllegalArgumentException("ServerUrl parameter required")
        val secretId = nodeParameters["SecretId"]?.jsonPrimitive?.content ?: throw IllegalArgumentException("SecretId parameter required")

        val toolInstance: JiraTool = if (arguments != null) {
            try {
                json.decodeFromJsonElement(JiraTool.serializer(), arguments)
            } catch (e: Exception) {
                logger.error("Failed to decode JiraTool arguments: {}", e.message)
                throw IllegalArgumentException("Invalid arguments for JiraTool: ${e.message}")
            }
        } else {
            throw IllegalArgumentException("arguments required")
        }

        toolInstance.serverUrl = serverUrl
        toolInstance.secretId = secretId
        toolInstance.clientFactory = jiraRestClientFactory

        return toolInstance
    }
}
