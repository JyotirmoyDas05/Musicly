package com.jyotirmoy.musicly.di

import javax.inject.Qualifier

/**
 * Qualifier for Deezer Retrofit instance.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DeezerRetrofit

/**
 * Qualifier for Fast OkHttpClient (Short timeouts).
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class FastOkHttpClient

/**
 * Qualifier for Gson instance configured for backup serialization.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BackupGson

/**
 * Qualifier for ExoPlayer streaming cache (write-through, for buffering online playback).
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PlayerCache

/**
 * Qualifier for ExoPlayer download cache (for explicitly downloaded/saved songs).
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DownloadCache
