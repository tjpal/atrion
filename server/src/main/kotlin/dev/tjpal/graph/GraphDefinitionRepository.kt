package dev.tjpal.graph

import dev.tjpal.model.GraphDefinition

interface GraphDefinitionRepository {
    fun add(graph: GraphDefinition): String
    fun get(id: String): GraphDefinition
    fun getAll(): List<GraphDefinition>
    fun delete(id: String)
    fun replace(id: String, graph: GraphDefinition)
}
