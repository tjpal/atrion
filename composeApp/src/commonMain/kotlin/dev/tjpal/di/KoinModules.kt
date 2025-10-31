package dev.tjpal.di

import org.koin.dsl.module
import io.ktor.client.*
import dev.tjpal.client.RESTApiClient
import dev.tjpal.viewmodel.GraphEditorViewModel
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.viewModel

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
        }
    }

    single {
        val baseUrl = getKoin().getProperty<String>("api.baseUrl") ?: "http://localhost:8081"
        RESTApiClient(get(), baseUrl)
    }

    // Provide the GraphEditorViewModel to Compose via Koin
    viewModel { GraphEditorViewModel(get()) }
}
