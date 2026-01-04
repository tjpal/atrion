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
     * Store or overwrite a prompt. Returns the sanitized filename used for storage.
     */
    fun putPrompt(name: String, content: String): String

    /**
     * Delete a prompt. Returns true if a file was deleted, false when not found.
     */
    fun deletePrompt(name: String): Boolean

    /**
     * Returns whether the prompt exists.
     */
    fun exists(name: String): Boolean
}
