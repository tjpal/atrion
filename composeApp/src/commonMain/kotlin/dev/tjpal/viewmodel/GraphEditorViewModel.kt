package dev.tjpal.viewmodel

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.tjpal.composition.foundation.structure.graphs.Connector
import dev.tjpal.composition.foundation.structure.graphs.EdgeSide
import dev.tjpal.composition.foundation.structure.graphs.EdgeSpec
import dev.tjpal.composition.foundation.structure.graphs.GraphState
import dev.tjpal.composition.foundation.structure.graphs.NodeSpec
import dev.tjpal.graph.ActiveGraphService
import dev.tjpal.graph.GraphRepository
import dev.tjpal.graph.LoadState
import dev.tjpal.model.ConnectorDefinition
import dev.tjpal.model.EdgeInstance
import dev.tjpal.model.ExtendedNodeDefinition
import dev.tjpal.model.GraphDefinition
import dev.tjpal.model.NodeDefinition
import dev.tjpal.model.NodeInstance
import dev.tjpal.model.NodeParameters
import dev.tjpal.model.NodeType
import dev.tjpal.model.Position
import dev.tjpal.model.StatusEntry
import dev.tjpal.ui.NodeContent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Single UI state emitted by the ViewModel. Contains everything the UI needs to create the foundation GraphEditor
 * inputs.
 */
data class GraphEditorUiState(
    val loadState: LoadState<GraphDefinition> = LoadState.Loading,
    val nodes: List<NodeSpec> = emptyList(),
    val edges: List<EdgeSpec> = emptyList(),
    val graphState: GraphState = GraphState(nodes),
    val lastErrorMessage: String = "",
    val isModified: Boolean = false
)

/**
 * Associated with the graphical representation of each node. Acts as a shortcut to access business logic data.
 */
data class NodeCustomData(
    val node: NodeInstance,
    val definition: NodeDefinition
)

class GraphEditorViewModel(
    private val repository: GraphRepository,
    private val activeGraphService: ActiveGraphService
) : ViewModel() {

    private val _uiState: MutableStateFlow<GraphEditorUiState> = MutableStateFlow(GraphEditorUiState())
    val uiState: StateFlow<GraphEditorUiState> = _uiState

    val nodeDefinitions = repository.nodeDefinitions
    val isModified = repository.loadedGraphWasModified

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
        // Preserve the isModified flag when switching to loading state
        _uiState.value = GraphEditorUiState(
            loadState = LoadState.Loading,
            nodes = emptyList(),
            edges = emptyList(),
            graphState = GraphState(emptyList()),
            isModified = _uiState.value.isModified
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
            graphState = graphState,
            isModified = _uiState.value.isModified
        )
    }

    private fun handleRepositoryError(loadState: LoadState.Error) {
        _uiState.value = _uiState.value.copy(
            loadState = LoadState.Error(loadState.message, loadState.throwable),
            lastErrorMessage = loadState.message
        )
    }

    fun onConnect(fromNodeId: String, fromConnectorId: String, toNodeId: String, toConnectorId: String) {
        repository.addEdge(fromNodeId, fromConnectorId, toNodeId, toConnectorId)
    }

    fun onDisconnect(nodeId: String, connectorId: String) {
        repository.removeEdge(nodeId, connectorId)
    }

    fun switchToEditMode() {
        if(activeGraphService.isGraphActive()) {
            if(activeGraphService.inTransition.value.not()) {
                viewModelScope.launch {
                    activeGraphService.stopActiveGraph()
                }
            }
        }
    }

    fun switchToExecutionMode() {
        if(activeGraphService.isGraphActive().not()) {
            if(activeGraphService.inTransition.value.not()) {
                viewModelScope.launch {
                    val currentGraphState = repository.graph.value
                    if(currentGraphState is LoadState.Ready) {
                        val graphId = currentGraphState.data.id ?: return@launch
                        activeGraphService.startActiveGraph(graphId)
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    fun insertNode(nodeDefinition: ExtendedNodeDefinition) {
        viewModelScope.launch {
            val nodeInstance = NodeInstance(
                id = Uuid.random().toString(),
                definitionName = nodeDefinition.definition.name,
                parameters = NodeParameters(),
                position = Position(x = 0, y = 0)
            )

            repository.addNode(nodeInstance)
        }
    }

    fun deleteNode(nodeId: String) {
        viewModelScope.launch {
            repository.removeNode(nodeId)
        }
    }

    fun setNodePosition(nodeSpec: NodeSpec, position: Offset) {
        repository.setNodePosition(nodeSpec.id, position.x.toInt(), position.y.toInt())
    }

    fun setNodeParameters(nodeId: String, parameters: NodeParameters) {
        viewModelScope.launch {
            repository.setNodeParameters(nodeId, parameters)
        }
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
                definitionName = customData.definition.name,
                parameters = customData.node.parameters,
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
                // Clear modified flag on successful save
                _uiState.value = _uiState.value.copy(isModified = false)
            } catch (ex: Exception) {
                val message = "Error saving graph: ${ex.message ?: ex.toString()}"
                _uiState.value = _uiState.value.copy(lastErrorMessage = message)
            }
        }
    }

    fun observeNodeStatus(nodeId: String): StateFlow<StatusEntry?> = activeGraphService.observeNodeLastStatus(nodeId)

    /**
     * Maps business logic nodes to UI state nodes
     */
    private fun mapNodes(nodes: List<NodeInstance>): List<NodeSpec> = nodes.map { node ->
        val nodeDefinition = when (val nodeDefinitions = repository.nodeDefinitions.value) {
            is LoadState.Ready -> nodeDefinitions.data.firstOrNull { it.definition.name == node.definitionName }
            else -> null
        }

        if(nodeDefinition == null) {
            return@mapNodes emptyList()
        }

        val connectors = buildConnectorsFromDefinition(nodeDefinition)

        NodeSpec(
            id = node.id,
            initialPosition = Offset(node.position.x.toFloat(), node.position.y.toFloat()),
            widthMultiplier = 6,
            heightMultiplier = 6,
            connectors = connectors,
            associatedData = NodeCustomData(node, nodeDefinition.definition),
            content = { _ ->
                NodeContent(
                    node = node,
                    definition = nodeDefinition.definition,
                    activeGraphService = activeGraphService,
                    graphRepository = repository,
                    viewModel = this,
                )
            },
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

    private fun buildConnectorsFromDefinition(nodeDefinition: ExtendedNodeDefinition): List<Connector> {
        val list = mutableListOf<Connector>()

        fun build(listOfDefinitions: List<ConnectorDefinition>, side: EdgeSide) {
            for ((index, def) in listOfDefinitions.withIndex()) {
                list.add(Connector(id = def.id, side = side, index = index))
            }
        }

        val toolConnectorSide = if(nodeDefinition.definition.type == NodeType.TOOL) EdgeSide.TOP else EdgeSide.BOTTOM
        val monitorConnectorSide = if(nodeDefinition.definition.type == NodeType.MONITOR) EdgeSide.BOTTOM else EdgeSide.TOP

        build(nodeDefinition.definition.inputConnectors, EdgeSide.LEFT)
        build(nodeDefinition.definition.outputConnectors, EdgeSide.RIGHT)
        build(nodeDefinition.definition.toolConnectors, toolConnectorSide)
        build(nodeDefinition.definition.debugConnectors, monitorConnectorSide)

        return list
    }
}
