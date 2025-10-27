package dev.tjpal.config

import kotlinx.serialization.Serializable

@Serializable
data class Config(
    val httpHost: String,
    val httpPort: Int,
    val udsPath: String,
    val openAICredentialPath: String
)