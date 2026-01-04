package dev.tjpal.nodes.template

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TemplateEngineTest {
    @Test
    fun `variable substitution success`() {
        runBlocking {
            val engine = TemplateEngine()
            val template = "Hello {{name}}, input: {{input}}"
            val result = engine.render(
                template = template,
                variables = mapOf("name" to "Alice", "input" to "hi"),
                llmToolInvoker = { _, _, _ -> "" }
            )
            assertEquals("Hello Alice, input: hi", result)
        }
    }

    @Test
    fun `missing variable throws`() {
        runBlocking {
            val engine = TemplateEngine()
            val template = "Hello {{name}}"
            assertFailsWith<IllegalArgumentException> {
                engine.render(
                    template = template,
                    variables = mapOf(),
                    llmToolInvoker = { _, _, _ -> "" }
                )
            }
        }
    }

    @Test
    fun `named args parsing and llm invoker called`() {
        runBlocking {
            val engine = TemplateEngine()
            val template = "Call: {{doit(s = \"a string\", i = 42, f = 3.14, flag = true, name = \"bob\")}}"

            val invoker = mockk<LlmInvoker>()
            coEvery { invoker.invoke(any(), any(), any()) } returns "OK"

            val llmToolInvoker: suspend (String, List<Any?>, Map<String, Any?>) -> String = { name, pos, named ->
                invoker.invoke(name, pos, named)
            }

            val result = engine.render(
                template = template,
                variables = mapOf(),
                llmToolInvoker = llmToolInvoker
            )

            assertEquals("Call: OK", result)

            coVerify { invoker.invoke(
                match { it == "doit" },
                match { it.isEmpty() },
                match {
                    it.size == 5 && it["s"] == "a string" && (it["i"] as Int) == 42 && (it["f"] as Double) == 3.14 && it["flag"] == true && it["name"] == "bob"
                }
            ) }
        }
    }

    @Test
    fun `static tool invocation with named args`() {
        runBlocking {
            val engine = TemplateEngine()
            val template = "Sum: {{sum(a = 1, b = 2, c = 3)}}"

            val staticTools = mapOf<String, suspend (List<Any?>, Map<String, Any?>, Map<String, Any?>) -> String>(
                "sum" to { pos, named, vars ->
                    val total = named.values.filterIsInstance<Number>().sumOf { it.toDouble() }
                    if (total % 1.0 == 0.0) total.toInt().toString() else total.toString()
                }
            )

            val result = engine.render(
                template = template,
                variables = mapOf(),
                llmToolInvoker = { _, _, _ -> "" },
                staticTools = staticTools
            )

            assertEquals("Sum: 6", result)
        }
    }

    @Test
    fun `unclosed placeholder throws`() {
        runBlocking {
            val engine = TemplateEngine()
            val template = "Hello {{name"
            assertFailsWith<IllegalArgumentException> {
                engine.render(
                    template = template,
                    variables = mapOf("name" to "Alice"),
                    llmToolInvoker = { _, _, _ -> "" }
                )
            }
        }
    }

    // Helper interface to allow mocking a suspend invoker with mockk
    interface LlmInvoker {
        suspend fun invoke(toolName: String, positional: List<Any?>, named: Map<String, Any?>): String
    }
}
