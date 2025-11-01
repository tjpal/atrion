package dev.tjpal.graph

import dev.tjpal.graph.hooks.RestInputRegistry
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RestInputRegistryTest {
    private lateinit var registry: RestInputRegistry

    @BeforeTest
    fun setUp() {
        registry = RestInputRegistry()
    }

    @Test
    fun `handleIncoming forwards to registered ActiveGraph and returns true`() {
        val graph = mockk<ActiveGraph>(relaxed = true)
        val graphInstanceId = "graph-inst-1"
        val nodeId = "node-1"
        val payload = "payload"
        val execId = "exec-1"

        registry.register(graphInstanceId, nodeId, graph)

        val result = registry.handleIncoming(graphInstanceId, nodeId, payload, execId)

        assertTrue(result)
        verify(exactly = 1) { graph.onInputEvent(nodeId, payload, execId) }
    }

    @Test
    fun `handleIncoming returns false when no registration exists`() {
        val result = registry.handleIncoming("unknown-graph", "unknown-node", "p", "e1")

        assertFalse(result)
    }

    @Test
    fun `unregister removes mapping so handleIncoming returns false`() {
        val graph = mockk<ActiveGraph>(relaxed = true)
        val graphInstanceId = "graph-inst-2"
        val nodeId = "node-2"

        registry.register(graphInstanceId, nodeId, graph)

        // ensure forwarding works first
        assertTrue(registry.handleIncoming(graphInstanceId, nodeId, "x", "exec-2"))

        registry.unregister(graphInstanceId, nodeId)

        val result = registry.handleIncoming(graphInstanceId, nodeId, "x", "exec-2")
        assertFalse(result)
    }

    @Test
    fun `handleIncoming returns false when ActiveGraph onInputEvent throws`() {
        val graph = mockk<ActiveGraph>(relaxed = false)
        val graphInstanceId = "graph-inst-3"
        val nodeId = "node-3"

        every { graph.onInputEvent(nodeId, any(), any()) } throws RuntimeException("boom")

        registry.register(graphInstanceId, nodeId, graph)

        val result = registry.handleIncoming(graphInstanceId, nodeId, "payload", "exec-3")

        assertFalse(result)
        verify(exactly = 1) { graph.onInputEvent(nodeId, "payload", "exec-3") }
    }
}
