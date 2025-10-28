package dev.tjpal.nodes

import dev.tjpal.model.NodeDefinition
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NodeRepository @Inject constructor(
    private val factoryMap: Map<String, @JvmSuppressWildcards NodeFactory>
) {
    fun getAllDefinitions(): List<NodeDefinition> =
        factoryMap.values.map(NodeFactory::definition)

    fun createNodeInstance(type: String, parametersJson: String): Node {
        val factory = factoryMap[type] ?: throw IllegalArgumentException("No factory registered for node type: $type")

        return factory.createNode(parametersJson)
    }
}
