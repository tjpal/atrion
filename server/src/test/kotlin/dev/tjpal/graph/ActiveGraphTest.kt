package dev.tjpal.graph

import dev.tjpal.model.GraphDefinition
import dev.tjpal.model.NodeInstance
import dev.tjpal.model.NodeParameters
import dev.tjpal.nodes.NodeRepository
import io.mockk.every
import io.mockk.mockk
import kotlin.test.BeforeTest

class ActiveGraphTest {
    private lateinit var graphDefinition: GraphDefinition
    private lateinit var nodeInstanceDef: NodeInstance
    private lateinit var nodeRepository: NodeRepository

    @BeforeTest
    fun setUp() {
        graphDefinition = mockk(relaxed = true)

        nodeInstanceDef = mockk(relaxed = true)
        every { nodeInstanceDef.id } returns "n1"
        every { nodeInstanceDef.definitionName } returns "type1"
        every { nodeInstanceDef.parameters } returns NodeParameters()

        every { graphDefinition.nodes } returns listOf(nodeInstanceDef)
        every { graphDefinition.edges } returns emptyList()

        nodeRepository = mockk()
    }
}
