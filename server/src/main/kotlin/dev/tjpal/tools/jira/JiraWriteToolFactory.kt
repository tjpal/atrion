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

class JiraWriteToolFactory(
    private val json: Json,
    private val jiraRestClientFactory: JiraRestClientFactory
) : ToolFactory {
    private val logger = logger<JiraWriteToolFactory>()

    override val toolClass: KClass<out Tool> = JiraWriteTool::class

    override fun create(arguments: JsonElement?, nodeParameters: JsonElement?): Tool {
        val nodeParameters = nodeParameters?.jsonObject ?: throw IllegalArgumentException("nodeParameters required")

        val serverUrl = nodeParameters["ServerUrl"]?.jsonPrimitive?.content ?: throw IllegalArgumentException("ServerUrl parameter required")
        val secretId = nodeParameters["SecretId"]?.jsonPrimitive?.content ?: throw IllegalArgumentException("SecretId parameter required")

        if (arguments == null) {
            throw IllegalArgumentException("arguments required")
        }

        val toolInstance: JiraWriteTool = try {
            json.decodeFromJsonElement(JiraWriteTool.serializer(), arguments)
        } catch (e: Exception) {
            logger.error("Failed to decode JiraWriteTool arguments: {}", e.message)
            throw IllegalArgumentException("Invalid arguments for JiraWriteTool: ${e.message}")
        }

        toolInstance.serverUrl = serverUrl
        toolInstance.secretId = secretId
        toolInstance.clientFactory = jiraRestClientFactory

        return toolInstance
    }
}
