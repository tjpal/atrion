package dev.tjpal.graph

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory thread-safe repository for active graphs.
 */
@Singleton
class ActiveGraphRepository @Inject constructor(
    private val activeGraphFactory: ActiveGraphFactory
) {
    private val activeGraphs: ConcurrentHashMap<String, ActiveGraph> = ConcurrentHashMap()

    fun start(graphId: String): String {
        val id = UUID.randomUUID().toString()
        val graph = activeGraphFactory.create(id, graphId)

        graph.activate()
        activeGraphs[id] = graph

        return id
    }

    fun get(id: String): ActiveGraph {
        return activeGraphs[id] ?: throw IllegalArgumentException("No active execution with id: $id")
    }

    fun delete(id: String) {
        val graph = activeGraphs.remove(id) ?: throw IllegalArgumentException("No active execution with id: $id")
        try {
            graph.stop()
        } catch (e: Exception) {
            // Ignore cleanup errors. Can't act on them.
        }
    }

    fun listAll(): List<ActiveGraph> = activeGraphs.values.toList()
}
