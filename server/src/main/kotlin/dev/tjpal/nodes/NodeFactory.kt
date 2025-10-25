package dev.tjpal.nodes

import dev.tjpal.graph.model.NodeDefinition

interface NodeFactory {
    // Provides the static definition that describes the node
    fun definition(): NodeDefinition

    // Creates a runtime node instance backed by the provided opaque parameter payload.
    fun createNode(parametersJson: String): Node
}

interface Node
