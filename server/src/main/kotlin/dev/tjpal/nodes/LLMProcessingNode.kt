package dev.tjpal.nodes

import dev.tjpal.ai.LLM
import dev.tjpal.ai.Request
import dev.tjpal.graph.status.StatusRegistry
import dev.tjpal.logging.logger
import dev.tjpal.model.NodeStatus
import dev.tjpal.model.StatusEntry

class LLMProcessingNode(
    private val parametersJson: String,
    private val llm: LLM,
    private val statusRegistry: StatusRegistry
) : Node {
    private val logger = logger<LLMProcessingNode>()

    override fun onActivate(context: NodeActivationContext) {
        // No action
    }

    override suspend fun onEvent(context: NodeInvocationContext, output: NodeOutput) {
        try {
            sendStatusEntry(NodeStatus.RUNNING, context.payload, null, null, context)
            val chain = llm.createResponseRequestChain()

            val request = Request(
                input = context.payload,
                instructions = ""
            )

            val response = chain.createResponse(request)
            val responsePayload = response.message

            chain.delete()

            sendStatusEntry(NodeStatus.FINISHED, null, responsePayload,null, context)
            output.send("text_out", responsePayload)
        } catch (e: Exception) {
            logger.error("LLMProcessingNode: error during LLM processing", e)

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
    }
}
