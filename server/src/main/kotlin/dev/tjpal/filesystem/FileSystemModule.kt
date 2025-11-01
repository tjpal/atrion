package dev.tjpal.filesystem

import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class FileSystemModule {
    @Provides
    @Singleton
    fun provideFileSystemService(): FileSystemService = DefaultFileSystemService()
}
