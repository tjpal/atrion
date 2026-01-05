package dev.tjpal.ai

import dagger.Binds
import dagger.Module

@Module
abstract class AIModule {
    @Binds
    abstract fun bindAIService(impl: OpenAILLM): LLM
}