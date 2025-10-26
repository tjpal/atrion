package dev.tjpal.graph.hooks

import dev.tjpal.graph.ActiveGraph
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Registry for REST input mappings. Maps (executionId, nodeId) -> ActiveGraph.
 * When an HTTP input arrives, the registry forwards it to the ActiveGraph.onInputEvent method.
 */
@Singleton
class RestInputRegistry @Inject constructor() {
    private data class Key(val executionId: String, val nodeId: String)
    private val map = ConcurrentHashMap<Key, ActiveGraph>()

    fun register(executionId: String, nodeId: String, graph: ActiveGraph) {
        map[Key(executionId, nodeId)] = graph
    }

    fun unregister(executionId: String, nodeId: String) {
        map.remove(Key(executionId, nodeId))
    }

    fun handleIncoming(executionId: String, nodeId: String, payload: String): Boolean {
        val graph = map[Key(executionId, nodeId)] ?: return false

        try {
            graph.onInputEvent(nodeId, payload)
            return true
        } catch (e: Exception) {
            println("Error forwarding REST input to ActiveGraph: ${e.message}")
            return false
        }
    }
}