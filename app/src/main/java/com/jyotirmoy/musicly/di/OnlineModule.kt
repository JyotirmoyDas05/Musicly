package com.jyotirmoy.musicly.di

import com.jyotirmoy.musicly.data.repository.YouTubeRepository
import com.jyotirmoy.musicly.data.repository.YouTubeRepositoryImpl
import com.jyotirmoy.musicly.data.repository.UpdateRepositoryImpl
import com.jyotirmoy.musicly.data.repository.DownloadRepositoryImpl
import com.jyotirmoy.musicly.domain.repository.UpdateRepository
import com.jyotirmoy.musicly.domain.repository.DownloadRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for online/YouTube Music dependencies and update/download functionality.
 *
 * Binds the [YouTubeRepository] interface to its implementation.
 * Binds the [UpdateRepository] and [DownloadRepository] interfaces.
 * Use cases (SearchOnlineUseCase, GetAlbumDetailsUseCase, etc.) are
 * constructor-injected and don't need explicit providers.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class OnlineModule {

    @Binds
    @Singleton
    abstract fun bindYouTubeRepository(
        impl: YouTubeRepositoryImpl
    ): YouTubeRepository

    @Binds
    @Singleton
    abstract fun bindUpdateRepository(
        impl: UpdateRepositoryImpl
    ): UpdateRepository

    @Binds
    @Singleton
    abstract fun bindDownloadRepository(
        impl: DownloadRepositoryImpl
    ): DownloadRepository
}
