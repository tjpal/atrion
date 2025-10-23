package dev.tjpal

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform