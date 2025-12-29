package dev.tjpal.config

import dev.tjpal.logging.logger
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Named

class ConfigLoader @Inject constructor(
    @param:Named("configPath") private val configPath: String,
    private val json: Json
) {
    private val logger = logger<ConfigLoader>()

    private val default = Config(
        httpHost = "0.0.0.0",
        httpPort = 8081,
        udsPath = "/tmp/atrion.socket",
        storageDirectory = System.getProperty("user.home") + "/.atrion/graphs",
        openAICredentialPath = System.getProperty("user.home") + "/.atrion/cred",
        statusRetentionEntries = 10000,
        secretsDirectory = System.getProperty("user.home") + "/.atrion/secrets",
        secretsMasterKeyPath = System.getProperty("user.home") + "/.atrion/secrets_master_key",
        nodeMemoryDirectory = System.getProperty("user.home") + "/.atrion/node-memory",
    )

    fun loadOrCreate(): Config {
        val file = File(configPath)
        return try {
            val text = file.readText()
            json.decodeFromString<Config>(text)
        } catch (e: Exception) {
            logger.error("Error reading config. Creating default file. Please review and restart.", e)

            file.parentFile?.mkdirs()
            file.writeText(json.encodeToString(default))

            default
        }
    }
}
