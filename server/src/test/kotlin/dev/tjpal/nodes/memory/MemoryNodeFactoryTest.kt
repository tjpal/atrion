package dev.tjpal.nodes.memory

import dev.tjpal.ai.tools.ToolRegistry
import dev.tjpal.tools.memory.MemoryStore
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test

class MemoryNodeFactoryTest {
    @Test
    fun `init registers tool factory with tool registry`() {
        val mockToolRegistry = mockk<ToolRegistry>(relaxed = true)
        val mockMemoryStore = mockk<MemoryStore>()

        val factory = MemoryNodeFactory(mockToolRegistry, mockMemoryStore)
        verify(exactly = 1) { mockToolRegistry.registerFactory("MemoryTool", any()) }
    }
}
