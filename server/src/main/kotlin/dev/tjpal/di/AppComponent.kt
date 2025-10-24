package dev.tjpal.di

import javax.inject.Singleton
import dagger.Component
import dev.tjpal.App
import dev.tjpal.config.ConfigModule

@Singleton
@Component(modules = [ConfigModule::class])
interface AppComponent {
    fun app(): App
}