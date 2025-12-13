package dev.tjpal.repository

import dev.tjpal.client.RESTApiClient
import dev.tjpal.model.ExtendedNodeDefinition
import dev.tjpal.model.GraphDefinition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

/**
 * Concrete GraphRepository that talks to the backend via RESTApiClient and maintains the state of the currently edited
 * graph
 */
class GraphRepository(
    private val api: RESTApiClient
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _graph: MutableStateFlow<LoadState<GraphDefinition>> = MutableStateFlow(LoadState.Loading)
    val graph: StateFlow<LoadState<GraphDefinition>> = _graph

    private val _nodeDefinitions: MutableStateFlow<LoadState<List<ExtendedNodeDefinition>>> = MutableStateFlow(LoadState.Loading)
    val nodeDefinitions: StateFlow<LoadState<List<ExtendedNodeDefinition>>> = _nodeDefinitions

    suspend fun refresh() {
        withContext(scope.coroutineContext + Dispatchers.Default) {
            _nodeDefinitions.value = LoadState.Loading
            _graph.value = LoadState.Loading

            try {
                val definitions = api.getNodeDefinitions()
                val clientDefs = definitions.map { ExtendedNodeDefinition(it) }
                _nodeDefinitions.value = LoadState.Ready(clientDefs)
            } catch (exception: Exception) {
                val message = "Failed to fetch node definitions from server ${exception.message ?: exception}"

                _nodeDefinitions.value = LoadState.Error(message, exception)
                _graph.value = LoadState.Error(message, exception)

                return@withContext
            }

            try {
                val graphs = api.getGraphs()

                if (graphs.isEmpty()) {
                    val empty = createDefaultGraphDefinition()
                    val newId = api.createGraph(empty)

                    _graph.value = LoadState.Ready(empty.copy(id = newId))
                } else {
                    _graph.value = LoadState.Ready(graphs.first())
                }
            } catch (exception: Exception) {
                val message = "Failed to fetch graphs from server ${exception.message ?: exception}"
                _graph.value = LoadState.Error(message, exception)
            }
        }
    }

    suspend fun save(graphDefinition: GraphDefinition) {
        withContext(scope.coroutineContext + Dispatchers.Default) {
            val graph = (_graph.value as? LoadState.Ready)?.data

            // Likely we are loading or in error state. Returns this for now as an error to the viewmodel.
            if(graph == null) {
                throw IllegalStateException("Cannot save graph: no current graph loaded")
            }

            try {
                val graphId = graph.id ?: throw IllegalStateException("Current graph has no id")

                api.replaceGraph(graphId, graphDefinition)
                val updated = graphDefinition.copy(id = graphId)

                _graph.value = LoadState.Ready(updated)
            } catch (exception: Exception) {
                val message = "Failed to save graph to server: ${exception.message ?: exception}"
                _graph.value = LoadState.Error(message, exception)
            }
        }
    }

    private fun createDefaultGraphDefinition(): GraphDefinition {
        return GraphDefinition(
            id = "",
            projectId = "",
            nodes = emptyList(),
            edges = emptyList()
        )
    }
}
