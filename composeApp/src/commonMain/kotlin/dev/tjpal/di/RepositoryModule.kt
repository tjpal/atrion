package dev.tjpal.di

import dev.tjpal.graph.GraphRepository
import org.koin.dsl.module

val repositoryModule = module {
    single {
        GraphRepository(get())
    }
}
