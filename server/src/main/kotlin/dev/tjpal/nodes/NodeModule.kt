package dev.tjpal.nodes

import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap

@Module
class NodeModule {
    @Provides
    @IntoMap
    @NodeFactoryKey("test")
    fun providesTestNodeFactory(): NodeFactory {
        return TestNodeFactory()
    }
}