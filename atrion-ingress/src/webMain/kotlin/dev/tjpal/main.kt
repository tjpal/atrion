package dev.tjpal

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import dev.tjpal.di.reviewsClientModule
import org.koin.core.context.startKoin

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    startKoin {
        modules(reviewsClientModule)
        properties(mapOf(
            "api.baseUrl" to "http://localhost:8081"
        ))
    }

    ComposeViewport {
        App()
    }
}
