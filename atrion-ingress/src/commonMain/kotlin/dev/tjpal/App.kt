package dev.tjpal

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.tjpal.composition.foundation.basics.text.Text
import dev.tjpal.composition.foundation.themes.cascade.Cascade
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    Cascade {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text("First item")
            Text("Second item")
        }
    }
}