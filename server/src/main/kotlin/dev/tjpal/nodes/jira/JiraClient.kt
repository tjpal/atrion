package dev.tjpal.nodes.jira

import com.atlassian.jira.rest.client.api.IssueRestClient
import com.atlassian.jira.rest.client.api.JiraRestClient
import com.atlassian.jira.rest.client.api.domain.Issue
import com.atlassian.jira.rest.client.api.domain.SearchResult
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory
import java.net.URI

class JiraClient {
    private val jiraServerUrl = URI("https://your-jira-server.com")
    private val client: JiraRestClient = AsynchronousJiraRestClientFactory().
        createWithBasicHttpAuthentication(jiraServerUrl, "your-username", "password")

    fun searchAndFetchFullIssues(jql: String): List<Issue> {
        val searchClient = client.searchClient
        val issueClient = client.issueClient

        // JQL search (page size example: 50)
        val fieldSet = mutableSetOf(
            "summary",
            "issuetype",
            "created",
            "updated",
            "project",
            "status"
        )
        val searchResult: SearchResult = searchClient.searchJql(jql, 50, 0, fieldSet).claim()

        // For each issue key, fetch with CHANGELOG expanded (history)
        val expand = listOf(IssueRestClient.Expandos.CHANGELOG)

        return searchResult.issues.map { basicIssue ->
            val key = basicIssue.key
            issueClient.getIssue(key, expand).claim()
        }
    }
}