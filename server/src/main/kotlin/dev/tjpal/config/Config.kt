package dev.tjpal.config

import kotlinx.serialization.Serializable

@Serializable
data class Config(
    val httpHost: String,
    val httpPort: Int,
    val udsPath: String,
    val storageDirectory: String,
    val promptDirectories: List<String>,
    val openAIGarbageCollectorPath: String,
    val openAICredentialPath: String,
    val statusRetentionEntries: Int,
    val secretsDirectory: String,
    val secretsMasterKeyPath: String,
    val nodeMemoryDirectory: String
)
