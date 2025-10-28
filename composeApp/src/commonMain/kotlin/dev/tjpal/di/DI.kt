package dev.tjpal.di

import dev.tjpal.client.RESTApiClient
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object AppDI : KoinComponent {
    val restApiClient: RESTApiClient by inject()
}
