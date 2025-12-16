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
        logger.debug("OpenAI conversation created id={}", conversation?.id())
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

        logger.debug("Creating OpenAI response for conversation={} with inputPreview={}", conversationID, request.input)
        val apiResponse = client.responses().create(builder.build())

        val response = Response(message = extractMessage(apiResponse))
        logger.debug("Response: {}", response.message)

        responseIDs.add(apiResponse.id())
        logger.debug("OpenAI response created id={} conversation={}", apiResponse.id(), conversationID)

        return response
    }

    override fun delete() {
        val conversation = conversation ?: throw IllegalStateException("Conversation not initialized")
        val conversationID = conversation.id()

        logger.debug("OpenAIRequestResponseChain: Deleting responses for conversation {}", conversationID)

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
