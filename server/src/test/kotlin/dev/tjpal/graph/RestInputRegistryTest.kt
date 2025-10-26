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
        val executionId = "exec-1"
        val nodeId = "node-1"
        val payload = "payload"

        registry.register(executionId, nodeId, graph)

        val result = registry.handleIncoming(executionId, nodeId, payload)

        assertTrue(result)
        verify(exactly = 1) { graph.onInputEvent(nodeId, payload) }
    }

    @Test
    fun `handleIncoming returns false when no registration exists`() {
        val result = registry.handleIncoming("unknown-exec", "unknown-node", "p")

        assertFalse(result)
    }

    @Test
    fun `unregister removes mapping so handleIncoming returns false`() {
        val graph = mockk<ActiveGraph>(relaxed = true)
        val executionId = "exec-2"
        val nodeId = "node-2"

        registry.register(executionId, nodeId, graph)

        // ensure forwarding works first
        assertTrue(registry.handleIncoming(executionId, nodeId, "x"))

        registry.unregister(executionId, nodeId)

        val result = registry.handleIncoming(executionId, nodeId, "x")
        assertFalse(result)
    }

    @Test
    fun `handleIncoming returns false when ActiveGraph onInputEvent throws`() {
        val graph = mockk<ActiveGraph>(relaxed = false)
        val executionId = "exec-3"
        val nodeId = "node-3"

        every { graph.onInputEvent(nodeId, any()) } throws RuntimeException("boom")

        registry.register(executionId, nodeId, graph)

        val result = registry.handleIncoming(executionId, nodeId, "payload")

        assertFalse(result)
        verify(exactly = 1) { graph.onInputEvent(nodeId, "payload") }
    }
}
