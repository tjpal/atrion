package dev.tjpal.graph.hooks

import dev.tjpal.graph.ActiveGraph
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Registry for REST input mappings. Maps (graphInstanceId, nodeId) -> ActiveGraph.
 * When an HTTP input arrives, the registry forwards it to the ActiveGraph.onInputEvent method.
 */
@Singleton
class RestInputRegistry @Inject constructor() {
    private data class Key(val graphInstanceId: String, val nodeId: String)
    private val map = ConcurrentHashMap<Key, ActiveGraph>()

    fun register(graphInstanceId: String, nodeId: String, graph: ActiveGraph) {
        map[Key(graphInstanceId, nodeId)] = graph
    }

    fun unregister(graphInstanceId: String, nodeId: String) {
        map.remove(Key(graphInstanceId, nodeId))
    }

    fun handleIncoming(graphInstanceId: String, nodeId: String, payload: String, executionId: String): Boolean {
        val graph = map[Key(graphInstanceId, nodeId)] ?: return false

        try {
            graph.onInputEvent(nodeId, payload, executionId)
            return true
        } catch (e: Exception) {
            println("Error forwarding REST input to ActiveGraph: ${e.message}")
            return false
        }
    }
}
