package dev.tjpal.nodes

import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
abstract class NodeModule {
    @Binds
    @IntoMap
    @NodeFactoryKey("rest_input")
    abstract fun bindRestInputNodeFactory(factory: RestInputNodeFactory): NodeFactory

    @Binds
    @IntoMap
    @NodeFactoryKey("llm_processor")
    abstract fun bindLLMProcessingNodeFactory(factory: LLMProcessingNodeFactory): NodeFactory

    @Binds
    @IntoMap
    @NodeFactoryKey("sync_output")
    abstract fun bindOutputSinkNodeFactory(factory: OutputSinkNodeFactory): NodeFactory
}
