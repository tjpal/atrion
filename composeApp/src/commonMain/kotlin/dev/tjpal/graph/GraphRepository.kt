package dev.tjpal.graph

import dev.tjpal.client.RESTApiClient
import dev.tjpal.model.EdgeInstance
import dev.tjpal.model.ExtendedNodeDefinition
import dev.tjpal.model.GraphDefinition
import dev.tjpal.model.NodeInstance
import dev.tjpal.model.Position
import dev.tjpal.util.decodePngBase64ToImageBitmap
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

    private val _loadedGraphWasModified = MutableStateFlow(false)
    val loadedGraphWasModified: StateFlow<Boolean> = _loadedGraphWasModified

    suspend fun refresh() {
        withContext(scope.coroutineContext + Dispatchers.Default) {
            _nodeDefinitions.value = LoadState.Loading
            _graph.value = LoadState.Loading
            _loadedGraphWasModified.value = false

            try {
                val definitions = api.getNodeDefinitions()

                val clientDefinitions = definitions.map {
                    val iconImage = decodePngBase64ToImageBitmap(it.icon)
                    ExtendedNodeDefinition(
                        definition = it,
                        iconImage = iconImage
                    )
                }
                _nodeDefinitions.value = LoadState.Ready(clientDefinitions)
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
            // Likely we are loading or in error state. Returns this for now as an error to the viewmodel.
            val graph = (_graph.value as? LoadState.Ready)?.data
                ?: throw IllegalStateException("Cannot save graph: no current graph loaded")

            try {
                val graphId = graph.id ?: throw IllegalStateException("Current graph has no id")
                api.replaceGraph(graphId, graphDefinition)

                _loadedGraphWasModified.value = false
            } catch (exception: Exception) {
                val message = "Failed to save graph to server: ${exception.message ?: exception}"
                _graph.value = LoadState.Error(message, exception)
            }
        }
    }

    fun setNodePosition(nodeId: String, x: Int, y: Int) {
        val readyState = _graph.value as? LoadState.Ready
            ?: throw IllegalStateException("Cannot set node position: no current graph loaded")
        val graph = readyState.data

        graph.nodes.firstOrNull { it.id == nodeId }?.let {
            it.position = Position(x, y)
        }

        _loadedGraphWasModified.value = true

        // Here we don't update the model as a minor optimization. When major updates
        // create an update graph, the position will be applied anyway. The same applies
        // for saving the graph. The visual representation does not rely on the model's node positions
        // so there it's updated correctly.
    }

     fun addNode(node: NodeInstance) {
         val readyState = _graph.value as? LoadState.Ready ?: throw IllegalStateException("Cannot add node: no current graph loaded")
         val graph = readyState.data

         if (graph.nodes.any { it.id == node.id }) {
             println("Node with id ${node.id} already exists in the graph")
             return
         }

         val updatedGraph = GraphDefinition(
             id = graph.id,
             projectId = graph.projectId,
             nodes = graph.nodes + node,
             edges = graph.edges,
         )

         _graph.value = LoadState.Ready(updatedGraph)
         _loadedGraphWasModified.value = true
     }

    fun removeNode(nodeId: String) {
        val current = _graph.value as? LoadState.Ready ?: throw IllegalStateException("Cannot remove node: no current graph loaded")
        val graph = current.data

        val nodesAfter = graph.nodes.filter { it.id != nodeId }
        val edgesAfter = graph.edges.filter { e -> !(e.fromNodeId == nodeId || e.toNodeId == nodeId) }

        val updatedGraph = GraphDefinition(
            id = graph.id,
            projectId = graph.projectId,
            nodes = nodesAfter,
            edges = edgesAfter
        )

        _graph.value = LoadState.Ready(updatedGraph)
        _loadedGraphWasModified.value = true
    }

    fun addEdge(fromNodeId: String, fromConnectorId: String, toNodeId: String, toConnectorId: String) {
        val current = _graph.value as? LoadState.Ready ?: throw IllegalStateException("Cannot add edge: no current graph loaded")
        val graph = current.data

        val newEdge = EdgeInstance(
            fromNodeId = fromNodeId,
            fromConnectorId = fromConnectorId,
            toNodeId = toNodeId,
            toConnectorId = toConnectorId
        )

        val updatedGraph = GraphDefinition(
            id = graph.id,
            projectId = graph.projectId,
            nodes = graph.nodes,
            edges = graph.edges + newEdge
        )

        _graph.value = LoadState.Ready(updatedGraph)
        _loadedGraphWasModified.value = true
    }

    fun removeEdge(nodeId: String, connectorId: String) {
        val current = _graph.value as? LoadState.Ready ?: throw IllegalStateException("Cannot remove edge: no current graph loaded")
        val graph = current.data

        val edgesAfter = graph.edges.filter { e ->
            !( (e.fromNodeId == nodeId && e.fromConnectorId == connectorId) ||
               (e.toNodeId == nodeId && e.toConnectorId == connectorId) )
        }

        val updatedGraph = GraphDefinition(
            id = graph.id,
            projectId = graph.projectId,
            nodes = graph.nodes,
            edges = edgesAfter
        )

        _graph.value = LoadState.Ready(updatedGraph)
        _loadedGraphWasModified.value = true
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
