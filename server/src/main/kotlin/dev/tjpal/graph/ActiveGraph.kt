package dev.tjpal.graph

import dev.tjpal.graph.model.GraphExecutionStatus

class ActiveGraph(val id: String, val graphId: String) {
    fun getExecutionStatus(): GraphExecutionStatus = GraphExecutionStatus(
        id = id,
        graphId = graphId,
        nodeExecutionStates = emptyList()
    )
}