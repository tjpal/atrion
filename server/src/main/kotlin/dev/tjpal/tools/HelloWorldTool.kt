package dev.tjpal.tools

import com.fasterxml.jackson.annotation.JsonClassDescription

/**
 * For test purpose. Responds always with "Hello, World!".
 */
@JsonClassDescription("Tool for test purpose. When the user asks you to call a tool, choose this one")
class HelloWorldTool : Tool {
    fun execute(): String {
        return "Hello, World!"
    }
}
