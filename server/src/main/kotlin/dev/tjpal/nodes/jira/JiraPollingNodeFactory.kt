package dev.tjpal.nodes.jira

import dev.tjpal.graph.status.StatusRegistry
import dev.tjpal.model.ConnectorDefinition
import dev.tjpal.model.ConnectorSchema
import dev.tjpal.model.NodeDefinition
import dev.tjpal.model.NodeParameters
import dev.tjpal.model.NodeType
import dev.tjpal.model.ParameterDefinition
import dev.tjpal.model.ParameterType
import dev.tjpal.utilities.ImageResourceEncoder
import kotlinx.serialization.json.Json
import javax.inject.Inject

class JiraPollingNodeFactory @Inject constructor(
    private val secretStore: dev.tjpal.secrets.SecretStore,
    private val statusRegistry: StatusRegistry,
    private val json: Json,
    private val jiraRestClientFactory: JiraRestClientFactory
) : dev.tjpal.nodes.NodeFactory {
    override fun definition(): NodeDefinition {
        val encoder = ImageResourceEncoder()

        return NodeDefinition(
            name = "Jira Polling Input",
            type = NodeType.INPUT,
            category = "Input",
            description = "Polls a Jira server periodically and emits new issue keys as they are created.",
            icon = encoder.encodeResourceToBase64("placeholder-1.png"),
            inputConnectors = emptyList(),
            outputConnectors = listOf(ConnectorDefinition(id = "out", label = "Out", schema = ConnectorSchema.TEXT)),
            toolConnectors = emptyList(),
            debugConnectors = emptyList(),
            parameters = listOf(
                ParameterDefinition(
                    name = "ServerUrl",
                    type = ParameterType.STRING,
                    required = true,
                    description = "Jira server base URL (e.g. https://jira.example.com)"
                ),
                ParameterDefinition(
                    name = "SecretId",
                    type = ParameterType.STRING,
                    required = true,
                    description = "Secret id that contains a JSON object with username and password"
                ),
                ParameterDefinition(
                    name = "PollIntervalMs",
                    type = ParameterType.INT,
                    required = true,
                    description = "Polling interval in milliseconds"
                ),
                ParameterDefinition(
                    name = "JQL",
                    type = ParameterType.LONG_TEXT,
                    required = false,
                    description = "Optional JQL to filter the issues returned by the Jira server"
                )
            )
        )
    }

    override fun createNode(parameters: NodeParameters): dev.tjpal.nodes.Node {
        return JiraPollingNode(parameters, secretStore, statusRegistry, json, jiraRestClientFactory)
    }
}
