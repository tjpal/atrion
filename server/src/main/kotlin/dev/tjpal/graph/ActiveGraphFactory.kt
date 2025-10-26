package dev.tjpal.graph

import dev.tjpal.nodes.NodeRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActiveGraphFactory @Inject constructor(
    private val graphDefinitionRepository: GraphDefinitionRepository,
    private val nodeRepository: NodeRepository,
) {
    fun create(executionId: String, graphId: String): ActiveGraph {
        val def = graphDefinitionRepository.get(graphId)
        return ActiveGraph(
            id = executionId,
            graphId = graphId,
            graphDefinition = def,
            nodeRepository = nodeRepository
        )
    }
}
