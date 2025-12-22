package dev.tjpal.nodes

import dev.tjpal.ai.LLM
import dev.tjpal.ai.Request
import dev.tjpal.graph.status.StatusRegistry
import dev.tjpal.logging.logger
import dev.tjpal.model.NodeParameters
import dev.tjpal.model.NodeStatus
import dev.tjpal.model.StatusEntry

class LLMProcessingNode(
    private val parameters: NodeParameters,
    private val llm: LLM,
    private val statusRegistry: StatusRegistry
) : Node {
    private val logger = logger<LLMProcessingNode>()

    override fun onActivate(context: NodeActivationContext) {
        // No action
        logger.debug("LLMProcessingNode.onActivate graphInstanceId={} nodeId={}", context.graphInstanceId, context.nodeId)
    }

    override suspend fun onEvent(context: NodeInvocationContext, output: NodeOutput) {
        try {
            sendStatusEntry(NodeStatus.RUNNING, context.payload, null, null, context)
            logger.info("LLMProcessingNode starting LLM chain for graphInstanceId={} nodeId={} executionId={}", context.graphInstanceId, context.nodeId, context.executionId)
            val chain = llm.createResponseRequestChain()

            val promptTemplate = parameters.values["Prompt"] ?: "{{input}}"
            val resolvedPrompt = promptTemplate.replace("{{input}}", context.payload)

            val request = Request(
                input = context.payload,
                instructions = resolvedPrompt
            )

            logger.debug("LLM request  {}", context.payload)

            val response = chain.createResponse(request)
            val responsePayload = response.message

            chain.delete()

            sendStatusEntry(NodeStatus.FINISHED, null, responsePayload,null, context)
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
        // No action
        logger.debug("LLMProcessingNode.onStop graphInstanceId={} nodeId={}", context.graphInstanceId, context.nodeId)
    }
}
