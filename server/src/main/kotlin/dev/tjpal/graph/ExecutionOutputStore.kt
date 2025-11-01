package dev.tjpal.graph

import kotlinx.serialization.Serializable
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class OutputRecord(
    val nodeId: String,
    val payload: String
)

@Singleton
class ExecutionOutputStore @Inject constructor() {
    private val store: ConcurrentHashMap<String, MutableList<OutputRecord>> = ConcurrentHashMap()

    fun appendOutput(executionId: String, nodeId: String, payload: String) {
        val list = store.computeIfAbsent(executionId) { Collections.synchronizedList(mutableListOf()) }
        list.add(OutputRecord(nodeId = nodeId, payload = payload))
    }

    /**
     * Retrieves outputs for the given executionId. If clearAfter is true, the outputs are removed from the store
     * atomically and returned; otherwise the outputs are returned as a snapshot and left in the store.
     */
    fun getOutputs(executionId: String, clearAfter: Boolean = false): List<OutputRecord> {
        return if (clearAfter) {
            val removed = store.remove(executionId)
            removed?.toList() ?: emptyList()
        } else {
            store[executionId]?.toList() ?: emptyList()
        }
    }

    fun clearOutputs(executionId: String) {
        store.remove(executionId)
    }

    fun clearAll() {
        store.clear()
    }
}
