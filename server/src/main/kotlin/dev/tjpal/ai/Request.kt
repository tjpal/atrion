package dev.tjpal.ai

import kotlin.reflect.KClass

data class Request(
    val input: String,
    val instructions: String,
    val model: String = "gpt-5-nano",
    val responseType: KClass<*>? = null,
    val temperature: Double = 1.0,
    val topP: Double? = null
)