package dev.tjpal.di

import dev.tjpal.viewmodel.ReviewViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val reviewsViewModelModule = module {
    viewModel { ReviewViewModel(get()) }
}
