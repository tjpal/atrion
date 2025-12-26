package dev.tjpal.nodes

import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import dev.tjpal.nodes.jira.JiraToolNodeFactory

@Module
abstract class NodeModule {
    @Binds
    @IntoMap
    @NodeFactoryKey("REST Input")
    abstract fun bindRestInputNodeFactory(factory: RestInputNodeFactory): NodeFactory

    @Binds
    @IntoMap
    @NodeFactoryKey("LLM Processor")
    abstract fun bindLLMProcessingNodeFactory(factory: LLMProcessingNodeFactory): NodeFactory

    @Binds
    @IntoMap
    @NodeFactoryKey("Sink Output")
    abstract fun bindOutputSinkNodeFactory(factory: OutputSinkNodeFactory): NodeFactory

    @Binds
    @IntoMap
    @NodeFactoryKey("HelloWorld Tool")
    abstract fun bindHelloWorldToolNodeFactory(factory: HelloWorldToolNodeFactory): NodeFactory

    @Binds
    @IntoMap
    @NodeFactoryKey("Jira Polling Input")
    abstract fun bindJiraPollingNodeFactory(factory: dev.tjpal.nodes.jira.JiraPollingNodeFactory): NodeFactory

    @Binds
    @IntoMap
    @NodeFactoryKey("Jira Tool")
    abstract fun bindJiraToolNodeFactory(factory: JiraToolNodeFactory): NodeFactory
}
