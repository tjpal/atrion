package dev.tjpal.model

import androidx.compose.ui.graphics.ImageBitmap

data class ExtendedNodeDefinition(
    val definition: NodeDefinition,
    val iconImage: ImageBitmap?
)
