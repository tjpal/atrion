package dev.tjpal.di

import org.koin.core.module.Module

fun listModules(): List<Module> = listOf(
    appModule,
    repositoryModule
)

fun listProperties(): Map<String, Any> = mapOf(
    "api.baseUrl" to "http://localhost:8081",
    "api.wsUrl" to "ws://localhost:8081"
)