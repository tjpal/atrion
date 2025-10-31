package dev.tjpal

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import dev.tjpal.di.listModules
import dev.tjpal.di.listProperties
import org.koin.core.context.startKoin

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    startKoin {
        modules(listModules())
        properties(listProperties())
    }

    ComposeViewport {
        App()
    }
}
