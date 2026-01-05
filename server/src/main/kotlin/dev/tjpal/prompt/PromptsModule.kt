package dev.tjpal.prompt

import dagger.Binds
import dagger.Module

@Module
abstract class PromptsModule {
    @Binds
    abstract fun bindPromptsRepository(impl: FileSystemPromptsRepository): PromptsRepository
}