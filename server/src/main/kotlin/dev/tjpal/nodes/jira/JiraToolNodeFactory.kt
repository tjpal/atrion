package dev.tjpal.nodes.jira

import dev.tjpal.ai.tools.ToolRegistry
import dev.tjpal.graph.status.StatusRegistry
import dev.tjpal.model.ConnectorDefinition
import dev.tjpal.model.ConnectorSchema
import dev.tjpal.model.NodeDefinition
import dev.tjpal.model.NodeParameters
import dev.tjpal.model.NodeType
import dev.tjpal.model.ParameterDefinition
import dev.tjpal.model.ParameterType
import dev.tjpal.nodes.Node
import dev.tjpal.nodes.NodeActivationContext
import dev.tjpal.nodes.NodeDeactivationContext
import dev.tjpal.nodes.NodeFactory
import dev.tjpal.nodes.NodeInvocationContext
import dev.tjpal.nodes.NodeOutput
import dev.tjpal.secrets.SecretStore
import dev.tjpal.tools.jira.JiraToolFactory
import dev.tjpal.utilities.ImageResourceEncoder
import kotlinx.serialization.json.Json
import javax.inject.Inject

class JiraToolNodeFactory @Inject constructor(
    private val toolRegistry: ToolRegistry,
    private val secretStore: SecretStore,
    private val statusRegistry: StatusRegistry,
    private val json: Json,
    private val jiraRestClientFactory: JiraRestClientFactory
) : NodeFactory {
    private val toolName = "JiraTool"

    override fun definition(): NodeDefinition {
        val resourceEncoder = ImageResourceEncoder()

        return NodeDefinition(
            id = "JiraTool",
            displayedName = "Jira",
            type = NodeType.TOOL,
            category = "Tool",
            description = "Tool that fetches a single Jira issue and returns compacted output (main attributes, comments, history)",
            icon = resourceEncoder.encodeResourceToBase64("jira-tool.png"),
            inputConnectors = emptyList(),
            outputConnectors = emptyList(),
            toolConnectors = listOf(ConnectorDefinition(id = "tool", label = "Tool", schema = ConnectorSchema.JSON)),
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
                )
            )
        )
    }

    override fun createNode(parameters: NodeParameters): Node {
        return object : Node {
            override fun onActivate(context: NodeActivationContext) {}
            override suspend fun onEvent(context: NodeInvocationContext, output: NodeOutput) {}
            override fun onStop(context: NodeDeactivationContext) {}
        }
    }

    init {
        toolRegistry.registerFactory(toolName, JiraToolFactory(json, jiraRestClientFactory))
    }
}
