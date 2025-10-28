package dev.tjpal.graph

import dev.tjpal.model.GraphExecutionStatus
import io.mockk.every
import io.mockk.mockk
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ActiveGraphRepositoryTest {
    private lateinit var repo: ActiveGraphRepository

    private val factory = mockk<ActiveGraphFactory>()

    @BeforeTest
    fun setUp() {
        every { factory.create(any(), any()) } answers {
            val id: String = arg(0)
            val graphId: String = arg(1)

            mockk<ActiveGraph>(relaxed = true) {
                every { this@mockk.id } returns id
                every { this@mockk.graphId } returns graphId
                every { getExecutionStatus() } returns GraphExecutionStatus(id, graphId, emptyList())
            }
        }

        repo = ActiveGraphRepository(factory)
    }

    @Test
    fun `starting a new execution returns an id and it can be retrieved`() {
        val graphId = "proj-1"
        val id = repo.start(graphId)

        assertTrue(id.isNotBlank())

        val retrieved = repo.get(id)

        assertEquals(id, retrieved.id)
        assertEquals(graphId, retrieved.graphId)

        val exec: GraphExecutionStatus = retrieved.getExecutionStatus()

        assertEquals(id, exec.id)
        assertEquals(graphId, exec.graphId)
        assertEquals(emptyList<Any>(), exec.nodeExecutionStates)
    }

    @Test
    fun `retrieving non-existing execution throws`() {
        assertFailsWith<IllegalArgumentException> {
            repo.get("does-not-exist")
        }
    }

    @Test
    fun `deleting non-existing execution throws`() {
        assertFailsWith<IllegalArgumentException> {
            repo.delete("does-not-exist")
        }
    }

    @Test
    fun `deleting existing execution removes it`() {
        val graphId = "proj-2"
        val id = repo.start(graphId)

        // ensure exists and then delete
        val retrieved = repo.get(id)
        assertEquals(id, retrieved.id)

        repo.delete(id)

        // now get should fail
        assertFailsWith<IllegalArgumentException> {
            repo.get(id)
        }
    }
}
