package dev.tjpal.secrets

import dev.tjpal.config.Config
import dev.tjpal.filesystem.FileSystemService
import dev.tjpal.logging.logger
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileSystemEncryptedSecretStore @Inject constructor(
    private val config: Config,
    private val json: Json,
    private val fileSystemService: FileSystemService
) : SecretStore {
    private val logger = logger<FileSystemEncryptedSecretStore>()
    private val masterKey: ByteArray
    private val secureRandom = SecureRandom()
    private val secretsDirPath = fileSystemService.getPath(config.secretsDirectory)

    init {
        try {
            fileSystemService.createDirectories(secretsDirPath)

            trySetPosixPermissions(secretsDirPath)
        } catch (e: Exception) {
            logger.error("Failed to ensure secrets directory exists: {}", e.message)
            throw IllegalStateException("Failed to ensure secrets directory exists: ${config.secretsDirectory}", e)
        }

        masterKey = loadMasterKey()
        require(masterKey.size == 32) { "Master key must be 32 bytes (256 bits)" }

        logger.info("FileSystemEncryptedSecretStore initialized secretsDir={}", config.secretsDirectory)
    }

    private fun trySetPosixPermissions(path: java.nio.file.Path) {
        try {
            val perms = PosixFilePermissions.fromString("rwx------")
            Files.setPosixFilePermissions(path, perms)
        } catch (e: UnsupportedOperationException) {
            // File system might not support POSIX permissions (e.g., Windows). Ignore it for now.
        } catch (e: Exception) {
            logger.debug("Could not set POSIX permissions for {}: {}", path, e.message)
        }
    }

    private fun loadMasterKey(): ByteArray {
        val env = System.getenv("ATRION_MASTER_KEY")

        if (!env.isNullOrBlank()) {
            val decoder = Base64.getDecoder()

            try {
                val decoded = decoder.decode(env.trim())
                if (decoded.size == 32) {
                    return decoded
                }
                logger.warn("ATRION_MASTER_KEY provided but decoded base64 length is not 32 bytes (actual=${decoded.size}).")
            } catch (e: IllegalArgumentException) {
                // Not valid base64. Pass on the exception. Raw data is only supported for files since shells often
                // cannot deal with raw binary data.
                throw e
            }
        }

        val masterPath = fileSystemService.getPath(config.secretsMasterKeyPath)

        return try {
            if (!fileSystemService.exists(masterPath)) {
                throw IllegalStateException("Master key not found: no ATRION_MASTER_KEY env var and file does not exist at ${config.secretsMasterKeyPath}")
            }

            val text = fileSystemService.readString(masterPath).trim()
            val decoder = Base64.getDecoder()
            try {
                val decoded = decoder.decode(text)
                if (decoded.size == 32) return decoded
            } catch (e: IllegalArgumentException) {
                // Not valid base64, try raw
            }

            val raw = text.toByteArray(Charsets.UTF_8)
            if (raw.size == 32) {
                return raw
            }

            throw IllegalStateException("Master key file exists but does not contain a valid 32-byte key (raw) or base64-encoded 32-byte key")
        } catch (e: Exception) {
            throw IllegalStateException("Failed to load master key: ${e.message}", e)
        }
    }

    override fun put(secretId: String?, name: String, type: String, plaintextJson: String, tags: List<String>): String {
        val id = secretId ?: UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val (ciphertextB64, ivB64) = encrypt(plaintextJson)

        val record = SecretRecord(
            id = id,
            name = name,
            type = type,
            version = 1,
            createdAt = now,
            updatedAt = now,
            iv = ivB64,
            ciphertext = ciphertextB64,
            tags = tags
        )

        val bytes = json.encodeToString(record).toByteArray(Charsets.UTF_8)
        val path = secretsDirPath.resolve("${id}.json")

        fileSystemService.write(path, bytes)
        trySetPosixPermissions(path)

        logger.info("Secret stored id={} name={} type={}", id, name, type)
        return id
    }

    override fun get(secretId: String): String {
        val path = secretsDirPath.resolve("${secretId}.json")
        if (!fileSystemService.exists(path)) {
            throw IllegalArgumentException("No secret with id: $secretId")
        }

        val text = fileSystemService.readString(path)
        val record = json.decodeFromString<SecretRecord>(text)

        return decrypt(record.ciphertext, record.iv)
    }

    override fun replace(secretId: String, plaintextJson: String) {
        val path = secretsDirPath.resolve("${secretId}.json")
        if (!fileSystemService.exists(path)) {
            throw IllegalArgumentException("No secret with id: $secretId")
        }

        val oldText = fileSystemService.readString(path)
        val oldRecord = json.decodeFromString<SecretRecord>(oldText)

        val (ciphertextB64, ivB64) = encrypt(plaintextJson)
        val now = System.currentTimeMillis()

        val newRecord = oldRecord.copy(
            version = oldRecord.version + 1,
            updatedAt = now,
            iv = ivB64,
            ciphertext = ciphertextB64
        )

        val bytes = json.encodeToString(newRecord).toByteArray(Charsets.UTF_8)
        fileSystemService.write(path, bytes)

        logger.info("Secret replaced id={} name={} version={}", secretId, newRecord.name, newRecord.version)
    }

    override fun delete(secretId: String) {
        val path = secretsDirPath.resolve("${secretId}.json")
        if (!fileSystemService.exists(path)) {
            throw IllegalArgumentException("No secret with id: $secretId")
        }

        val deleted = try {
            fileSystemService.deleteIfExists(path)
        } catch (e: Exception) {
            throw IllegalStateException("Failed to delete secret file: ${e.message}", e)
        }

        if (!deleted) throw IllegalArgumentException("No secret with id: $secretId")

        logger.info("Secret deleted id={}", secretId)
    }

    override fun list(): List<SecretMetadata> {
        if (!fileSystemService.exists(secretsDirPath) || !fileSystemService.isDirectory(secretsDirPath)) {
            return emptyList()
        }

        return fileSystemService.list(secretsDirPath)
            .filter { path -> path.fileName.toString().endsWith(".json") }
            .mapNotNull { path ->
                try {
                    val text = fileSystemService.readString(path)
                    val record = json.decodeFromString<SecretRecord>(text)
                    SecretMetadata(
                        id = record.id,
                        name = record.name,
                        type = record.type,
                        version = record.version,
                        createdAt = record.createdAt,
                        updatedAt = record.updatedAt,
                        tags = record.tags
                    )
                } catch (e: Exception) {
                    logger.error("Failed to read/parse secret file ${'$'}{path.fileName}: ${e.message}")
                    null
                }
            }
    }

    private fun encrypt(plaintext: String): Pair<String, String> {
        val iv = ByteArray(12)
        secureRandom.nextBytes(iv)

        val spec = GCMParameterSpec(128, iv)
        val keySpec = SecretKeySpec(masterKey, "AES")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, spec)

        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        val encoder = Base64.getEncoder()
        return Pair(encoder.encodeToString(ciphertext), encoder.encodeToString(iv))
    }

    private fun decrypt(ciphertextB64: String, ivB64: String): String {
        val decoder = Base64.getDecoder()
        val ciphertext = decoder.decode(ciphertextB64)
        val iv = decoder.decode(ivB64)

        val spec = GCMParameterSpec(128, iv)
        val keySpec = SecretKeySpec(masterKey, "AES")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, keySpec, spec)

        val plaintextBytes = cipher.doFinal(ciphertext)
        return String(plaintextBytes, Charsets.UTF_8)
    }
}
