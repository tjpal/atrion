package dev.tjpal.nodes.payload

import kotlin.reflect.KClass

object PayloadTypeToClass {
    fun toClass(type: String): KClass<out NodePayload>? {
        return when (type) {
            "RawPayload" -> RawPayload::class
            else -> null
        }
    }

    fun toType(payload: NodePayload): String {
        return when (payload) {
            is RawPayload -> "RawPayload"
            else -> "Unknown"
        }
    }
}