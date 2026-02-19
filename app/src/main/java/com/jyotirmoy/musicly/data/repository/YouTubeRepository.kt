package com.jyotirmoy.musicly.data.repository

import com.jyotirmoy.musicly.data.database.FormatCacheEntity
import com.jyotirmoy.musicly.data.database.OnlineSongEntity
import com.jyotirmoy.musicly.data.model.MediaMetadata
import com.jyotirmoy.musicly.data.model.OnlineAlbumDetail
import com.jyotirmoy.musicly.data.model.OnlineArtistDetail
import com.jyotirmoy.musicly.data.model.OnlineHomePage
import com.jyotirmoy.musicly.data.model.OnlineMoodDetail
import com.jyotirmoy.musicly.data.model.OnlinePlaylistContinuation
import com.jyotirmoy.musicly.data.model.OnlinePlaylistDetail
import com.jyotirmoy.musicly.data.model.OnlineSearchResult
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.SearchSuggestions
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.YTItem
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for YouTube Music / online content.
 * Wraps the innertube [YouTube] API and provides local caching via Room.
 *
 * All network calls return [Result] to allow graceful error handling.
 */
interface YouTubeRepository {

    // ---- Search ----

    /** Get search suggestions for the given query. */
    suspend fun searchSuggestions(query: String): Result<SearchSuggestions>

    /** Search for songs matching the query. */
    suspend fun searchSongs(query: String): Result<OnlineSearchResult>

    /** Search for albums matching the query. */
    suspend fun searchAlbums(query: String): Result<OnlineSearchResult>

    /** Search for artists matching the query. */
    suspend fun searchArtists(query: String): Result<OnlineSearchResult>

    /** Search for playlists matching the query. */
    suspend fun searchPlaylists(query: String): Result<OnlineSearchResult>

    /** Load more search results from a continuation token. */
    suspend fun searchContinuation(continuation: String): Result<OnlineSearchResult>

    // ---- Content Details ----

    /** Get full album details including song list. */
    suspend fun getAlbumDetails(browseId: String): Result<OnlineAlbumDetail>

    /** Get artist page with sections (songs, albums, singles, etc.). */
    suspend fun getArtistDetails(browseId: String): Result<OnlineArtistDetail>

    // ---- Home / Explore ----

    /** Get the YouTube Music home feed. */
    suspend fun getHome(continuation: String? = null): Result<OnlineHomePage>

    /** Get the explore page (new releases, mood & genres). */
    suspend fun getExplorePage(): Result<OnlineHomePage>

    /** Browse a mood/genre page by browseId to get its playlists. */
    suspend fun browseMood(browseId: String, params: String? = null): Result<OnlineMoodDetail>

    /** Get online playlist details (songs, author, thumbnail, etc.). */
    suspend fun getOnlinePlaylistDetails(playlistId: String): Result<OnlinePlaylistDetail>

    /** Load more songs from an online playlist via continuation token. */
    suspend fun getOnlinePlaylistContinuation(continuation: String): Result<OnlinePlaylistContinuation>

    // ---- Playback ----

    /** Get or resolve the playback stream URL for a video. Returns cached if still valid. */
    suspend fun getStreamUrl(videoId: String): Result<String>

    /** Cache a resolved format for a video. */
    suspend fun cacheFormat(format: FormatCacheEntity)

    /** Get cached format if still valid. */
    suspend fun getCachedFormat(videoId: String): FormatCacheEntity?

    // ---- Local Cache (Online Songs) ----

    /** Cache an online song's metadata locally (e.g., after playing it). */
    suspend fun cacheOnlineSong(metadata: MediaMetadata)

    /** Get a cached online song by its video ID. */
    suspend fun getCachedSong(videoId: String): OnlineSongEntity?

    /** Observe recently played online songs. */
    fun observeRecentlyPlayed(limit: Int = 50): Flow<List<OnlineSongEntity>>

    /** Observe most played online songs. */
    fun observeMostPlayed(limit: Int = 50): Flow<List<OnlineSongEntity>>

    /** Record that a song was played. */
    suspend fun recordPlay(videoId: String, durationMs: Long)

    /** Toggle favorite status for an online song. */
    suspend fun toggleFavorite(videoId: String, isFavorite: Boolean)

    /** Observe online songs in library. */
    fun observeLibrarySongs(): Flow<List<OnlineSongEntity>>

    /** Observe favorite online songs. */
    fun observeFavoriteOnlineSongs(): Flow<List<OnlineSongEntity>>

    // ---- Online Search History ----

    /** Save a search query to online search history. */
    suspend fun saveSearchQuery(query: String)

    /** Observe recent online search history. */
    fun observeSearchHistory(limit: Int = 20): Flow<List<String>>

    /** Delete a specific search query from history. */
    suspend fun deleteSearchQuery(query: String)

    /** Clear all online search history. */
    suspend fun clearSearchHistory()

    // ---- Lyrics ----

    /** Get lyrics for a song by its browse endpoint. */
    suspend fun getLyrics(videoId: String): Result<String?>

    // ---- Related / Queue ----

    /** Get related songs for a video (for "Up Next" / autoplay). */
    suspend fun getRelated(videoId: String): Result<List<MediaMetadata>>
}
