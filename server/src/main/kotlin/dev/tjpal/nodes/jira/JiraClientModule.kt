package dev.tjpal.nodes.jira

import dagger.Binds
import dagger.Module
import javax.inject.Singleton

@Module
abstract class JiraClientModule {
    @Binds
    @Singleton
    abstract fun bindJiraRestClientFactory(factory: DefaultJiraRestClientFactory): JiraRestClientFactory
}
