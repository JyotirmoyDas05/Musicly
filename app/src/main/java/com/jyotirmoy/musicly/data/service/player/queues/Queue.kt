package com.jyotirmoy.musicly.data.service.player.queues

import com.jyotirmoy.musicly.data.model.MediaMetadata

/**
 * Represents a playback queue that can load items incrementally.
 * Modeled after Metrolist's queue system for YouTube Music radio/autoplay.
 */
interface Queue {
    /** An optional item to preload (e.g. the song the user tapped) before the full queue loads. */
    val preloadItem: MediaMetadata?

    /** Fetches the initial queue status (title, items, starting index). */
    suspend fun getInitialStatus(): Status

    /** Whether there are more items to load via pagination. */
    fun hasNextPage(): Boolean

    /** Loads the next page of items via continuation token. */
    suspend fun nextPage(): List<MediaMetadata>

    data class Status(
        val title: String?,
        val items: List<MediaMetadata>,
        val mediaItemIndex: Int,
        val position: Long = 0L,
    )
}
