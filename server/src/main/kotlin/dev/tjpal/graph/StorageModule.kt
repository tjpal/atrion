package dev.tjpal.graph

import dagger.Binds
import dagger.Module

@Module
abstract class StorageModule {
    @Binds
    abstract fun bindGraphDefinitionRepository(impl: FileSystemGraphDefinitionRepository): GraphDefinitionRepository
}
