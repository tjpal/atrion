package dev.tjpal.tools.jira.dto

import kotlinx.serialization.Serializable

@Serializable
data class JiraMainAttributes(
    val key: String,
    val summary: String? = null,
    val description: String? = null,
    val issueType: String? = null,
    val created: String? = null,
    val updated: String? = null,
    val projectKey: String? = null,
    val reporter: String? = null,
    val assignee: String? = null
)

@Serializable
data class JiraComment(
    val author: String? = null,
    val created: String? = null,
    val body: String? = null
)

@Serializable
data class JiraHistoryChangeItem(
    val field: String? = null,
    val from: String? = null,
    val to: String? = null
)

@Serializable
data class JiraHistoryItem(
    val author: String? = null,
    val created: String? = null,
    val items: List<JiraHistoryChangeItem> = emptyList()
)

@Serializable
data class JiraIssueDto(
    val main: JiraMainAttributes,
    val comments: List<JiraComment>? = null,
    val history: List<JiraHistoryItem>? = null
)
