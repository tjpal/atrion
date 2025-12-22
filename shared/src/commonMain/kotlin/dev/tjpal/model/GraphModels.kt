package dev.tjpal.model

import kotlinx.serialization.Serializable

@Serializable
data class GraphDefinition(
    val id: String? = null,
    val projectId: String,
    val nodes: List<NodeInstance>,
    val edges: List<EdgeInstance>
)

@Serializable
data class NodeParameters(val values: Map<String, String> = emptyMap())

@Serializable
data class NodeInstance(
    val id: String,
    val definitionName: String,
    var parameters: NodeParameters = NodeParameters(),
    var position: Position
)

@Serializable
data class EdgeInstance(
    val fromNodeId: String,
    val toNodeId: String,
    val fromConnectorId: String,
    val toConnectorId: String
)

@Serializable
data class Position(val x: Int, val y: Int)

@Serializable
enum class NodeExecutionStatus { PENDING, RUNNING, FINISHED, ERROR }

@Serializable
data class ConnectorOutput(
    val connectorId: String,
    val payload: String
)

@Serializable
data class NodeExecutionState(
    val nodeID: String,
    val status: NodeExecutionStatus,
    val executionLog: String? = null,
    val errorSummary: String? = null,
    val connectorOutputs: List<ConnectorOutput> = emptyList()
)

@Serializable
data class GraphExecutionStatus(
    val id: String,
    val graphId: String,
    val nodeExecutionStates: List<NodeExecutionState>
)
