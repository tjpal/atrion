package dev.tjpal.nodes.jira

import com.atlassian.jira.rest.client.api.JiraRestClient

interface JiraRestClientFactory {
    fun create(serverUrl: String, secretId: String): JiraRestClient
}
