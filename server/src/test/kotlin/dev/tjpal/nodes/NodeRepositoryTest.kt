package dev.tjpal.nodes

import dev.tjpal.model.NodeDefinition
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class NodeRepositoryTest {
    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @AfterTest
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `Definitions are returned correctly for a node factory`() {
        val nodeDef = mockk<NodeDefinition>()
        val factory = mockk<NodeFactory>()
        every { factory.definition() } returns nodeDef

        val repo = NodeRepository(mapOf("my-type" to factory))

        val definitions = repo.getAllDefinitions()

        assertEquals(listOf(nodeDef), definitions)
    }

    @Test
    fun `An exception is thrown when the type is unknown`() {
        val repo = NodeRepository(emptyMap())

        val ex = assertFailsWith<IllegalArgumentException> {
            repo.createNodeInstance("unknown-type", "{}")
        }

        assertTrue(ex.message?.contains("No factory registered for node type") == true)
    }

    @Test
    fun `A valid node instance is returned when createNodeInstance is called with a valid type`() {
        val node = mockk<Node>()
        val factory = mockk<NodeFactory>()
        val json = """{"foo":"bar"}"""
        every { factory.createNode(json) } returns node

        val repo = NodeRepository(mapOf("my-type" to factory))

        val result = repo.createNodeInstance("my-type", json)

        assertSame(node, result)
        verify(exactly = 1) { factory.createNode(json) }
    }
}