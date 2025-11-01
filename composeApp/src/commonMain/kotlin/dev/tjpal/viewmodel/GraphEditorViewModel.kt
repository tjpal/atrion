package dev.tjpal.viewmodel

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.tjpal.foundation.basics.text.Text
import dev.tjpal.foundation.structure.graphs.EdgeSpec
import dev.tjpal.foundation.structure.graphs.GraphState
import dev.tjpal.foundation.structure.graphs.NodeSpec
import dev.tjpal.foundation.structure.graphs.Connector
import dev.tjpal.foundation.structure.graphs.EdgeSide
import dev.tjpal.model.ConnectorDefinition
import dev.tjpal.model.EdgeInstance
import dev.tjpal.model.GraphDefinition
import dev.tjpal.model.NodeDefinition
import dev.tjpal.model.NodeInstance
import dev.tjpal.model.Position
import dev.tjpal.repository.GraphRepository
import dev.tjpal.repository.LoadState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Single UI state emitted by the ViewModel. Contains everything the UI needs to create the foundation GraphEditor
 * inputs.
 */
data class GraphEditorUiState(
    val loadState: LoadState<GraphDefinition> = LoadState.Loading,
    val nodes: List<NodeSpec> = emptyList(),
    val edges: List<EdgeSpec> = emptyList(),
    val graphState: GraphState = GraphState(nodes),
    val lastErrorMessage: String = ""
)

/**
 * Custom data associated with each node in the GraphEditor. Makes mapping between business logic state and UI state easier.
 */
data class NodeCustomData(
    val definitionName: String
)

class GraphEditorViewModel(
    private val repository: GraphRepository
) : ViewModel() {

    private val _uiState: MutableStateFlow<GraphEditorUiState> = MutableStateFlow(GraphEditorUiState())
    val uiState: StateFlow<GraphEditorUiState> = _uiState

    init {
        viewModelScope.launch {
            repository.refresh()

            repository.graph.collect { loadState ->
                when (loadState) {
                    is LoadState.Loading -> handleRepositoryLoading()
                    is LoadState.Ready -> handleRepositoryReady(loadState)
                    is LoadState.Error -> handleRepositoryError(loadState)
                }
            }
        }
    }

    private fun handleRepositoryLoading() {
        _uiState.value = GraphEditorUiState(
            loadState = LoadState.Loading,
            nodes = emptyList(),
            edges = emptyList(),
            graphState = GraphState(emptyList())
        )
    }

    private fun handleRepositoryReady(loadState: LoadState.Ready<GraphDefinition>) {
        val graph = loadState.data

        val nodes = mapNodes(graph.nodes)
        val edges = mapEdges(graph.edges)
        val graphState = GraphState(nodes)

        _uiState.value = GraphEditorUiState(
            loadState = LoadState.Ready(graph),
            nodes = nodes,
            edges = edges,
            graphState = graphState
        )
    }

    private fun handleRepositoryError(loadState: LoadState.Error) {
        _uiState.value = _uiState.value.copy(
            loadState = LoadState.Error(loadState.message, loadState.throwable),
            lastErrorMessage = loadState.message
        )
    }

    fun onConnect(fromNodeId: String, fromConnectorId: String, toNodeId: String, toConnectorId: String) { val newEdge = EdgeInstance(
            fromNodeId = fromNodeId,
            fromConnectorId = fromConnectorId,
            toNodeId = toNodeId,
            toConnectorId = toConnectorId
        )

        val uiEdges = _uiState.value.edges + EdgeSpec(fromNodeId, fromConnectorId, toNodeId, toConnectorId)
        _uiState.value = _uiState.value.copy(edges = uiEdges)
    }

    fun onDisconnect(nodeId: String, connectorId: String) {
        val uiFiltered = _uiState.value.edges.filter { e ->
            !((e.fromNodeId == nodeId && e.fromConnectorId == connectorId) ||
                (e.toNodeId == nodeId && e.toConnectorId == connectorId))
        }

        _uiState.value = _uiState.value.copy(edges = uiFiltered)
    }

    fun refresh() {
        viewModelScope.launch {
            repository.refresh()
        }
    }

    /**
     * Persist the current visual representation into a GraphDefinition and send to repository.
     */
    fun save() {
        val graphState = _uiState.value.graphState

        val nodes = _uiState.value.nodes.map { node ->
            val customData = node.associatedData as? NodeCustomData ?:
                throw IllegalStateException("Node ${node.id} missing NodeCustomData")

            val currentPosition = graphState.nodePositions[node.id]
                ?: throw IllegalStateException("Node ${node.id} missing position in GraphState")

            NodeInstance(
                id = node.id,
                definitionName = customData.definitionName,
                parametersJson = "{}",
                position = Position(
                    x = currentPosition.x.toInt(),
                    y = currentPosition.y.toInt()
                )
            )
        }

        val edges = _uiState.value.edges.map { edge ->
            EdgeInstance(
                fromNodeId = edge.fromNodeId,
                fromConnectorId = edge.fromConnectorId,
                toNodeId = edge.toNodeId,
                toConnectorId = edge.toConnectorId
            )
        }

        val newGraphDefinition = GraphDefinition(
            projectId = "",
            nodes = nodes,
            edges = edges
        )

        viewModelScope.launch {
            try {
                repository.save(newGraphDefinition)
            } catch (ex: Exception) {
                val message = "Error saving graph: ${ex.message ?: ex.toString()}"
                _uiState.value = _uiState.value.copy(lastErrorMessage = message)
            }
        }
    }

    /**
     * Maps business logic nodes to UI state nodes
     */
    private fun mapNodes(nodes: List<NodeInstance>): List<NodeSpec> = nodes.map { node ->
        val nodeDefinitions = repository.nodeDefinitions.value

        val nodeDefinition = when (nodeDefinitions) {
            is LoadState.Ready -> nodeDefinitions.data.firstOrNull { it.name == node.definitionName }
            else -> null
        }

        val connectors = if (nodeDefinition != null) {
            val list = mutableListOf<Connector>()

            fun build(listOfDefinitions: List<ConnectorDefinition>, side: EdgeSide) {
                for ((index, def) in listOfDefinitions.withIndex()) {
                    list.add(Connector(id = def.id, side = side, index = index))
                }
            }

            build(nodeDefinition.inputConnectors, EdgeSide.LEFT)
            build(nodeDefinition.outputConnectors, EdgeSide.RIGHT)
            build(nodeDefinition.toolConnectors, EdgeSide.BOTTOM)
            build(nodeDefinition.debugConnectors, EdgeSide.TOP)

            list
        } else {
            emptyList()
        }

        NodeSpec(
            id = node.id,
            initialPosition = Offset(node.position.x.toFloat(), node.position.y.toFloat()),
            widthMultiplier = 6,
            heightMultiplier = 6,
            connectors = connectors,
            associatedData = NodeCustomData(definitionName = node.definitionName),
            content = { _ -> Text(node.definitionName) },
        )
    }

    /**
     * Maps business logic edges to UI state edges
     */
    private fun mapEdges(edges: List<EdgeInstance>): List<EdgeSpec> = edges.map { edge ->
        EdgeSpec(
            fromNodeId = edge.fromNodeId,
            fromConnectorId = edge.fromConnectorId,
            toNodeId = edge.toNodeId,
            toConnectorId = edge.toConnectorId
        )
    }
}
