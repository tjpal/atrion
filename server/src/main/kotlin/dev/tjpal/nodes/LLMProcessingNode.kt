package dev.tjpal.nodes

import dev.tjpal.ai.LLM
import dev.tjpal.ai.Request
import dev.tjpal.ai.tools.Tool
import dev.tjpal.ai.tools.ToolRegistry
import dev.tjpal.graph.status.StatusRegistry
import dev.tjpal.logging.logger
import dev.tjpal.model.NodeParameters
import dev.tjpal.model.NodeStatus
import dev.tjpal.model.StatusEntry
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlin.reflect.KClass

class LLMProcessingNode(
    private val parameters: NodeParameters,
    private val llm: LLM,
    private val statusRegistry: StatusRegistry,
    private val toolRegistry: ToolRegistry,
    private val json: Json
) : Node {
    private val logger = logger<LLMProcessingNode>()

    override fun onActivate(context: NodeActivationContext) {
        logger.debug("LLMProcessingNode.onActivate graphInstanceId={} nodeId={}", context.graphInstanceId, context.nodeId)
    }

    override suspend fun onEvent(context: NodeInvocationContext, output: NodeOutput) {
        try {
            sendStatusEntry(NodeStatus.RUNNING, context.payload, null, null, context)
            logger.info("LLMProcessingNode starting LLM chain for graphInstanceId={} nodeId={} executionId={}", context.graphInstanceId, context.nodeId, context.executionId)
            val chain = llm.createResponseRequestChain()

            val promptTemplate = parameters.values["Prompt"] ?: "{{input}}"
            val resolvedPrompt = promptTemplate.replace("{{input}}", context.payload)

            // Gather attached tools and their node-level parameters (serialized into JSON elements)
            val attached = context.graph.getAttachedToolDefinitionsWithParameters(context.nodeId)
            val toolDefinitionNames: List<String> = attached.map { it.first }

            // Build list of KClass for tools by asking the registry/service for the class to pass to the OpenAI builder
            val toolKClasses: List<KClass<out Tool>> = toolDefinitionNames.mapNotNull { defName ->
                val resolved = toolRegistry.getToolClass(defName)
                if (resolved == null) {
                    logger.warn("Unresolved tool referenced by node {}: {}", context.nodeId, defName)
                }
                resolved
            }

            // Build toolsMetadata map (tool definition name -> nodeParameters JSON)
            val toolsMetadata: Map<String, JsonElement> = attached.mapNotNull { (defName, nodeParams) ->
                val mutableNodeParams = nodeParams.values.toMutableMap()

                mutableNodeParams["graphInstanceId"] = context.graphInstanceId
                mutableNodeParams["nodeId"] = context.nodeId

                val elem = json.encodeToJsonElement(mutableNodeParams)
                Pair(defName, elem)
            }.toMap()

            val request = Request(
                input = context.payload,
                instructions = resolvedPrompt,
                tools = toolKClasses,
                toolStaticParameters = if (toolsMetadata.isEmpty()) null else toolsMetadata
            )

            logger.debug("LLM request  {}", context.payload)

            val response = chain.createResponse(request)
            val responsePayload = response.message

            chain.delete()

            sendStatusEntry(NodeStatus.FINISHED, null, responsePayload, null, context)
            output.send("text_out", responsePayload)
            logger.info("LLMProcessingNode finished LLM call for executionId={} nodeId={}", context.executionId, context.nodeId)
        } catch (e: Exception) {
            logger.error("LLMProcessingNode: error during LLM processing for executionId={} nodeId={}: {}", context.executionId, context.nodeId, e.message)

            // For now just pass the exception message as error output. Later we turn this into a more user-friendly message.
            sendStatusEntry(NodeStatus.ERROR, null, null, e.message, context)
        }
    }

    private fun sendStatusEntry(
        status: NodeStatus,
        inputPayload: String?,
        outputPayload: String?,
        message: String?,
        context: NodeInvocationContext
    ) {
        val entry = StatusEntry(
            graphInstanceId = context.graphInstanceId,
            executionId = context.executionId,
            nodeId = context.nodeId,
            timestamp = System.currentTimeMillis(),
            nodeStatus = status,
            inputPayload = inputPayload,
            outputPayload = outputPayload,
            message = message
        )

        statusRegistry.registerStatusEvent(entry)
    }

    override fun onStop(context: NodeDeactivationContext) {
        logger.debug("LLMProcessingNode.onStop graphInstanceId={} nodeId={}", context.graphInstanceId, context.nodeId)
    }
}
