package dev.tjpal.secrets

interface SecretStore {
    /**
     * Store a secret. If secretId is null a new UUID will be generated and returned.
     * plaintextJson is the plaintext JSON string to encrypt and store.
     */
    fun put(secretId: String? = null, name: String, type: String, plaintextJson: String, tags: List<String> = emptyList()): String

    /**
     * Retrieve decrypted plaintext JSON for the given secret id.
     * Throws IllegalArgumentException if the secret is not found or decryption fails.
     */
    fun get(secretId: String): String

    /**
     * Replace the plaintext payload for an existing secret (increments version and updates timestamps).
     */
    fun replace(secretId: String, plaintextJson: String)

    /**
     * Delete the secret file.
     */
    fun delete(secretId: String)

    /**
     * List metadata (no plaintext) for all secrets.
     */
    fun list(): List<SecretMetadata>
}
