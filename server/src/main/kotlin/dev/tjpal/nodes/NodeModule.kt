package dev.tjpal.nodes

import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
abstract class NodeModule {
    @Binds
    @IntoMap
    @NodeFactoryKey("test")
    abstract fun bindTestNodeFactory(factory: TestNodeFactory): NodeFactory
}
