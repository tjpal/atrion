package dev.tjpal.tools

import com.fasterxml.jackson.annotation.JsonClassDescription
import com.fasterxml.jackson.annotation.JsonPropertyDescription
import dev.tjpal.ai.openai.OpenAIRequestResponseChain
import dev.tjpal.logging.logger

/**
 * For test purpose. Responds always with "Hello, World!".
 */
@JsonClassDescription("Tool for test purpose. When the user asks you to call a tool, choose this one")
class HelloWorldTool : Tool {
    private val logger = logger<OpenAIRequestResponseChain>()

    init {
        logger.debug("HelloWorldTool initialized")
    }

    @JsonPropertyDescription("Insert here a name that was provided by the user.")
    val name: String = ""

    fun execute(): String {
        logger.debug("HelloWorldTool.execute called with name={}", name)
        return "$name: Hello, World!"
    }
}
