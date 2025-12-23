package dev.tjpal.nodes

import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

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
}
