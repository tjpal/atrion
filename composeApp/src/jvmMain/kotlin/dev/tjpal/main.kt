package dev.tjpal

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
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
    ) {
        App()
    }
}
