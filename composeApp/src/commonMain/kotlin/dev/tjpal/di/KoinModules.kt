package dev.tjpal.di

import dev.tjpal.client.RESTApiClient
import dev.tjpal.graph.ActiveGraphService
import dev.tjpal.viewmodel.GraphEditorViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single<HttpClient> {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(
                    Json {
                        prettyPrint = true
                        ignoreUnknownKeys = true
                        encodeDefaults = true
                    }
                )
            }

            install(WebSockets)
        }
    }

    single {
        val baseUrl = getKoin().getProperty("api.baseUrl") ?: "http://localhost:8081"
        RESTApiClient(get(), baseUrl)
    }

    single {
        ActiveGraphService(get())
    }

    viewModel { GraphEditorViewModel(get(), get()) }
}
