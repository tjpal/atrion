package dev.tjpal.filesystem

import java.nio.file.Path

interface FileSystemService {
    fun createDirectories(path: Path)
    fun write(path: Path, bytes: ByteArray)
    fun exists(path: Path): Boolean
    fun readString(path: Path): String
    fun list(path: Path): List<Path>
    fun deleteIfExists(path: Path): Boolean
    fun isDirectory(path: Path): Boolean
    fun getPath(first: String, vararg more: String): Path
}
