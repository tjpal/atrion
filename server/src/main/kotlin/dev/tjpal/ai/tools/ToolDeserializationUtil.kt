package dev.tjpal.ai.tools

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.serializer
import kotlin.reflect.KClass
import kotlin.reflect.full.createType

object ToolDeserializationUtil {
    private val json = Json { ignoreUnknownKeys = true }

    fun deserialize(toolClass: KClass<out Tool>, element: JsonElement): Tool {
        try {
            val kotlinType = toolClass.createType()
            val typeSerializer = serializer(kotlinType) as KSerializer<*>

            return json.decodeFromJsonElement(typeSerializer, element) as Tool
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to deserialize arguments for tool ${toolClass.qualifiedName}", e)
        }
    }
}
