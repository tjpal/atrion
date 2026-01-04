package dev.tjpal

import androidx.compose.runtime.Composable
import dev.tjpal.composition.foundation.themes.cascade.Cascade
import dev.tjpal.ui.ReviewPanel

@Composable
fun App() {
    Cascade {
        ReviewPanel()
    }
}
