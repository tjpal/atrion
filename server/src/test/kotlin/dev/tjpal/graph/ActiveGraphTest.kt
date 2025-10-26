package dev.tjpal.graph

import dev.tjpal.graph.model.GraphDefinition
import dev.tjpal.graph.model.NodeInstance
import dev.tjpal.nodes.Node
import dev.tjpal.nodes.NodeRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

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
        every { nodeInstanceDef.parametersJson } returns "{}"

        every { graphDefinition.nodes } returns listOf(nodeInstanceDef)
        every { graphDefinition.edges } returns emptyList()

        nodeRepository = mockk()
    }

    @Test
    fun `activate should call onActivate on all nodes`() {
        val node = mockk<Node>(relaxed = true)

        every { nodeRepository.createNodeInstance("type1", "{}") } returns node

        val activeGraph = ActiveGraph(id = "exec-activate", graphId = "g1", graphDefinition = graphDefinition, nodeRepository = nodeRepository)

        activeGraph.activate()

        // verify node instance created and onActivate called
        verify(exactly = 1) { nodeRepository.createNodeInstance("type1", "{}") }
        verify(exactly = 1) { node.onActivate(any()) }
    }

    @Test
    fun `onInputEvent schedules node and node receives onEvent`() {
        val latch = CountDownLatch(1)

        val node = mockk<Node>(relaxed = true)

        // Mock the suspending onEvent to count down the latch when invoked
        coEvery { node.onEvent(any(), any()) } coAnswers {
            latch.countDown()
            Unit
        }

        every { nodeRepository.createNodeInstance("type1", "{}") } returns node

        val activeGraph = ActiveGraph(id = "exec-input", graphId = "g1", graphDefinition = graphDefinition, nodeRepository = nodeRepository)

        // activate to create mailboxes
        activeGraph.activate()

        // trigger input event
        activeGraph.onInputEvent("n1", "hello")

        // wait for the node.onEvent to be called
        val completed = latch.await(2, TimeUnit.SECONDS)

        // Ensure the onEvent was invoked within the timeout
        assertTrue(completed)

        // verify createNodeInstance was used to construct the node instance
        verify(atLeast = 1) { nodeRepository.createNodeInstance("type1", "{}") }
        coVerify(atLeast = 1) {
            node.onEvent(
                match { it.payload == "hello" && it.nodeId == "n1" && it.executionId == "exec-input" },
                any()
            )
        }
    }

    @Test
    fun `stop should call onStop on all nodes`() = runBlocking {
        val node = mockk<Node>(relaxed = true)

        every { nodeRepository.createNodeInstance("type1", "{}") } returns node

        val activeGraph = ActiveGraph(id = "exec-stop", graphId = "g1", graphDefinition = graphDefinition, nodeRepository = nodeRepository)

        activeGraph.activate()
        activeGraph.stop()

        // verify node instance created for stop call and onStop invoked
        verify(atLeast = 1) { nodeRepository.createNodeInstance("type1", "{}") }
        verify(exactly = 1) { node.onStop(any()) }
    }
}
