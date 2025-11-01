package dev.tjpal.graph

import dev.tjpal.model.GraphDefinition
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory thread-safe repository for GraphDefinition objects (keeps previous behavior).
 */
@Singleton
class InMemoryGraphDefinitionRepository @Inject constructor() : GraphDefinitionRepository {
    private val store: ConcurrentHashMap<String, GraphDefinition> = ConcurrentHashMap()

    override fun add(graph: GraphDefinition): String {
        val id = UUID.randomUUID().toString()
        val toStore = graph.copy(id = id)
        store[id] = toStore
        return id
    }

    override fun get(id: String): GraphDefinition {
        return store[id] ?: throw IllegalArgumentException("No graph with id: $id")
    }

    override fun getAll(): List<GraphDefinition> {
        return store.values.toList()
    }

    override fun delete(id: String) {
        store.remove(id) ?: throw IllegalArgumentException("No graph with id: $id")
    }

    override fun replace(id: String, graph: GraphDefinition) {
        val toStore = graph.copy(id = id)

        val result = store.computeIfPresent(id) { _, _ -> toStore }

        if (result == null) throw IllegalArgumentException("No graph with id: $id")
    }
}
