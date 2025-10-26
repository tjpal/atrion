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

    fun getOutputs(executionId: String): List<OutputRecord> {
        return store[executionId]?.toList() ?: emptyList()
    }

    fun clearOutputs(executionId: String) {
        store.remove(executionId)
    }

    fun clearAll() {
        store.clear()
    }
}
