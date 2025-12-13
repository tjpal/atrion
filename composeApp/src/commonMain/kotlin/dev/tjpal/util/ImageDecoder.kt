package dev.tjpal.util

import androidx.compose.ui.graphics.ImageBitmap

expect suspend fun decodePngBase64ToImageBitmap(base64: String): ImageBitmap?
