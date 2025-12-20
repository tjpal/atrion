package dev.tjpal.model

import kotlinx.serialization.Serializable

@Serializable
enum class NodeType { INPUT, OUTPUT, PROCESSOR, TOOL, MONITOR }

@Serializable
enum class ConnectorSchema { TEXT, AUDIO, JSON }

@Serializable
data class ConnectorDefinition(
    val id: String,
    val label: String,
    val schema: ConnectorSchema,
    val minConnections: Int = 0,
    val maxConnections: Int = Int.MAX_VALUE,
    val optional: Boolean = false
)

@Serializable
enum class ParameterType { BOOLEAN, INT, FLOAT, STRING , LONG_TEXT}

@Serializable
data class ParameterDefinition(
    val name: String,
    val type: ParameterType,
    val required: Boolean = false,
    val default: String? = null,
    val description: String? = null,
    val minValue: String? = null,
    val maxValue: String? = null
)

@Serializable
data class NodeDefinition(
    val name: String,
    val type: NodeType,
    val category: String,
    val description: String,
    val icon: String,
    val inputConnectors: List<ConnectorDefinition> = emptyList(),
    val outputConnectors: List<ConnectorDefinition> = emptyList(),
    val toolConnectors: List<ConnectorDefinition> = emptyList(),
    val debugConnectors: List<ConnectorDefinition> = emptyList(),
    val parameters: List<ParameterDefinition> = emptyList()
)
