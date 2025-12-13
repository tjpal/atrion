package dev.tjpal.utilities

import java.io.InputStream
import java.util.*

class ImageResourceEncoder {
    fun encodeResourceToBase64(resourcePath: String, classLoader: ClassLoader = Thread.currentThread().contextClassLoader): String {
        val normalized = resourcePath.removePrefix("/")

        val stream = classLoader.getResourceAsStream(normalized)
            ?: throw IllegalArgumentException("Resource not found: $resourcePath")

        stream.use {
            return encodeInputStreamToBase64(it)
        }
    }

    private fun encodeInputStreamToBase64(input: InputStream): String {
        val bytes = input.readBytes()
        return Base64.getEncoder().encodeToString(bytes)
    }
}
