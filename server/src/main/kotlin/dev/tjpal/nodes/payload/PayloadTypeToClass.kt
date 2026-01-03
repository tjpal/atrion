package dev.tjpal.nodes.payload

import kotlinx.serialization.json.Json
import kotlin.reflect.KClass

object PayloadTypeToClass {
    fun toClass(type: String): KClass<out NodePayload>? {
        return when (type) {
            "RawPayload" -> RawPayload::class
            "ReviewRequestPayload" -> ReviewRequestPayload::class
            "ReviewDecisionPayload" -> ReviewDecisionPayload::class
            else -> null
        }
    }

    fun toType(type: KClass<out NodePayload>): String {
        return when (type) {
            RawPayload::class -> "RawPayload"
            ReviewRequestPayload::class -> "ReviewRequestPayload"
            ReviewDecisionPayload::class -> "ReviewDecisionPayload"
            else -> "UnknownPayload"
        }
    }

    fun stringToPayload(type: KClass<out NodePayload>?, text: String, json: Json = Json { prettyPrint = true }): NodePayload {
        return when (type) {
            RawPayload::class -> RawPayload.serializer().let { serializer ->
                json.decodeFromString(serializer, text)
            }
            ReviewRequestPayload::class -> ReviewRequestPayload.serializer().let { serializer ->
                json.decodeFromString(serializer, text)
            }
            ReviewDecisionPayload::class -> ReviewDecisionPayload.serializer().let { serializer ->
                json.decodeFromString(serializer, text)
            }
            else -> RawPayload(text) // Fallback. If no type is specified, by definition RawPayload is used
        }
    }
}
