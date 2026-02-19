package com.jyotirmoy.musicly.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.jyotirmoy.musicly.data.model.MediaAlbum
import com.jyotirmoy.musicly.data.model.MediaArtist
import com.jyotirmoy.musicly.data.model.MediaMetadata

/**
 * Room entity for caching online songs (YouTube Music tracks).
 * Uses YouTube video ID as the primary key (String).
 * This is separate from the local [SongEntity] which uses MediaStore Long IDs.
 */
@Entity(
    tableName = "online_songs",
    indices = [
        Index(value = ["title"]),
        Index(value = ["album_id"]),
        Index(value = ["in_library"]),
        Index(value = ["last_played_timestamp"]),
    ]
)
data class OnlineSongEntity(
    @PrimaryKey
    @ColumnInfo(name = "id") val id: String, // YouTube video ID
    @ColumnInfo(name = "title") val title: String,
    /** Comma-separated artist names for display. */
    @ColumnInfo(name = "artists_text") val artistsText: String,
    /** JSON-encoded list of artist id/name pairs for structured access. */
    @ColumnInfo(name = "artists_json") val artistsJson: String,
    @ColumnInfo(name = "album_id") val albumId: String? = null,
    @ColumnInfo(name = "album_name") val albumName: String? = null,
    @ColumnInfo(name = "duration") val duration: Int? = null, // seconds
    @ColumnInfo(name = "thumbnail_url") val thumbnailUrl: String? = null,
    @ColumnInfo(name = "explicit") val explicit: Boolean = false,
    @ColumnInfo(name = "in_library") val inLibrary: Boolean = false,
    @ColumnInfo(name = "is_favorite") val isFavorite: Boolean = false,
    @ColumnInfo(name = "total_play_time_ms") val totalPlayTimeMs: Long = 0,
    @ColumnInfo(name = "play_count") val playCount: Int = 0,
    @ColumnInfo(name = "last_played_timestamp") val lastPlayedTimestamp: Long = 0,
    @ColumnInfo(name = "date_added") val dateAdded: Long = 0,
)

/**
 * Room entity for caching resolved audio stream format info.
 * Keyed by video ID. Cached to avoid re-resolving for recently played tracks.
 */
@Entity(
    tableName = "online_format_cache",
)
data class FormatCacheEntity(
    @PrimaryKey
    @ColumnInfo(name = "id") val id: String, // YouTube video ID
    @ColumnInfo(name = "itag") val itag: Int? = null,
    @ColumnInfo(name = "mime_type") val mimeType: String? = null,
    @ColumnInfo(name = "bitrate") val bitrate: Int? = null,
    @ColumnInfo(name = "sample_rate") val sampleRate: Int? = null,
    @ColumnInfo(name = "content_length") val contentLength: Long? = null,
    @ColumnInfo(name = "loudness_db") val loudnessDb: Float? = null,
    @ColumnInfo(name = "playback_url") val playbackUrl: String? = null,
    @ColumnInfo(name = "expires_at") val expiresAt: Long = 0, // timestamp when URL expires
    @ColumnInfo(name = "cached_at") val cachedAt: Long = System.currentTimeMillis(),
)

/**
 * Room entity for online search history (separate from local search history).
 */
@Entity(
    tableName = "online_search_history",
    indices = [
        Index(value = ["query"], unique = true),
    ]
)
data class OnlineSearchHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "query") val query: String,
    @ColumnInfo(name = "timestamp") val timestamp: Long = System.currentTimeMillis(),
)

// ---- Mappers ----

/**
 * Convert an [OnlineSongEntity] to [MediaMetadata].
 */
fun OnlineSongEntity.toMediaMetadata(): MediaMetadata {
    val parsedArtists = parseArtistsJson(artistsJson)
    return MediaMetadata(
        id = id,
        title = title,
        artists = parsedArtists,
        album = if (albumId != null && albumName != null) MediaAlbum(id = albumId, title = albumName) else null,
        duration = duration,
        thumbnailUrl = thumbnailUrl,
        explicit = explicit,
        isLocal = false,
    )
}

/**
 * Convert a [MediaMetadata] to [OnlineSongEntity] for caching.
 */
fun MediaMetadata.toOnlineSongEntity(): OnlineSongEntity {
    return OnlineSongEntity(
        id = id,
        title = title,
        artistsText = displayArtist,
        artistsJson = encodeArtistsJson(artists),
        albumId = album?.id,
        albumName = album?.title,
        duration = duration,
        thumbnailUrl = thumbnailUrl,
        explicit = explicit,
        dateAdded = System.currentTimeMillis(),
    )
}

// Simple JSON encoding/decoding for artist lists without requiring kotlinx.serialization in entity layer
private fun encodeArtistsJson(artists: List<MediaArtist>): String {
    return artists.joinToString("|") { "${it.id ?: "null"}::${it.name}" }
}

private fun parseArtistsJson(json: String): List<MediaArtist> {
    if (json.isBlank()) return emptyList()
    return json.split("|").mapNotNull { entry ->
        val parts = entry.split("::", limit = 2)
        if (parts.size == 2) {
            MediaArtist(
                id = parts[0].takeIf { it != "null" },
                name = parts[1]
            )
        } else null
    }
}
