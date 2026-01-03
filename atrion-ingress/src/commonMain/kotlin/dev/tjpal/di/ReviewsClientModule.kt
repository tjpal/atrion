package dev.tjpal.di

import dev.tjpal.client.ReviewsApiClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val reviewsClientModule = module {
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
        ReviewsApiClient(get(), baseUrl)
    }
}
