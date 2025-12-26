package dev.tjpal.ai.openai

import com.openai.client.OpenAIClient
import com.openai.models.conversations.Conversation
import com.openai.models.responses.ResponseCreateParams
import com.openai.models.responses.ResponseFunctionToolCall
import com.openai.models.responses.ResponseInputItem
import com.openai.models.responses.ResponseOutputItem
import dev.tjpal.ai.Request
import dev.tjpal.ai.RequestResponseChain
import dev.tjpal.ai.Response
import dev.tjpal.ai.tools.Tool
import dev.tjpal.ai.tools.ToolDeserializationUtil
import dev.tjpal.ai.tools.ToolRegistry
import dev.tjpal.logging.logger
import kotlinx.serialization.json.Json

class OpenAIRequestResponseChain(
    private val client: OpenAIClient,
    private val toolRegistry: ToolRegistry
) : RequestResponseChain() {
    private val logger = logger<OpenAIRequestResponseChain>()
    private var conversation: Conversation? = null
    private var responseIDs = mutableListOf<String>()

    private val json = Json { ignoreUnknownKeys = true }

    fun create() {
        conversation = client.conversations().create()
        logger.debug("OpenAI conversation created id={}", conversation?.id())
    }

    override fun createResponse(request: Request): Response {
        val conversation = conversation ?: throw IllegalStateException("Conversation not initialized")
        val conversationID = conversation.id()

        val initialUserMessage = initialUserMessage(request)

        var itemsToSend: List<ResponseInputItem> = listOf(initialUserMessage)
        var lastApiResponse: com.openai.models.responses.Response

        while (true) {
            try {
                lastApiResponse = buildAndSend(itemsToSend, request, conversationID)
            } catch (e: Exception) {
                val msg = "Failed to create response for conversation $conversationID: ${e.message}"
                logger.error(msg, e)
                throw IllegalStateException(msg, e)
            }

            responseIDs.add(lastApiResponse.id())

            logger.debug("Received response id={} conversation={}", lastApiResponse.id(), conversationID)

            val functionCallItems = lastApiResponse.output().filter { it.isFunctionCall() }

            if (functionCallItems.isEmpty()) {
                logger.debug("Final response reached id={}", lastApiResponse.id())
                break
            }

            itemsToSend = processFunctionCalls(functionCallItems)
        }

        val finalMessage = extractMessage(lastApiResponse)
        logger.debug("Final message extracted: {}", finalMessage.take(200))

        return Response(message = finalMessage)
    }

    override fun delete() {
        val conversation = conversation ?: throw IllegalStateException("Conversation not initialized")
        val conversationID = conversation.id()

        logger.debug("OpenAIRequestResponseChain: Deleting responses for conversation {}", conversationID)

        responseIDs.forEach { responseID ->
            logger.info("OpenAIRequestResponseChain: Deleting response {}", responseID)

            try {
                client.responses().delete(responseID)
            } catch (e: Exception) {
                logger.error("Failed to delete response {}", responseID, e)
                // Continue for now. Not much we can do ...
            }
        }

        logger.info("OpenAIRequestResponseChain: Deleting conversation {}", conversationID)

        try {
            client.conversations().delete(conversationID)
        } catch (e: Exception) {
            logger.error("Failed to delete conversation {}", conversationID, e)
            // Continue
        }

        this.conversation = null
        responseIDs.clear()
    }

    private fun initialUserMessage(request: Request): ResponseInputItem {
        return ResponseInputItem.ofMessage(
            ResponseInputItem.Message.builder()
                .addInputTextContent(request.input)
                .role(ResponseInputItem.Message.Role.USER)
                .build()
        )
    }

    private fun buildAndSend(
        items: List<ResponseInputItem>,
        request: Request,
        conversationID: String
    ): com.openai.models.responses.Response {
        val builder = ResponseCreateParams.builder()
            .input(ResponseCreateParams.Input.ofResponse(items))
            .instructions(request.instructions)
            .model(request.model)
            .temperature(request.temperature)
            .conversation(conversationID)

        request.tools.forEach { toolKClass ->
            logger.debug("Adding tool to response builder: {}", toolKClass.qualifiedName)
            builder.addTool(toolKClass.java)
        }

        request.responseType?.let {
            logger.debug("Setting response type to: {}", it.java)
            builder.text(it.java)
        }

        request.topP?.let {
            logger.debug("Setting topP to: {}", it)
            builder.topP(it)
        }

        logger.debug("Creating OpenAI response for conversation={}", conversationID)

        return client.responses().create(builder.build())
    }

    private fun processFunctionCalls(functionCallItems: List<ResponseOutputItem>): List<ResponseInputItem> {
        val nextItems = mutableListOf<ResponseInputItem>()

        for (item in functionCallItems) {
            val functionCall = item.asFunctionCall()
            val functionName = functionCall.name()
            logger.info("Model requested function call name={} callId={}", functionName, functionCall.callId())

            val toolClass = toolRegistry.resolve(functionName)
            if (toolClass == null) {
                val msg = "No tool registered for function name: $functionName"
                logger.error(msg)
                throw IllegalStateException(msg)
            }

            val nodeParams: JsonElement? = request.toolStaticParameters?.get(functionName)

            val toolOutput: String = try {
                logger.info("Invoking tool {} with arguments={} nodeParams={}", functionName, parsedArguments, nodeParams)
                toolRegistry.invokeTool(functionName, parsedArguments, nodeParams)
            } catch (e: Exception) {
                val msg = "Tool invocation failed for $functionName: ${e.message}"
                logger.error(msg, e)
                throw IllegalStateException(msg, e)
            }

            logger.info(
                "Tool {} executed successfully callId={} outputPreview={}",
                functionName,
                functionCall.callId(),
                toolOutput.take(200)
            )

            // When using a conversation, do not re-send the functionCall item
            nextItems.add(
                ResponseInputItem.ofFunctionCallOutput(
                    ResponseInputItem.FunctionCallOutput.builder()
                        .callId(functionCall.callId())
                        .output(toolOutput)
                        .build()
                )
            )
        }

        return nextItems
    }

    private fun deserializeTool(functionCall: ResponseFunctionToolCall): Tool {
        val functionName = functionCall.name()
        val toolClass = toolRegistry.resolve(functionName)
            ?: throw IllegalStateException("No tool registered for function name: $functionName")

        return ToolDeserializationUtil.deserialize(toolClass, json.parseToJsonElement(functionCall.arguments()))
    }

    private fun executeTool(tool: Tool, functionName: String, callId: String): String {
        return try {
            logger.debug("Executing tool {}", functionName)
            tool.execute()
        } catch (e: Exception) {
            val msg = "Tool execution failed for $functionName (callId=$callId): ${e.message}"
            logger.error(msg, e)
            throw IllegalStateException(msg, e)
        }
    }

    private fun extractMessage(response: com.openai.models.responses.Response): String {
        if (response.output().isEmpty()) {
            return ""
        }

        val stringBuilder = StringBuilder()

        response.output().forEach { outItem ->
            if (outItem.message().isPresent) {
                val message = outItem.message().get()

                message.content().forEach { content ->
                    val outputText = content.outputText()
                    if (outputText.isPresent) {
                        stringBuilder.append(outputText.get().text())
                        stringBuilder.append('\n')
                    }
                }
            }
        }

        return stringBuilder.toString().trimEnd()
    }
}
