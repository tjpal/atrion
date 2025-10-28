package dev.tjpal.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) = runBlocking {
    val defaultBase = "http://localhost:8081"
    var baseUrl = defaultBase
    args.forEach { arg ->
        if (arg.startsWith("--base-url=")) baseUrl = arg.substringAfter("=")
    }

    val client = HttpClient(OkHttp)
    val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    println("Atrion CLI test client")
    println("Server base URL: $baseUrl")
    println("Type 'help' for available commands")

    while (true) {
        print("> ")
        val line = readLine() ?: break
        val trimmed = line.trim()
        if (trimmed.isEmpty()) continue

        val parts = trimmed.split(Regex("\\s+"), 3)
        val cmd = parts[0].lowercase()

        try {
            when (cmd) {
                "help", "h", "?" -> printHelp()
                "exit", "quit" -> {
                    println("Exiting")
                    client.close()
                    exitProcess(0)
                }
                "definitions" -> doGet(client, baseUrl, "/definitions", json)
                "upload-graph" -> {
                    val arg = parts.getOrNull(1)
                    if (arg == null) {
                        println("Usage: upload-graph <@file|paste|json>")
                        continue
                    }
                    val body = if (arg.startsWith("@")) {
                        val path = arg.substring(1)
                        File(path).readText()
                    } else if (arg == "paste") {
                        println("Paste graph JSON, end with a single line containing only 'EOF'")
                        readMultilineUntilEOF()
                    } else {
                        parts.getOrNull(2) ?: arg
                    }

                    doPostRaw(client, baseUrl, "/graphs", body, json)
                }
                "modify-graph", "update-graph" -> {
                    val id = parts.getOrNull(1)
                    if (id.isNullOrBlank()) {
                        println("Usage: modify-graph <id> <@file|paste|json>")
                        continue
                    }

                    val payloadArg = parts.getOrNull(2)
                    val body = when {
                        payloadArg == null -> {
                            println("Usage: modify-graph <id> <@file|paste|json>")
                            continue
                        }
                        payloadArg.startsWith("@") -> File(payloadArg.substring(1)).readText()
                        payloadArg == "paste" -> {
                            println("Paste graph JSON, end with a single line containing only 'EOF'")
                            readMultilineUntilEOF()
                        }
                        else -> payloadArg
                    }

                    doPutRaw(client, baseUrl, "/graphs/$id", body, json)
                }
                "get-graph" -> {
                    val id = parts.getOrNull(1)
                    if (id.isNullOrBlank()) { println("Usage: get-graph <id>"); continue }
                    doGet(client, baseUrl, "/graphs/$id", json)
                }
                "delete-graph" -> {
                    val id = parts.getOrNull(1)
                    if (id.isNullOrBlank()) { println("Usage: delete-graph <id>"); continue }
                    doDelete(client, baseUrl, "/graphs/$id", json)
                }
                "start-execution" -> {
                    val graphId = parts.getOrNull(1)
                    if (graphId.isNullOrBlank()) { println("Usage: start-execution <graphId>"); continue }

                    val jsonInput = "{\"graphId\":\"$graphId\"}"

                    doPostRaw(client, baseUrl, "/executions", jsonInput, json)
                }
                "list-executions" -> doGet(client, baseUrl, "/executions", json)
                "delete-execution" -> {
                    val id = parts.getOrNull(1)
                    if (id.isNullOrBlank()) { println("Usage: delete-execution <id>"); continue }
                    doDelete(client, baseUrl, "/executions/$id", json)
                }
                "rest-input" -> {
                    // Usage: rest-input <executionId> <nodeId> <@file|payload>
                    val exe = parts.getOrNull(1)
                    val rest = parts.getOrNull(2)
                    if (exe.isNullOrBlank() || rest.isNullOrBlank()) {
                        println("Usage: rest-input <executionId> <nodeId> <@file|payload>")
                        println("Note: when payload contains spaces, quote it or use @filename to read from file")
                        continue
                    }

                    val sub = rest.split(Regex("\\s+"), 2)
                    val nodeId = sub[0]
                    val payloadRaw = if (sub.size > 1) sub[1] else ""

                    val payload = if (payloadRaw.startsWith("@")) {
                        val path = payloadRaw.substring(1)
                        File(path).readText()
                    } else if (payloadRaw.isEmpty()) {
                        println("Enter payload (single line):")
                        readLine() ?: ""
                    } else payloadRaw

                    val reqJson = "{\"executionId\":\"$exe\",\"nodeId\":\"$nodeId\",\"payload\":\"${escapeForJson(payload)}\"}"
                    doPostRaw(client, baseUrl, "/rest-input", reqJson, json)
                }
                "outputs" -> {
                    val id = parts.getOrNull(1)
                    if (id.isNullOrBlank()) { println("Usage: outputs <executionId>"); continue }
                    doGet(client, baseUrl, "/executions/$id/outputs", json)
                }
                else -> println("Unknown command: $cmd. Type 'help' for commands.")
            }
        } catch (e: Exception) {
            println("Error: ${e.message}")
        }
    }
}

private fun printHelp() {
    println(
        """
Available commands:
  definitions
      GET /definitions - list node definitions

  upload-graph <@file|paste|json>
      POST /graphs - create a new graph. Use @path/to/file to load JSON from file, 'paste' to paste JSON interactively, or provide JSON inline.
      Note: the server returns only the created id (not the full graph).

  modify-graph <id> <@file|paste|json>
      PUT /graphs/{id} - replace an existing graph by id. Use @path/to/file to load JSON from file, 'paste' to paste JSON interactively, or provide JSON inline.

  get-graph <id>
      GET /graphs/{id}

  delete-graph <id>
      DELETE /graphs/{id}

  start-execution <graphId>
      POST /executions { "graphId": "..." }

  list-executions
      GET /executions

  delete-execution <id>
      DELETE /executions/{id}

  rest-input <executionId> <nodeId> <@file|payload>
      POST /rest-input - send input to a specific active execution/node. Use @file to read payload from file.

  outputs <executionId>
      GET /executions/{id}/outputs

  help
  exit
""".trimIndent()
    )
}

private suspend fun doGet(client: HttpClient, baseUrl: String, path: String, json: Json) {
    val url = baseUrl.trimEnd('/') + path
    val resp: HttpResponse = client.get(url)
    printResponse(resp)
}

private suspend fun doDelete(client: HttpClient, baseUrl: String, path: String, json: Json) {
    val url = baseUrl.trimEnd('/') + path
    val resp: HttpResponse = client.delete(url)
    printResponse(resp)
}

private suspend fun doPostRaw(client: HttpClient, baseUrl: String, path: String, rawJson: String, json: Json) {
    val url = baseUrl.trimEnd('/') + path
    val resp: HttpResponse = client.post(url) {
        setBody(TextContent(rawJson, ContentType.Application.Json))
    }
    printResponse(resp)
}

private suspend fun doPutRaw(client: HttpClient, baseUrl: String, path: String, rawJson: String, json: Json) {
    val url = baseUrl.trimEnd('/') + path
    val resp: HttpResponse = client.put(url) {
        setBody(TextContent(rawJson, ContentType.Application.Json))
    }
    printResponse(resp)
}

private suspend fun printResponse(resp: HttpResponse) {
    val status = resp.status
    val text = try { resp.bodyAsText() } catch (e: Exception) { "" }

    println("HTTP ${status.value} ${status.description}")

    if (text.isNullOrBlank()) return

    // Print the complete response body as a string
    println(text)
}

private fun readMultilineUntilEOF(): String {
    val sb = StringBuilder()
    while (true) {
        val l = readLine() ?: break
        if (l.trim() == "EOF") break
        sb.append(l).append('\n')
    }
    return sb.toString()
}

private fun escapeForJson(s: String): String {
    return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
}
