package dev.tjpal.atrion.admin.config

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class Config(
    val serverUrl: String,
    val serverPort: Int,
    val udsPath: String,
    val keyDirectory: String,
    val adminUsername: String
) {
    companion object {
        fun tryLoad(): Config? {
            val file = File(System.getProperty("user.home") + "/.atrion-admin/config.json")
            val json = Json { ignoreUnknownKeys = true }

            return try {
                val text = file.readText()
                json.decodeFromString<Config>(text)
            } catch (e: Exception) {
                return null
            }
        }
    }

    fun store(filename: String) {
        val json = kotlinx.serialization.json.Json { prettyPrint = true }
        val text = json.encodeToString(this)
        val file = File(filename)

        file.parentFile?.mkdirs()
        file.writeText(text)
    }
}
