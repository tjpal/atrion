package dev.tjpal.secrets

import dagger.Binds
import dagger.Module
import javax.inject.Singleton

@Module
abstract class SecretsModule {
    @Binds
    @Singleton
    abstract fun bindSecretStore(impl: FileSystemEncryptedSecretStore): SecretStore
}
