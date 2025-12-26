package dev.tjpal.tools

import dev.tjpal.ai.tools.ToolFactory
import dev.tjpal.ai.tools.Tool
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlin.reflect.KClass

/**
 * Simple factory for HelloWorldTool. It expects that either nodeParameters or arguments (JSON) contain a
 * "name" field (string). If not present, it falls back to empty name.
 */
class HelloWorldToolFactory(private val json: Json = Json { ignoreUnknownKeys = true }) : ToolFactory {
    override val toolClass: KClass<out Tool> = HelloWorldTool::class

    override fun create(arguments: JsonElement?, nodeParameters: JsonElement?): Tool {
        // Prefer nodeParameters over arguments as defaults; tools can implement any merging strategy.
        val source = nodeParameters ?: arguments
        val name = try {
            source?.jsonObject?.get("name")?.let { elem ->
                // Attempt to extract a string value; keep it simple and strip quotes produced by toString if necessary
                val raw = elem.toString()
                if (raw.length >= 2 && raw.startsWith('"') && raw.endsWith('"')) raw.substring(1, raw.length - 1) else raw
            } ?: ""
        } catch (_: Exception) {
            ""
        }

        return HelloWorldTool(name = name)
    }
}
