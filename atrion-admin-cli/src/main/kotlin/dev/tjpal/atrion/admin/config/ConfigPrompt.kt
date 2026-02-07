package dev.tjpal.atrion.admin.config

data class ServerUrl(
    val serverUrl: String,
    val serverPort: Int,
    val udsPath: String
)

fun parseServerUrl(input: String): ServerUrl {
    when {
        input.startsWith("https://") -> {
            val url = input.removePrefix("https://")
            val parts = url.split(":")

            if(parts.size != 2) {
                throw IllegalArgumentException("Invalid server URL format")
            }

            val host = parts[0]
            val port = parts[1].toIntOrNull() ?: throw IllegalArgumentException("Invalid port number")

            return ServerUrl(serverUrl = host, serverPort = port, udsPath = "")
        }
        input.startsWith("uds://") -> {
            val path = input.removePrefix("uds://")

            return ServerUrl(serverUrl = "", serverPort = 0, udsPath = path)
        }
        else -> throw IllegalArgumentException("Invalid server URL format")
    }
}

fun promptUserForConfig(): Config {
    println("Enter server URL ( https://<server-name>:port or uds://path")
    val serverUrl = parseServerUrl(readln())

    println("Enter key directory: Default: ${System.getProperty("user.home")}/.atrion-admin/keys")
    val keyDirectoryInput = readln()
    val keyDirectory = keyDirectoryInput.ifBlank {
        System.getProperty("user.home") + "/.atrion-admin/keys"
    }

    println("Enter admin username: Default: admin")
    val adminUsernameInput = readln()
    val adminUsername = adminUsernameInput.ifBlank { "admin" }

    val config = Config(
        serverUrl = serverUrl.serverUrl,
        serverPort = serverUrl.serverPort,
        udsPath = serverUrl.udsPath,
        keyDirectory = keyDirectory,
        adminUsername = adminUsername
    )

    config.store("${System.getProperty("user.home")}/.atrion-admin/config.json")
    return config
}

fun getConfig(): Config {
    var config = Config.tryLoad()

    if (config != null) {
        return config
    }

    println("No or invalid config found")
    return promptUserForConfig()
}

