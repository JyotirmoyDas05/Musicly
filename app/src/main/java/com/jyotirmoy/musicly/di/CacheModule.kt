package com.jyotirmoy.musicly.di

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

/**
 * Hilt module providing ExoPlayer cache instances for online streaming.
 *
 * Two separate caches are maintained:
 * - **PlayerCache**: Write-through streaming cache with LRU eviction (250 MB).
 *   Used to buffer audio as it streams from YouTube, avoiding re-downloads on seek.
 * - **DownloadCache**: Persistent cache for explicitly downloaded/saved songs.
 *   Uses NoOpCacheEvictor so downloads are never evicted automatically.
 */
@OptIn(UnstableApi::class)
@Module
@InstallIn(SingletonComponent::class)
object CacheModule {

    private const val PLAYER_CACHE_SIZE = 250L * 1024 * 1024 // 250 MB

    @Provides
    @Singleton
    fun provideDatabaseProvider(
        @ApplicationContext context: Context,
    ): StandaloneDatabaseProvider {
        return StandaloneDatabaseProvider(context)
    }

    @Provides
    @Singleton
    @PlayerCache
    fun providePlayerCache(
        @ApplicationContext context: Context,
        databaseProvider: StandaloneDatabaseProvider,
    ): SimpleCache {
        val cacheDir = File(context.cacheDir, "exo_player_cache")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        return SimpleCache(
            cacheDir,
            LeastRecentlyUsedCacheEvictor(PLAYER_CACHE_SIZE),
            databaseProvider,
        )
    }

    @Provides
    @Singleton
    @DownloadCache
    fun provideDownloadCache(
        @ApplicationContext context: Context,
        databaseProvider: StandaloneDatabaseProvider,
    ): SimpleCache {
        val cacheDir = File(context.filesDir, "exo_download_cache")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        return SimpleCache(
            cacheDir,
            NoOpCacheEvictor(),
            databaseProvider,
        )
    }
}
