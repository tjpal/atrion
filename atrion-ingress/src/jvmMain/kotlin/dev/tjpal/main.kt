package dev.tjpal

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.tjpal.di.reviewsClientModule
import org.koin.core.context.startKoin

fun main() = application {
    startKoin {
        modules(reviewsClientModule)
        properties(mapOf(
            "api.baseUrl" to "http://localhost:8081"
        ))
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "atrion-ingress",
    ) {
        App()
    }
}
