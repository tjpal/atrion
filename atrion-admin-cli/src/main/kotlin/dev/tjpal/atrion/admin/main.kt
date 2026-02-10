package dev.tjpal.atrion.admin

import dev.tjpal.atrion.admin.config.Config
import dev.tjpal.atrion.admin.config.getConfig
import dev.tjpal.client.RESTApiClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.system.exitProcess

private sealed interface Command {
    data object ListExecutions : Command
    data class CreateGraphInstance(val graphId: String) : Command
    data class DeleteGraphInstance(val graphInstanceId: String) : Command
    data object ListGraphIds : Command
}

fun main(args: Array<String>) = runBlocking {
    val command = parseCommand(args) ?: run {
        printUsage()
        exitProcess(1)
    }

    val config = getConfig()
    val baseUrl = buildBaseUrl(config)
    val webSocketBaseUrl = toWebSocketBaseUrl(baseUrl)

    val client = HttpClient(CIO) {
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

    try {
        val api = RESTApiClient(client, baseUrl, webSocketBaseUrl)

        when (command) {
            is Command.ListExecutions -> listExecutions(api)
            is Command.CreateGraphInstance -> createGraphInstance(api, command.graphId)
            is Command.DeleteGraphInstance -> deleteGraphInstance(api, command.graphInstanceId)
            is Command.ListGraphIds -> listGraphIds(api)
        }
    } catch (exception: Exception) {
        println("Operation failed: ${exception.message ?: exception}")
        exitProcess(1)
    } finally {
        client.close()
    }
}

private fun parseCommand(args: Array<String>): Command? {
    if (args.size < 2) return null

    return when (args[0]) {
        "executions" -> {
            when (args[1]) {
                "list" -> if (args.size == 2) Command.ListExecutions else null
                "create" -> if (args.size == 3) Command.CreateGraphInstance(args[2]) else null
                "delete" -> if (args.size == 3) Command.DeleteGraphInstance(args[2]) else null
                else -> null
            }
        }
        "graphs" -> {
            when (args[1]) {
                "list" -> if (args.size == 2) Command.ListGraphIds else null
                else -> null
            }
        }
        else -> null
    }
}

private fun printUsage() {
    println("Atrion Admin CLI")
    println("Usage:")
    println("  atrion-admin-cli executions list")
    println("  atrion-admin-cli executions create <graphId>")
    println("  atrion-admin-cli executions delete <graphInstanceId>")
    println("  atrion-admin-cli graphs list")
}

private suspend fun listExecutions(api: RESTApiClient) {
    val executions = api.listExecutions()

    if (executions.isEmpty()) {
        println("No executions found.")
        return
    }

    executions.forEach {
        println("${it.id}\tgraphId=${it.graphId}\tnodes=${it.nodeExecutionStates.size}")
    }
}

private suspend fun createGraphInstance(api: RESTApiClient, graphId: String) {
    val graphInstanceId = api.createGraphInstance(graphId)
    println("Created graph instance: $graphInstanceId")
}

private suspend fun deleteGraphInstance(api: RESTApiClient, graphInstanceId: String) {
    api.deleteGraphInstance(graphInstanceId)
    println("Deleted graph instance: $graphInstanceId")
}

private suspend fun listGraphIds(api: RESTApiClient) {
    val graphIds = api.getGraphs()
        .mapNotNull { it.id?.takeIf { id -> id.isNotBlank() } }

    if (graphIds.isEmpty()) {
        println("No graph IDs found.")
        return
    }

    graphIds.forEach { println(it) }
}

private fun buildBaseUrl(config: Config): String {
    if (config.udsPath.isNotBlank()) {
        throw IllegalArgumentException("UDS endpoints are not supported yet")
    }

    val rawHost = config.serverUrl.trim().removeSuffix("/")

    if (rawHost.isBlank()) {
        throw IllegalArgumentException("serverUrl must not be empty")
    }

    return if (rawHost.startsWith("http://") || rawHost.startsWith("https://")) {
        if (rawHost.substringAfter("://").contains(":")) rawHost else "$rawHost:${config.serverPort}"
    } else {
        "https://$rawHost:${config.serverPort}"
    }
}

private fun toWebSocketBaseUrl(baseUrl: String): String {
    return when {
        baseUrl.startsWith("https://") -> "wss://${baseUrl.removePrefix("https://")}"
        baseUrl.startsWith("http://") -> "ws://${baseUrl.removePrefix("http://")}"
        else -> throw IllegalArgumentException("Unsupported URL scheme in base URL: $baseUrl")
    }
}
