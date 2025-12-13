package dev.tjpal.util

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

actual suspend fun decodePngBase64ToImageBitmap(base64: String): ImageBitmap? {
    return try {
        withContext(Dispatchers.IO) {
            val bytes = Base64.getDecoder().decode(base64)
            val skImage = org.jetbrains.skia.Image.makeFromEncoded(bytes)

            skImage.toComposeImageBitmap()
        }
    } catch (e: Exception) {
        println("Failed to decode image from base64: ${e.message}")
        null
    }
}
