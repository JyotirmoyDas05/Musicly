package com.jyotirmoy.musicly.data.service.player.queues

import com.jyotirmoy.musicly.data.model.MediaMetadata
import com.jyotirmoy.musicly.data.model.toMediaMetadata
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.WatchEndpoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * A queue powered by YouTube Music's radio/autoplay algorithm.
 *
 * When a user plays a single online song, this class fetches YouTube's "radio" playlist
 * using the format `RDAMVM${videoId}`, which returns a list of related songs
 * that YouTube's algorithm deems similar. Supports pagination via continuation tokens.
 *
 * This mirrors the behavior of Metrolist's YouTubeQueue.
 */
class YouTubeQueue(
    private var endpoint: WatchEndpoint,
    override val preloadItem: MediaMetadata? = null,
) : Queue {
    private var continuation: String? = null
    private var retryCount = 0
    private val maxRetries = 3

    override suspend fun getInitialStatus(): Queue.Status {
        return withContext(Dispatchers.IO) {
            var lastException: Throwable? = null

            for (attempt in 0..maxRetries) {
                try {
                    val nextResult = YouTube.next(endpoint, continuation).getOrThrow()
                    endpoint = nextResult.endpoint
                    continuation = nextResult.continuation
                    retryCount = 0
                    return@withContext Queue.Status(
                        title = nextResult.title,
                        items = nextResult.items.map { it.toMediaMetadata() },
                        mediaItemIndex = nextResult.currentIndex ?: 0,
                    )
                } catch (e: Exception) {
                    Timber.w(e, "YouTubeQueue: attempt $attempt failed")
                    lastException = e
                    // If first attempt fails and we have a videoId but no playlistId, try radio format
                    if (attempt == 0 && endpoint.videoId != null && endpoint.playlistId == null) {
                        endpoint = WatchEndpoint(
                            videoId = endpoint.videoId,
                            playlistId = "RDAMVM${endpoint.videoId}",
                            params = "wAEB"
                        )
                    }
                }
            }
            throw lastException ?: Exception("Failed to get initial status")
        }
    }

    override fun hasNextPage(): Boolean = continuation != null

    override suspend fun nextPage(): List<MediaMetadata> {
        return withContext(Dispatchers.IO) {
            var lastException: Throwable? = null

            for (attempt in 0..maxRetries) {
                try {
                    val nextResult = YouTube.next(endpoint, continuation).getOrThrow()
                    endpoint = nextResult.endpoint
                    continuation = nextResult.continuation
                    retryCount = 0
                    return@withContext nextResult.items.map { it.toMediaMetadata() }
                } catch (e: Exception) {
                    Timber.w(e, "YouTubeQueue.nextPage: attempt $attempt failed")
                    lastException = e
                    retryCount++
                    if (retryCount >= maxRetries) {
                        continuation = null // Stop trying to load more
                    }
                }
            }
            throw lastException ?: Exception("Failed to get next page")
        }
    }

    companion object {
        /**
         * Creates a YouTubeQueue configured as a radio station for the given song.
         * Uses YouTube's radio playlist format `RDAMVM${videoId}` to get similar songs.
         */
        fun radio(song: MediaMetadata): YouTubeQueue {
            return YouTubeQueue(
                WatchEndpoint(
                    videoId = song.id,
                    playlistId = "RDAMVM${song.id}",
                    params = "wAEB"
                ),
                song
            )
        }
    }
}
