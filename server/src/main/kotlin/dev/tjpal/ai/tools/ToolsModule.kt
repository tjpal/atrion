package dev.tjpal.ai.tools

import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class ToolsModule {
    @Provides
    @Singleton
    fun provideToolRegistry(): ToolRegistry = ToolRegistry()
}