package dev.tjpal.ai

import dagger.Binds
import dagger.Module
import dev.tjpal.ai.openai.OpenAILLM

@Module
abstract class AIModule {
    @Binds
    abstract fun bindAIService(impl: OpenAILLM): LLM
}