package dev.tjpal.graph

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory thread-safe repository for ActiveGraph objects.
 */
@Singleton
class ActiveGraphRepository @Inject constructor() {
    private val activeGraphs: ConcurrentHashMap<String, ActiveGraph> = ConcurrentHashMap()

    fun start(graphId: String): String {
        val id = UUID.randomUUID().toString()
        val graph = ActiveGraph(id = id, graphId = graphId)

        activeGraphs[id] = graph

        return id
    }

    fun get(id: String): ActiveGraph {
        return activeGraphs[id] ?: throw IllegalArgumentException("No active execution with id: $id")
    }

    fun delete(id: String) {
        activeGraphs.remove(id) ?: throw IllegalArgumentException("No active execution with id: $id")
    }

    fun listAll(): List<ActiveGraph> = activeGraphs.values.toList()
}
