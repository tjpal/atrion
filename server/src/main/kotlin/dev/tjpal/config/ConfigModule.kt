package dev.tjpal.config

import dagger.Module
import dagger.Provides
import kotlinx.serialization.json.Json
import javax.inject.Named
import javax.inject.Singleton

@Module
class ConfigModule {

    @Provides
    @Singleton
    fun providesJson(): Json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    @Provides
    @Singleton
    @Named("configPath")
    fun providesConfigPath(): String = System.getenv("ATRION_CONFIG_PATH") ?: "/etc/atrion/config.json"

    @Provides
    @Singleton
    fun providesConfig(loader: ConfigLoader): Config = loader.loadOrCreate()
}