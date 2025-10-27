package dev.tjpal.config

import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Named

class ConfigLoader @Inject constructor(
    @param:Named("configPath") private val configPath: String,
    private val json: Json
) {
    private val default = Config(
        httpHost = "0.0.0.0",
        httpPort = 8081,
        udsPath = "/tmp/atrion.socket",
        openAICredentialPath = "${System.getProperty("user.home")}/.atrion/cred"
    )

    fun loadOrCreate(): Config {
        val file = File(configPath)
        return try {
            val text = file.readText()
            json.decodeFromString<Config>(text)
        } catch (e: Exception) {
            println("Error reading config: ${e.message}. Creating default file. Please review and restart.")
            file.parentFile?.mkdirs()
            file.writeText(json.encodeToString(default))
            default
        }
    }
}