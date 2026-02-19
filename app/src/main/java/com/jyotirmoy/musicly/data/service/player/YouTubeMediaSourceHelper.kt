package com.jyotirmoy.musicly.data.service.player

import android.content.Context
import android.net.ConnectivityManager
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSourceFactory
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.extractor.mkv.MatroskaExtractor
import androidx.media3.extractor.mp4.FragmentedMp4Extractor
import com.jyotirmoy.musicly.data.database.FormatCacheEntity
import com.jyotirmoy.musicly.data.database.OnlineDao
import com.jyotirmoy.musicly.data.preferences.AudioQuality
import com.jyotirmoy.musicly.utils.YTPlayerUtils
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.YouTubeClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides a [MediaSourceFactory] capable of playing both local content URIs and
 * YouTube Music streams. For YouTube content, it:
 *
 * 1. Checks the download cache (read-only) for fully-saved songs.
 * 2. Checks the player cache for recently-streamed data.
 * 3. Falls back to resolving the stream URL via [YTPlayerUtils.playerResponseForPlayback].
 *
 * The resolved URLs are cached in-memory with expiry timestamps to avoid redundant calls.
 * Format metadata (itag, bitrate, etc.) is persisted to Room via [OnlineDao].
 *
 * Stream URLs are identified by the MediaItem's mediaId: if it looks like a YouTube video ID
 * (11-char alphanumeric string), it is resolved; otherwise, it is played directly as a URI.
 */
@OptIn(UnstableApi::class)
@Singleton
class YouTubeMediaSourceHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    @com.jyotirmoy.musicly.di.PlayerCache private val playerCache: SimpleCache,
    @com.jyotirmoy.musicly.di.DownloadCache private val downloadCache: SimpleCache,
    private val onlineDao: OnlineDao,
) {
    companion object {
        private const val TAG = "YTMediaSource"
        /**
         * Chunk size for cache writes. YouTube streams are fetched in 512 KB segments.
         */
        private const val CHUNK_LENGTH = 512L * 1024L

        /**
         * Simple heuristic: YouTube video IDs are 11-char base64url strings.
         * Local content URIs and file paths will never match this pattern.
         */
        private val YOUTUBE_ID_REGEX = Regex("^[A-Za-z0-9_-]{11}$")
    }

    /**
     * In-memory cache of resolved stream URLs mapped by mediaId -> (url, expiryTimestamp).
     */
    private val songUrlCache = HashMap<String, Pair<String, Long>>()

    /**
     * Current audio quality preference. Must be updated when user changes setting.
     */
    @Volatile
    var audioQuality: AudioQuality = AudioQuality.AUTO

    /**
     * Set to temporarily bypass cache for a specific mediaId (e.g., after quality change).
     */
    @Volatile
    var bypassCacheMediaId: String? = null

    /**
     * Clears the in-memory URL cache for a specific video.
     */
    fun clearUrlCache(mediaId: String) {
        songUrlCache.remove(mediaId)
    }

    /**
     * Clears the entire in-memory URL cache.
     */
    fun clearAllUrlCache() {
        songUrlCache.clear()
    }

    /**
     * Clears cached data for a video from the player cache.
     */
    fun clearPlayerCache(mediaId: String) {
        playerCache.removeResource(mediaId)
    }

    /**
     * Creates the full [MediaSourceFactory] chain:
     *   ResolvingDataSource -> CacheDataSource (download + player) -> OkHttp/Default upstream
     */
    fun createMediaSourceFactory(): MediaSourceFactory {
        return DefaultMediaSourceFactory(
            createResolvingDataSourceFactory(),
            // Only WebM (Matroska) and fragmented MP4 extractors are needed for YouTube streams
            ExtractorsFactory {
                arrayOf(MatroskaExtractor(), FragmentedMp4Extractor())
            }
        )
    }

    /**
     * Creates a [DataSource.Factory] that falls through to default behavior for local URIs.
     * This allows the same ExoPlayer to handle both local songs and YouTube streams.
     */
    fun createHybridMediaSourceFactory(): MediaSourceFactory {
        // DefaultMediaSourceFactory with default extractors handles local files,
        // while our custom factory handles YouTube streams via the resolver.
        return DefaultMediaSourceFactory(createResolvingDataSourceFactory())
    }

    // ---- Internal factory chain ----

    private fun createResolvingDataSourceFactory(): DataSource.Factory {
        return ResolvingDataSource.Factory(createCacheDataSourceFactory()) { dataSpec ->
            resolveDataSpec(dataSpec)
        }
    }

    private fun createCacheDataSourceFactory(): DataSource.Factory {
        val upstreamFactory = createUpstreamDataSourceFactory()

        // Download cache: read-only (no writes), checked first
        val downloadCacheFactory = CacheDataSource.Factory()
            .setCache(downloadCache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setCacheWriteDataSinkFactory(null) // read-only
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        // Player cache: write-through, wraps around download cache
        return CacheDataSource.Factory()
            .setCache(playerCache)
            .setUpstreamDataSourceFactory(downloadCacheFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    private fun createUpstreamDataSourceFactory(): DataSource.Factory {
        // Use OkHttp for network requests with YouTube proxy support
        val okHttpClient = OkHttpClient.Builder()
            .proxy(YouTube.proxy)
            .build()

        val okHttpFactory = OkHttpDataSource.Factory(okHttpClient)
            .setUserAgent(YouTubeClient.USER_AGENT_WEB)

        // DefaultDataSource wraps OkHttp for network + handles content:// and file:// URIs
        return DefaultDataSource.Factory(context, okHttpFactory)
    }

    /**
     * The core resolver. Determines if this is a YouTube stream request and resolves the URL.
     * For local content (content:// URIs, file paths), returns the dataSpec unchanged.
     */
    private fun resolveDataSpec(dataSpec: DataSpec): DataSpec {
        val mediaId = dataSpec.key ?: return dataSpec

        // Check if this looks like a YouTube video ID
        if (!YOUTUBE_ID_REGEX.matches(mediaId)) {
            return dataSpec
        }

        // Check if download cache has this content
        if (downloadCache.isCached(mediaId, dataSpec.position, CHUNK_LENGTH)) {
            return dataSpec
        }

        // Check if player cache has this content (unless bypassing)
        val shouldBypassCache = bypassCacheMediaId == mediaId
        if (!shouldBypassCache && playerCache.isCached(mediaId, dataSpec.position, CHUNK_LENGTH)) {
            return dataSpec
        }
        if (shouldBypassCache) {
            bypassCacheMediaId = null // One-time bypass
        }

        // Check in-memory URL cache
        val cached = songUrlCache[mediaId]
        if (cached != null && cached.second > System.currentTimeMillis()) {
            return dataSpec
                .withUri(cached.first.toUri())
                .subrange(dataSpec.uriPositionOffset, CHUNK_LENGTH)
        }

        // Resolve stream URL via YTPlayerUtils
        Timber.tag(TAG).d("Resolving stream URL for mediaId=%s", mediaId)
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val playbackData = runBlocking(Dispatchers.IO) {
            YTPlayerUtils.playerResponseForPlayback(
                videoId = mediaId,
                audioQuality = audioQuality,
                connectivityManager = connectivityManager,
            )
        }.getOrElse { throwable ->
            Timber.tag(TAG).e(throwable, "Failed to resolve stream URL for %s", mediaId)
            throw throwable
        }

        val streamUrl = playbackData.streamUrl
        val expiresInSeconds = playbackData.streamExpiresInSeconds

        // Cache the resolved URL in memory
        songUrlCache[mediaId] = streamUrl to (System.currentTimeMillis() + (expiresInSeconds * 1000L))

        // Persist format metadata to Room
        runBlocking(Dispatchers.IO) {
            val format = playbackData.format
            onlineDao.upsertFormatCache(
                FormatCacheEntity(
                    id = mediaId,
                    itag = format.itag,
                    mimeType = format.mimeType,
                    bitrate = format.bitrate,
                    sampleRate = format.audioSampleRate,
                    contentLength = format.contentLength,
                    loudnessDb = playbackData.audioConfig?.loudnessDb?.toFloat(),
                    playbackUrl = streamUrl,
                    expiresAt = System.currentTimeMillis() + (expiresInSeconds * 1000L),
                )
            )
        }

        Timber.tag(TAG).d(
            "Resolved stream for %s: itag=%d bitrate=%d expires=%ds",
            mediaId,
            playbackData.format.itag,
            playbackData.format.bitrate,
            expiresInSeconds,
        )

        return dataSpec
            .withUri(streamUrl.toUri())
            .subrange(dataSpec.uriPositionOffset, CHUNK_LENGTH)
    }
}
