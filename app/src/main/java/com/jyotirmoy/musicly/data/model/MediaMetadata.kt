package com.jyotirmoy.musicly.data.model

import android.os.Bundle
import androidx.compose.runtime.Immutable
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata as ExoPlayerMediaMetadata
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem

/**
 * Unified media metadata that bridges between:
 * - Innertube API models (SongItem, AlbumItem, ArtistItem)
 * - Local Song model (offline files)
 * - Room entities (OnlineSongEntity)
 *
 * This serves as the "domain model" for playback, shared across online and offline sources.
 */
@Immutable
data class MediaMetadata(
    val id: String,
    val title: String,
    val artists: List<MediaArtist>,
    val album: MediaAlbum? = null,
    val duration: Int? = null, // seconds
    val thumbnailUrl: String? = null,
    val explicit: Boolean = false,
    val isLocal: Boolean = false,
    /** For local songs, the content URI; for online songs, null (resolved at playback time). */
    val contentUri: String? = null,
) {
    val displayArtist: String
        get() = artists.joinToString(", ") { it.name }

    companion object {
        fun empty() = MediaMetadata(
            id = "",
            title = "",
            artists = emptyList(),
        )
    }
}

@Immutable
data class MediaArtist(
    val id: String?,
    val name: String,
)

@Immutable
data class MediaAlbum(
    val id: String,
    val title: String,
)

// ---- Extension mappers ----

/**
 * Converts an innertube [SongItem] to [MediaMetadata].
 */
fun SongItem.toMediaMetadata(): MediaMetadata = MediaMetadata(
    id = id,
    title = title,
    artists = artists.map { MediaArtist(id = it.id, name = it.name) },
    album = album?.let { MediaAlbum(id = it.id, title = it.name) },
    duration = duration,
    thumbnailUrl = thumbnail,
    explicit = explicit,
    isLocal = false,
)

/**
 * Converts an [OnlineContentItem.SongContent] to [MediaMetadata].
 */
fun OnlineContentItem.SongContent.toMediaMetadata(): MediaMetadata = MediaMetadata(
    id = id,
    title = title,
    artists = artists,
    album = album,
    duration = duration,
    thumbnailUrl = thumbnailUrl,
    explicit = explicit,
    isLocal = false,
)

/**
 * Converts a local [Song] to [MediaMetadata].
 */
fun Song.toMediaMetadata(): MediaMetadata = MediaMetadata(
    id = id,
    title = title,
    artists = if (artists.isNotEmpty()) {
        artists.map { MediaArtist(id = it.id.toString(), name = it.name) }
    } else {
        listOf(MediaArtist(id = artistId.toString(), name = artist))
    },
    album = if (albumId >= 0) MediaAlbum(id = albumId.toString(), title = album) else null,
    duration = (duration / 1000).toInt(), // Song.duration is millis, MediaMetadata uses seconds
    thumbnailUrl = albumArtUriString,
    isLocal = true,
    contentUri = contentUriString,
)

/**
 * Converts an innertube [AlbumItem] to a simplified detail model.
 */
fun AlbumItem.toOnlineAlbumDetail(): OnlineAlbumDetail = OnlineAlbumDetail(
    browseId = browseId,
    playlistId = playlistId,
    title = title,
    artists = artists?.map { MediaArtist(id = it.id, name = it.name) } ?: emptyList(),
    year = year,
    thumbnailUrl = thumbnail,
)

/**
 * Converts an innertube [ArtistItem] to a simplified detail model.
 */
fun ArtistItem.toOnlineArtistDetail(): OnlineArtistDetail = OnlineArtistDetail(
    browseId = id,
    name = title,
    thumbnailUrl = thumbnail,
)

// ---- Online detail models ----

/**
 * Represents an album from YouTube Music with enough data to display details.
 */
@Immutable
data class OnlineAlbumDetail(
    val browseId: String,
    val playlistId: String,
    val title: String,
    val artists: List<MediaArtist>,
    val year: Int? = null,
    val thumbnailUrl: String? = null,
    val songs: List<MediaMetadata> = emptyList(),
    val otherVersions: List<OnlineAlbumDetail> = emptyList(),
) {
    val displayArtist: String
        get() = artists.joinToString(", ") { it.name }
}

/**
 * Represents an artist from YouTube Music with sections of content.
 */
@Immutable
data class OnlineArtistDetail(
    val browseId: String,
    val name: String,
    val thumbnailUrl: String? = null,
    val description: String? = null,
    val subscriberCountText: String? = null,
    val sections: List<OnlineArtistSection> = emptyList(),
)

/**
 * A section within an artist page (e.g., "Songs", "Albums", "Singles").
 */
@Immutable
data class OnlineArtistSection(
    val title: String,
    val items: List<OnlineContentItem>,
    val moreBrowseId: String? = null,
)

/**
 * A generic wrapper for items that appear in online content sections.
 * Can represent songs, albums, artists, or playlists from YouTube Music.
 */
@Immutable
sealed class OnlineContentItem {
    abstract val id: String
    abstract val title: String
    abstract val thumbnailUrl: String?

    data class SongContent(
        override val id: String,
        override val title: String,
        override val thumbnailUrl: String?,
        val artists: List<MediaArtist>,
        val album: MediaAlbum? = null,
        val duration: Int? = null,
        val explicit: Boolean = false,
    ) : OnlineContentItem()

    data class AlbumContent(
        override val id: String,
        val browseId: String,
        val playlistId: String,
        override val title: String,
        override val thumbnailUrl: String?,
        val artists: List<MediaArtist>,
        val year: Int? = null,
        val explicit: Boolean = false,
    ) : OnlineContentItem()

    data class ArtistContent(
        override val id: String,
        override val title: String,
        override val thumbnailUrl: String?,
    ) : OnlineContentItem()

    data class PlaylistContent(
        override val id: String,
        override val title: String,
        override val thumbnailUrl: String?,
        val author: String? = null,
        val songCount: String? = null,
    ) : OnlineContentItem()

    /**
     * Represents a mood/genre item from the YouTube Music Explore page.
     * Has a colored stripe instead of a thumbnail.
     */
    data class MoodContent(
        override val id: String,       // browseId for the mood/genre
        override val title: String,
        override val thumbnailUrl: String? = null,
        val stripeColor: Long = 0L,    // YTMusic's color for this mood
        val params: String? = null,    // Optional params for the browse endpoint
    ) : OnlineContentItem()
}

/**
 * Represents the home feed from YouTube Music with categorized sections.
 */
@Immutable
data class OnlineHomePage(
    val sections: List<OnlineHomeSection>,
    val continuation: String? = null,
)

@Immutable
data class OnlineHomeSection(
    val title: String,
    val label: String? = null,
    val thumbnailUrl: String? = null,
    val items: List<OnlineContentItem>,
)

/**
 * Represents a search result page with categorized items.
 */
@Immutable
data class OnlineSearchResult(
    val items: List<OnlineContentItem>,
    val continuation: String? = null,
)

/**
 * Represents the result of browsing a mood/genre page from YouTube Music.
 * Contains categorized playlist sections fetched via the browse endpoint.
 */
@Immutable
data class OnlineMoodDetail(
    val title: String?,
    val sections: List<OnlineMoodSection>,
)

@Immutable
data class OnlineMoodSection(
    val title: String?,
    val items: List<OnlineContentItem>,
)

/**
 * Represents an online YouTube Music playlist with its songs.
 * Used for the online playlist detail screen.
 */
@Immutable
data class OnlinePlaylistDetail(
    val id: String,
    val title: String,
    val author: MediaArtist? = null,
    val thumbnailUrl: String? = null,
    val songCount: String? = null,
    val songs: List<MediaMetadata> = emptyList(),
    val songsContinuation: String? = null,
    val continuation: String? = null,
)

/**
 * Represents a page of continuation results when loading more songs for an online playlist.
 */
@Immutable
data class OnlinePlaylistContinuation(
    val songs: List<MediaMetadata>,
    val continuation: String? = null,
)

// ---- MediaItem conversion (following Metrolist pattern) ----

/**
 * Converts [MediaMetadata] to [MediaItem] for playback.
 *
 * For online songs (id is YouTube video ID):
 * - URI is set to just the video ID (not a full URL)
 * - ResolvingDataSource will detect the ID and resolve it to a stream URL
 *
 * For local songs:
 * - URI is set to the contentUri if available
 */
fun MediaMetadata.toMediaItem(): MediaItem = MediaItem.Builder()
    .setMediaId(id)
    .setUri(
        // For online songs, use just the video ID as URI
        // ResolvingDataSource will extract mediaId from customCacheKey and resolve it
        // For local songs, use the contentUri
        if (isLocal && contentUri != null) {
            contentUri.toUri()
        } else {
            // Online song: just the video ID
            id.toUri()
        }
    )
    .setCustomCacheKey(id)  // Ensure cache key matches mediaId for ResolvingDataSource
    .setMediaMetadata(
        ExoPlayerMediaMetadata.Builder()
            .setTitle(title)
            .setArtist(displayArtist)
            .setAlbumTitle(album?.title)
            .setArtworkUri(thumbnailUrl?.toUri())
            .setExtras(Bundle().apply {
                thumbnailUrl?.let { putString("artwork_uri", it) }
            })
            .build()
    )
    .build()
