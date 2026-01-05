package dev.tjpal.prompt

interface PromptsRepository {
    /**
     * List logical prompt names available in the repository.
     */
    fun listPromptNames(): List<String>

    /**
     * Read the prompt content by name. Always performs a fresh read from disk.
     * Throws IllegalArgumentException if the prompt does not exist.
     */
    fun getPrompt(name: String): String

    /**
     * Returns whether the prompt exists.
     */
    fun exists(name: String): Boolean
}
