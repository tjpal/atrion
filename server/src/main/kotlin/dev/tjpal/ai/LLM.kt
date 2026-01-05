package dev.tjpal.ai

interface LLM {
    fun createResponseRequestChain(): RequestResponseChain
    fun transcriptAudio(filePath: String): String
    fun runResponseGarbageCollection()
}
