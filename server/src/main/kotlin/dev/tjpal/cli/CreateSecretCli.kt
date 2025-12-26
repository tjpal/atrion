package dev.tjpal.cli

import dev.tjpal.config.ConfigLoader
import dev.tjpal.filesystem.DefaultFileSystemService
import dev.tjpal.secrets.FileSystemEncryptedSecretStore
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.system.exitProcess

/**
 * Simple CLI to create encrypted secrets using the application's SecretStore implementation.
 *
 * Usage example:
 *   java -jar atrion-cli.jar --name jira-prod --type jira_credentials --file /tmp/jira-plaintext.json [--id my-id] [--tags tag1,tag2] [--delete]
 *
 * - The CLI uses the same master-key lookup rules as the server: ATRION_MASTER_KEY env var (base64 32 bytes) or file at config.secretsMasterKeyPath.
 * - The CLI loads the same config.json as the server; it will use ATRION_CONFIG_PATH env var if set or the default ~/.atrion/config.json.
 */
fun main(rawArgs: Array<String>) {
    val args = rawArgs.toList()

    if (args.isEmpty() || args.contains("--help") || args.contains("-h")) {
        printUsageAndExit()
    }

    fun getArgValue(flag: String): String? {
        val idx = args.indexOf(flag)
        if (idx >= 0 && idx + 1 < args.size) return args[idx + 1]
        return null
    }

    val name = getArgValue("--name") ?: run {
        System.err.println("Missing --name")
        printUsageAndExit(1)
        return
    }

    val type = getArgValue("--type") ?: run {
        System.err.println("Missing --type")
        printUsageAndExit(1)
        return
    }

    val filePath = getArgValue("--file") ?: run {
        System.err.println("Missing --file (path to plaintext JSON)")
        printUsageAndExit(1)
        return
    }

    val id = getArgValue("--id")
    val tagsRaw = getArgValue("--tags")
    val deleteAfter = args.contains("--delete")

    val tags = tagsRaw?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()

    try {
        val plaintextFile = File(filePath)
        if (!plaintextFile.exists()) {
            System.err.println("Plaintext file not found: $filePath")
            exitProcess(2)
        }

        val plaintext = plaintextFile.readText()

        val configPath = System.getenv("ATRION_CONFIG_PATH")
            ?: System.getProperty("user.home") + "/.atrion/config.json"

        val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

        val configLoader = ConfigLoader(configPath, json)
        val config = configLoader.loadOrCreate()

        val filesystemService = DefaultFileSystemService()
        val store = FileSystemEncryptedSecretStore(config, json, filesystemService)

        val createdId = store.put(id, name, type, plaintext, tags)

        println("Secret stored with id=$createdId")

        if (deleteAfter) {
            try {
                if (plaintextFile.delete()) {
                    println("Deleted plaintext file: $filePath")
                } else {
                    System.err.println("Unable to delete plaintext file: $filePath (please remove manually)")
                }
            } catch (e: Exception) {
                System.err.println("Error deleting plaintext file: ${e.message}")
            }
        }

        exitProcess(0)
    } catch (e: Exception) {
        System.err.println("Failed to create secret: ${e.message}")
        e.printStackTrace(System.err)
        exitProcess(3)
    }
}

private fun printUsageAndExit(code: Int = 0): Nothing {
    println("Usage: CreateSecretCli --name <name> --type <type> --file <plaintext.json> [--id <id>] [--tags tag1,tag2] [--delete]")
    println()
    println("Options:")
    println("  --name    Human-readable name for the secret (required)")
    println("  --type    Type identifier, e.g. 'jira_credentials' (required)")
    println("  --file    Path to plaintext JSON file containing the secret payload (required)")
    println("  --id      Optional id to use (if omitted a UUID is generated)")
    println("  --tags    Optional comma-separated tags")
    println("  --delete  If present, the plaintext file will be deleted after successful import")
    println()
    println("Master key: set ATRION_MASTER_KEY env var (base64 32 bytes) or ensure config.secretsMasterKeyPath file exists and contains the key")
    kotlin.system.exitProcess(code)
}
