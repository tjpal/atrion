package dev.tjpal.nodes

import dev.tjpal.ai.LLM
import dev.tjpal.ai.Request

class LLMProcessingNode(private val parametersJson: String, private val llm: LLM) : Node {
    override fun onActivate(context: NodeActivationContext) {
        // No action
    }

    override suspend fun onEvent(context: NodeInvocationContext, output: NodeOutput) {
        try {
            val chain = llm.createResponseRequestChain()

            val request = Request(
                input = context.payload,
                instructions = ""
            )

            val response = chain.createResponse(request)
            val responsePayload = response.message

            chain.delete()

            output.send("text_out", responsePayload)
        } catch (e: Exception) {
            println("LLMProcessingNode: error during LLM processing: ${e.message}")
        }
    }

    override fun onStop(context: NodeDeactivationContext) {
        // No action
    }
}
