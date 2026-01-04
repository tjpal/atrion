package dev.tjpal.di

import dev.tjpal.repo.ReviewRepository
import org.koin.dsl.module

val reviewsRepositoryModule = module {
    single { ReviewRepository(get()) }
}
