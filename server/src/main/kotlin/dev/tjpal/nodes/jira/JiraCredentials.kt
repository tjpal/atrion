package dev.tjpal.nodes.jira

import kotlinx.serialization.Serializable

@Serializable
data class JiraCredentials(
    val username: String? = null,
    val password: String? = null
)
