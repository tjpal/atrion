package dev.tjpal.graph

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import dev.tjpal.logging.logger

/**
 * In-memory thread-safe repository for active graphs.
 */
@Singleton
class ActiveGraphRepository @Inject constructor(
    private val activeGraphFactory: ActiveGraphFactory
) {
    private val logger = logger<ActiveGraphRepository>()
    private val activeGraphs: ConcurrentHashMap<String, ActiveGraph> = ConcurrentHashMap()

    fun start(graphId: String): String {
        val id = UUID.randomUUID().toString()
        val graph = activeGraphFactory.create(id, graphId)

        graph.activate()
        activeGraphs[id] = graph

        logger.info("Active graph started id={} graphId={}", id, graphId)

        return id
    }

    fun get(id: String): ActiveGraph {
        return activeGraphs[id] ?: throw IllegalArgumentException("No active execution with id: $id")
    }

    fun delete(id: String) {
        val graph = activeGraphs.remove(id) ?: throw IllegalArgumentException("No active execution with id: $id")
        try {
            graph.stop()
            logger.info("Active graph stopped id={}", id)
        } catch (e: Exception) {
            // Ignore cleanup errors. Can't act on them.
            logger.debug("Error stopping graph id={}: {}", id, e.message)
        }
    }

    fun listAll(): List<ActiveGraph> = activeGraphs.values.toList()
}
