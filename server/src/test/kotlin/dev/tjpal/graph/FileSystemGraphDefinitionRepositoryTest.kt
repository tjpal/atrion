package dev.tjpal.graph

import dev.tjpal.config.Config
import dev.tjpal.model.GraphDefinition
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TestFileSystemService(private val baseDirectory: Path) : dev.tjpal.filesystem.FileSystemService {
    override fun createDirectories(path: Path) {
        val target = toReal(path)
        Files.createDirectories(target)
    }

    override fun write(path: Path, bytes: ByteArray) {
        val target = toReal(path)
        Files.write(target, bytes)
    }

    override fun exists(path: Path): Boolean = Files.exists(toReal(path))

    override fun readString(path: Path): String = Files.readString(toReal(path))

    override fun list(path: Path): List<Path> {
        val target = toReal(path)
        if (!Files.exists(target) || !Files.isDirectory(target)) return emptyList()
        return Files.list(target).use { stream -> stream.toList() }
    }

    override fun deleteIfExists(path: Path): Boolean = Files.deleteIfExists(toReal(path))

    override fun isDirectory(path: Path): Boolean = Files.isDirectory(toReal(path))

    override fun getPath(first: String, vararg more: String): Path {
        val candidate = if (more.isEmpty()) Paths.get(first) else Paths.get(first, *more)
        return if (candidate.isAbsolute) candidate else baseDirectory.resolve(candidate)
    }

    private fun toReal(path: Path): Path {
        return if (path.isAbsolute) path else baseDirectory.resolve(path)
    }
}

class FileSystemGraphDefinitionRepositoryTest {
    private lateinit var baseDirectory: Path
    private lateinit var fileSystemService: TestFileSystemService
    private lateinit var repository: GraphDefinitionRepository
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    @BeforeTest
    fun setUp() {
        baseDirectory = Files.createTempDirectory("graphs-fs-test")
        fileSystemService = TestFileSystemService(baseDirectory)
        val config = Config(
            httpHost = "0.0.0.0",
            httpPort = 8081,
            udsPath = "/tmp/atrion.socket",
            storageDirectory = baseDirectory.toString(),
            openAICredentialPath = "",
            statusRetentionEntries = 10
        )
        repository = FileSystemGraphDefinitionRepository(config, json, fileSystemService)
    }

    @AfterTest
    fun tearDown() {
        try {
            Files.walk(baseDirectory)
                .sorted(Comparator.reverseOrder())
                .forEach { path -> try { Files.deleteIfExists(path) } catch (ignored: Exception) { } }
        } catch (ignored: Exception) {
        }
    }

    @Test
    fun `adding a new graph returns an id and it can be retrieved`() {
        val graph = GraphDefinition(projectId = "proj-1", nodes = emptyList(), edges = emptyList())
        val generatedId = repository.add(graph)

        assertTrue(generatedId.isNotBlank())

        val retrieved = repository.get(generatedId)

        assertEquals(generatedId, retrieved.id)
        assertEquals(graph.projectId, retrieved.projectId)
        assertEquals(graph.nodes, retrieved.nodes)
        assertEquals(graph.edges, retrieved.edges)
    }

    @Test
    fun `retrieving non-existing graph throws`() {
        assertFailsWith<IllegalArgumentException> {
            repository.get("does-not-exist")
        }
    }

    @Test
    fun `deleting non-existing graph throws`() {
        assertFailsWith<IllegalStateException> {
            repository.delete("does-not-exist")
        }
    }

    @Test
    fun `deleting existing graph removes it`() {
        val graph = GraphDefinition(projectId = "proj-2", nodes = emptyList(), edges = emptyList())

        val generatedId = repository.add(graph)
        val retrieved = repository.get(generatedId)

        assertEquals(generatedId, retrieved.id)

        repository.delete(generatedId)

        assertFailsWith<IllegalArgumentException> {
            repository.get(generatedId)
        }
    }

    @Test
    fun `replace existing graph updates stored graph`() {
        val original = GraphDefinition(projectId = "original-project", nodes = emptyList(), edges = emptyList())

        val generatedId = repository.add(original)
        val replacement = GraphDefinition(projectId = "replacement-project", nodes = emptyList(), edges = emptyList())
        repository.replace(generatedId, replacement)

        val after = repository.get(generatedId)

        assertEquals(generatedId, after.id)
        assertEquals("replacement-project", after.projectId)
        assertEquals(replacement.nodes, after.nodes)
        assertEquals(replacement.edges, after.edges)
    }

    @Test
    fun `replace non-existing graph throws`() {
        val graph = GraphDefinition(projectId = "p", nodes = emptyList(), edges = emptyList())

        assertFailsWith<IllegalArgumentException> {
            repository.replace("does-not-exist", graph)
        }
    }
}
