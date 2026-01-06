package dev.tjpal.nodes

import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import dev.tjpal.nodes.jira.JiraPollingNodeFactory
import dev.tjpal.nodes.jira.JiraToolNodeFactory
import dev.tjpal.nodes.jira.JiraWriteToolNodeFactory
import dev.tjpal.nodes.memory.MemoryNodeFactory
import dev.tjpal.nodes.hitl.HumanInTheLoopNodeFactory
import dev.tjpal.nodes.multiplexer.MultiplexerNodeFactory
import dev.tjpal.nodes.machineinloop.MachineInTheLoopNodeFactory

@Module
abstract class NodeModule {
    @Binds
    @IntoMap
    @NodeFactoryKey("RESTEndpointInput")
    abstract fun bindRestInputNodeFactory(factory: RestInputNodeFactory): NodeFactory

    @Binds
    @IntoMap
    @NodeFactoryKey("LLMProcessor")
    abstract fun bindLLMProcessingNodeFactory(factory: LLMProcessingNodeFactory): NodeFactory

    @Binds
    @IntoMap
    @NodeFactoryKey("RESTSinkOutput")
    abstract fun bindOutputSinkNodeFactory(factory: OutputSinkNodeFactory): NodeFactory

    @Binds
    @IntoMap
    @NodeFactoryKey("JiraPollingInput")
    abstract fun bindJiraPollingNodeFactory(factory: JiraPollingNodeFactory): NodeFactory

    @Binds
    @IntoMap
    @NodeFactoryKey("JiraTool")
    abstract fun bindJiraToolNodeFactory(factory: JiraToolNodeFactory): NodeFactory

    @Binds
    @IntoMap
    @NodeFactoryKey("JiraWriteTool")
    abstract fun bindJiraWriteToolNodeFactory(factory: JiraWriteToolNodeFactory): NodeFactory

    @Binds
    @IntoMap
    @NodeFactoryKey("MemoryTool")
    abstract fun bindMemoryNodeFactory(factory: MemoryNodeFactory): NodeFactory

    @Binds
    @IntoMap
    @NodeFactoryKey("HumanInTheLoop")
    abstract fun bindHumanInTheLoopNodeFactory(factory: HumanInTheLoopNodeFactory): NodeFactory

    @Binds
    @IntoMap
    @NodeFactoryKey("Multiplexer")
    abstract fun bindMultiplexerNodeFactory(factory: MultiplexerNodeFactory): NodeFactory

    @Binds
    @IntoMap
    @NodeFactoryKey("MachineInTheLoop")
    abstract fun bindMachineInTheLoopNodeFactory(factory: MachineInTheLoopNodeFactory): NodeFactory
}
