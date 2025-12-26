package dev.tjpal.secrets

import kotlinx.serialization.Serializable

@Serializable
data class SecretRecord(
    val id: String,
    val name: String,
    val type: String,
    val version: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val iv: String,
    val ciphertext: String,
    val tags: List<String> = emptyList()
)

@Serializable
data class SecretMetadata(
    val id: String,
    val name: String,
    val type: String,
    val version: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val tags: List<String> = emptyList()
)
