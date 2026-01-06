package dev.tjpal.di

import dagger.Component
import dev.tjpal.App
import dev.tjpal.ai.AIModule
import dev.tjpal.ai.tools.ToolsModule
import dev.tjpal.config.ConfigModule
import dev.tjpal.filesystem.FileSystemModule
import dev.tjpal.graph.StorageModule
import dev.tjpal.nodes.NodeModule
import dev.tjpal.nodes.jira.JiraClientModule
import dev.tjpal.prompt.PromptsModule
import dev.tjpal.secrets.SecretsModule
import javax.inject.Singleton

@Singleton
@Component(modules = [
    ConfigModule::class,
    NodeModule::class,
    AIModule::class,
    StorageModule::class,
    FileSystemModule::class,
    ToolsModule::class,
    SecretsModule::class,
    JiraClientModule::class,
    PromptsModule::class
])
interface AppComponent {
    fun app(): App
}
