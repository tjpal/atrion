package dev.tjpal.tools

import com.fasterxml.jackson.annotation.JsonClassDescription
import com.fasterxml.jackson.annotation.JsonPropertyDescription
import dev.tjpal.ai.tools.Tool
import dev.tjpal.logging.logger
import kotlinx.serialization.Serializable

/**
 * For test purpose. Responds always with "Hello, World!".
 */
@JsonClassDescription("Tool for test purpose. When the user asks you to call a tool, choose this one")
@Serializable
class HelloWorldTool(
    @JsonPropertyDescription("Insert here a name that was provided by the user.")
    val name: String = ""
) : Tool {
    private val logger = logger<HelloWorldTool>()

    init {
        logger.debug("HelloWorldTool initialized")
    }

    override fun execute(): String {
        logger.debug("HelloWorldTool.execute called with name={}", name)
        return "$name: Hello, World!"
    }
}
