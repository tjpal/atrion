package dev.tjpal.graph

import dev.tjpal.graph.model.GraphDefinition
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class GraphDefinitionRepositoryTest {
    private lateinit var repo: GraphDefinitionRepository

    @BeforeTest
    fun setUp() {
        repo = GraphDefinitionRepository()
    }

    @Test
    fun `adding a new graph returns an id and it can be retrieved`() {
        val graph = GraphDefinition(projectId = "proj-1", nodes = emptyList(), edges = emptyList())
        val id = repo.add(graph)

        assertTrue(id.isNotBlank())

        val retrieved = repo.get(id)
        assertEquals(id, retrieved.id)
        assertEquals(graph.projectId, retrieved.projectId)
        assertEquals(graph.nodes, retrieved.nodes)
        assertEquals(graph.edges, retrieved.edges)
    }

    @Test
    fun `retrieving non-existing graph throws`() {
        assertFailsWith<IllegalArgumentException> {
            repo.get("does-not-exist")
        }
    }

    @Test
    fun `deleting non-existing graph throws`() {
        assertFailsWith<IllegalArgumentException> {
            repo.delete("does-not-exist")
        }
    }

    @Test
    fun `deleting existing graph removes it`() {
        val graph = GraphDefinition(projectId = "proj-2", nodes = emptyList(), edges = emptyList())
        val id = repo.add(graph)

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
