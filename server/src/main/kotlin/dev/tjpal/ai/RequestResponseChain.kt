package dev.tjpal.ai

abstract class RequestResponseChain {
    abstract fun createResponse(request: Request): Response
    abstract fun delete()
}