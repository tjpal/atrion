package dev.tjpal.di

import org.koin.dsl.module
import dev.tjpal.repository.GraphRepository

val repositoryModule = module {
    single {
        GraphRepository(get())
    }
}
