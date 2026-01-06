package dev.tjpal

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.tjpal.di.listModules
import dev.tjpal.di.listProperties
import org.koin.core.context.startKoin

fun main() = application {
    startKoin {
        modules(listModules())
        properties(listProperties())
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "atrion",
        state = rememberWindowState(width = 1920.dp, height = 1080.dp)
    ) {
        App()
    }
}
