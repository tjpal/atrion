package dev.tjpal.nodes.payload

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ReviewDecisionPayload(
    val originalReviewId: String,
    val status: String,
    val reviewer: String? = null,
    val comment: String? = null
) : NodePayload {
    override fun asString(): String {
        val json = Json { prettyPrint = true; encodeDefaults = true }
        return json.encodeToString(this)
    }
}
