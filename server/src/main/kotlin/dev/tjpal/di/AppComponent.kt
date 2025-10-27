package dev.tjpal.di

import dagger.Component
import dev.tjpal.App
import dev.tjpal.ai.AIModule
import dev.tjpal.config.ConfigModule
import dev.tjpal.nodes.NodeModule
import javax.inject.Singleton

@Singleton
@Component(modules = [ConfigModule::class, NodeModule::class, AIModule::class])
interface AppComponent {
    fun app(): App
}
