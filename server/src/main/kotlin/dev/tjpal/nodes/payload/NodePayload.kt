package dev.tjpal.nodes.payload

import kotlinx.serialization.Serializable

/**
 * Supertype for node payloads. The implementation must overwride asString ans either provide
 * * a raw string output
 * * a JSON string so it can be passed to a LLM (structured input)
 */
@Serializable
sealed interface NodePayload {
    fun asString(): String

}

/**
 * Fundamental payload type. If a node does not specify an input payload or accepted payloads,
 * this type will be used. Must be supported by all nodes.
 */
@Serializable
data class RawPayload(val text: String) : NodePayload {
    override fun asString(): String = text
}
