package dev.tjpal.ai.openai

import com.openai.client.OpenAIClient
import com.openai.models.conversations.Conversation
import com.openai.models.responses.ResponseCreateParams
import dev.tjpal.ai.Request
import dev.tjpal.ai.RequestResponseChain
import dev.tjpal.ai.Response
import dev.tjpal.logging.logger

class OpenAIRequestResponseChain(private val client: OpenAIClient): RequestResponseChain() {
    private val logger = logger<OpenAIRequestResponseChain>()
    private var conversation: Conversation? = null
    private var responseIDs = mutableListOf<String>()

    fun create() {
        conversation = client.conversations().create()
    }

    override fun createResponse(request: Request): Response {
        val conversation = conversation ?: throw IllegalStateException("Conversation not initialized")
        val conversationID = conversation.id()

        val builder = ResponseCreateParams.builder()
            .input(request.input)
            .instructions(request.instructions)
            .model(request.model)
            .temperature(request.temperature)
            .conversation(conversationID)

        request.responseType?.let { builder.text(it.java) }
        request.topP?.let { builder.topP(it) }

        val apiResponse = client.responses().create(builder.build())
        val response = Response(message = extractMessage(apiResponse))

        responseIDs.add(apiResponse.id())

        return response
    }

    override fun delete() {
        val conversation = conversation ?: throw IllegalStateException("Conversation not initialized")
        val conversationID = conversation.id()

        responseIDs.forEach { responseID ->
            logger.info("OpenAIRequestResponseChain: Deleting response {}", responseID)
            client.responses().delete(responseID)
        }

        logger.info("OpenAIRequestResponseChain: Deleting conversation {}", conversationID)
        client.conversations().delete(conversationID)

        this.conversation = null
        responseIDs.clear()
    }

    private fun extractMessage(response: com.openai.models.responses.Response): String {
        if(response.output().isEmpty())
            return ""

        return response.output().map {
            when {
                it.message().isPresent -> {
                    it.message().get().content().map { content ->
                        if(content.outputText().isEmpty)
                            ""
                        else
                            return content.outputText().get().text()
                    }
                }
                else -> ""
            }
        }.joinToString("\n")
    }
}
