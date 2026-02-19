package com.jyotirmoy.musicly.di

import com.jyotirmoy.musicly.data.repository.YouTubeRepository
import com.jyotirmoy.musicly.data.repository.YouTubeRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for online/YouTube Music dependencies.
 *
 * Binds the [YouTubeRepository] interface to its implementation.
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
}
