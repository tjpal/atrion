package dev.tjpal.filesystem

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.inject.Singleton

@Singleton
class DefaultFileSystemService : FileSystemService {
    override fun createDirectories(path: Path) {
        Files.createDirectories(path)
    }

    override fun write(path: Path, bytes: ByteArray) {
        Files.write(path, bytes)
    }

    override fun exists(path: Path): Boolean = Files.exists(path)

    override fun readString(path: Path): String = Files.readString(path)

    override fun list(path: Path): List<Path> {
        if (!Files.exists(path) || !Files.isDirectory(path)) return emptyList()
        return Files.list(path).use { stream -> stream.toList() }
    }

    override fun deleteIfExists(path: Path): Boolean = Files.deleteIfExists(path)

    override fun isDirectory(path: Path): Boolean = Files.isDirectory(path)

    override fun getPath(first: String, vararg more: String): Path = Paths.get(first, *more)
}
