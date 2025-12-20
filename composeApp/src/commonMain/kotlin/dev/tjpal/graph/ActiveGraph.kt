package dev.tjpal.graph

import dev.tjpal.model.StatusEntry
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ActiveGraph(val graphInstanceId: String) {
    private var nodeLastStatusFlows: MutableMap<String, MutableStateFlow<StatusEntry?>> = mutableMapOf()
    private var currentExecutionId: String? = null
    private var lock = SynchronizedObject()

    fun appendStatus(status: StatusEntry) {
        synchronized(lock) {
            val isNewExecution = (currentExecutionId == null) || (status.executionId != currentExecutionId)

            if (isNewExecution) {
                currentExecutionId = status.executionId
                clearPreviousExecutionData()
            }

            val belongsToCurrentExecution = status.executionId == currentExecutionId

            if (belongsToCurrentExecution) {
                val nodeId = status.nodeId
                val nodeFlow = nodeLastStatusFlows.getOrPut(nodeId) { MutableStateFlow(null) }
                nodeFlow.value = status
            }
        }
    }

    fun observeNodeLastStatus(nodeId: String): StateFlow<StatusEntry?> {
        synchronized(lock) {
            return nodeLastStatusFlows.getOrPut(nodeId) { MutableStateFlow(null) }
        }
    }

    private fun clearPreviousExecutionData() {
        // Emit null to all existing flows to indicate reset
        for(flow in nodeLastStatusFlows.values) {
            flow.value = null
        }
    }
}