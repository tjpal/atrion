package dev.tjpal.graph

import dev.tjpal.client.RESTApiClient
import dev.tjpal.model.StatusEntry
import io.ktor.util.date.getTimeMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ActiveGraphService(private val api: RESTApiClient) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var streamingJob: Job? = null

    private val _activeGraph = MutableStateFlow<ActiveGraph?>(null)
    val activeGraph: StateFlow<ActiveGraph?> = _activeGraph

    private val _inTransition = MutableStateFlow(false)
    val inTransition: StateFlow<Boolean> = _inTransition

    fun isGraphActive(): Boolean {
        return _activeGraph.value != null
    }

    fun observeNodeLastStatus(nodeId: String): StateFlow<StatusEntry?> {
        val current = _activeGraph.value
        return current?.observeNodeLastStatus(nodeId) ?: MutableStateFlow(null)
    }

    suspend fun startActiveGraph(graphId: String): String {
        try {
            _inTransition.value = true
            val startTimestamp = getTimeMillis()
            val graphInstanceId = api.createGraphInstance(graphId)

            val existing = _activeGraph.value
            if (existing != null) {
                stopStreamingLocked()
            }

            val active = ActiveGraph(graphInstanceId = graphInstanceId)
            _activeGraph.value = active

            startStreamingEvents(startTimestamp)

            return graphInstanceId
        } catch(e: Exception) {
            println("Failed to start active graph: $e")
            return ""
        } finally {
            _inTransition.value = false
        }
    }

    suspend fun stopActiveGraph() {
        _inTransition.value = true
        val idToDelete = _activeGraph.value?.graphInstanceId ?: return

        try {
            api.deleteGraphInstance(idToDelete)
            stopStreamingLocked()

            _activeGraph.value = null
        } catch(e: Exception) {
            println("Failed to delete graph: $idToDelete")
        } finally {
            _inTransition.value = false
        }
    }

    private fun startStreamingEvents(since: Long) {
        if (streamingJob?.isActive == true) {
            return
        }

        if(_activeGraph.value == null) {
            println("No graph active")
            return
        }

        streamingJob = scope.launch {
            statusStreamingLoop(since)
        }
    }

    private suspend fun statusStreamingLoop(since: Long) {
        while (true) {
            try {
                api.streamStatuses(since = since) { status: StatusEntry ->
                    val current = _activeGraph.value
                    if (current != null && status.graphInstanceId == current.graphInstanceId) {
                        current.appendStatus(status)
                    } else {
                        // Ignore statuses for other instances. We show only the last execution ID in the UI.
                    }
                }
            } catch (e: Exception) {
                break
            }
        }
    }

    private suspend fun stopStreamingLocked() {
        val job = streamingJob

        if (job != null) {
            job.cancelAndJoin()
            streamingJob = null
        }
    }
}
