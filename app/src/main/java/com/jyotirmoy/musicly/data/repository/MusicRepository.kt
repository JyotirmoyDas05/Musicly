package com.jyotirmoy.musicly.data.repository

import android.net.Uri
import androidx.paging.PagingData
import com.jyotirmoy.musicly.data.model.Album
import com.jyotirmoy.musicly.data.model.Artist
import com.jyotirmoy.musicly.data.model.Lyrics
import com.jyotirmoy.musicly.data.model.LyricsSourcePreference
import com.jyotirmoy.musicly.data.model.Playlist
import com.jyotirmoy.musicly.data.model.SearchFilterType
import com.jyotirmoy.musicly.data.model.SearchHistoryItem
import com.jyotirmoy.musicly.data.model.SearchResultItem
import com.jyotirmoy.musicly.data.model.Song
import kotlinx.coroutines.flow.Flow

interface MusicRepository {
    /**
     * Gets the list of audio files (songs) filtered by allowed directories.
     * @return Flow that emits a complete list of Song objects.
     */
    fun getAudioFiles(): Flow<List<Song>> // Existing Flow for reactive updates

    /**
     * Returns paginated songs for efficient display of large libraries.
     * @return Flow of PagingData<Song> for use with LazyPagingItems.
     */
    fun getPaginatedSongs(sortOption: com.jyotirmoy.musicly.data.model.SortOption): Flow<PagingData<Song>>

    /**
     * Returns paginated favorite songs for efficient display.
     * @return Flow of PagingData<Song> for use with LazyPagingItems.
     */
    fun getPaginatedFavoriteSongs(sortOption: com.jyotirmoy.musicly.data.model.SortOption): Flow<PagingData<Song>>

    /**
     * Returns all favorite songs as a list (for playback queue on shuffle).
     */
    suspend fun getFavoriteSongsOnce(): List<Song>

    /**
     * Returns the count of favorite songs (reactive).
     */
    fun getFavoriteSongCountFlow(): Flow<Int>

    /**
     * Returns the count of songs in the library.
     * @return Flow emitting the current song count.
     */
    fun getSongCountFlow(): Flow<Int>

    /**
     * Returns a random selection of songs for efficient shuffle.
     * Uses database-level RANDOM() for performance.
     * @param limit Maximum number of songs to return.
     * @return List of randomly selected songs.
     */
    suspend fun getRandomSongs(limit: Int): List<Song>

    /**
     * Gets the list of filtered albums.
     * @return Flow that emits a complete list of Album objects.
     */
    fun getAlbums(): Flow<List<Album>> // Existing Flow for reactive updates

    /**
     * Gets the filtered list of artists.
     * @return Flow that emits a complete list of Artist objects.
     */
    fun getArtists(): Flow<List<Artist>> // Existing Flow for reactive updates

    /**
     * Gets the complete list of songs once.
     * @return List of Song objects.
     */
    suspend fun getAllSongsOnce(): List<Song>

    /**
     * Gets the complete list of albums once.
     * @return List of Album objects.
     */
    suspend fun getAllAlbumsOnce(): List<Album>

    /**
     * Gets the complete list of artists once.
     * @return List of Artist objects.
     */
    suspend fun getAllArtistsOnce(): List<Artist>

    /**
     * Gets a specific album by its ID.
     * @param id The ID of the album.
     * @return Flow that emits the Album object or null if not found.
     */
    fun getAlbumById(id: Long): Flow<Album?>

    /**
     * Gets the filtered list of artists.
     * @return Flow that emits a complete list of Artist objects.
     */
    //fun getArtists(): Flow<List<Artist>>

    /**
     * Gets the list of songs for a specific album (NOT paginated for the playback queue).
     * @param albumId The ID of the album.
     * @return Flow that emits a list of Song objects belonging to the album.
     */
    fun getSongsForAlbum(albumId: Long): Flow<List<Song>>

    /**
     * Gets the list of songs for a specific artist (NOT paginated for the playback queue).
     * @param artistId The artist ID.
     * @return Flow that emits a list of Song objects belonging to the artist.
     */
    fun getSongsForArtist(artistId: Long): Flow<List<Song>>

    /**
     * Gets a list of songs by their IDs.
     * @param songIds List of song IDs.
     * @return Flow that emits a list of Song objects corresponding to the IDs, in the same order.
     */
    fun getSongsByIds(songIds: List<String>): Flow<List<Song>>

    /**
     * Gets a song by its file path.
     * @param path The file path.
     * @return The Song object or null if not found.
     */
    suspend fun getSongByPath(path: String): Song?

    /**
     * Gets all unique directories containing audio files.
     * This is mainly used for the initial directory setup.
     * It also manages the initial saving of allowed directories if it's the first time.
     * @return Set of unique directory paths.
     */
    suspend fun getAllUniqueAudioDirectories(): Set<String>

    fun getAllUniqueAlbumArtUris(): Flow<List<Uri>> // New for theme preloading

    suspend fun invalidateCachesDependentOnAllowedDirectories() // New for theme preloading

    fun searchSongs(query: String): Flow<List<Song>>
    fun searchAlbums(query: String): Flow<List<Album>>
    fun searchArtists(query: String): Flow<List<Artist>>
    suspend fun searchPlaylists(query: String): List<Playlist> // Keep suspend, since there is no Flow yet
    fun searchAll(query: String, filterType: SearchFilterType): Flow<List<SearchResultItem>>

    // Search History
    suspend fun addSearchHistoryItem(query: String)
    suspend fun addSearchHistoryItemObj(item: SearchHistoryItem)
    suspend fun getRecentSearchHistory(limit: Int): List<SearchHistoryItem>
    fun observeRecentSearchHistory(): Flow<List<SearchHistoryItem>>
    suspend fun deleteSearchHistoryItemByQuery(query: String)
    suspend fun clearSearchHistory()


    /**
     * Gets the list of songs for a specific genre (placeholder implementation).
     * @param genreId The genre ID (e.g., "pop", "rock").
     * @return Flow that emits a list of Song objects (simulated for this genre).
     */
    fun getMusicByGenre(genreId: String): Flow<List<Song>> // Changed to Flow

    /**
     * Toggles the favorite status of a song.
     * @param songId The song ID.
     * @return The new favorite status (true if it is favorite, false if not).
     */
    suspend fun toggleFavoriteStatus(songId: String): Boolean

    /**
     * Explicitly sets the favorite status of a song.
     * @param songId The song ID.
     * @param isFavorite Target state.
     */
    suspend fun setFavoriteStatus(songId: String, isFavorite: Boolean)

    /**
     * Gets favorite song IDs directly from Room (favorites table).
     */
    suspend fun getFavoriteSongIdsOnce(): Set<String>

    /**
     * Gets a specific song by its ID.
     * @param songId The song ID.
     * @return Flow that emits the Song object or null if not found.
     */
    fun getSong(songId: String): Flow<Song?>
    fun getArtistById(artistId: Long): Flow<Artist?>
    fun getArtistsForSong(songId: Long): Flow<List<Artist>>

    /**
     * Gets the list of genres, either mocked or read from metadata.
     * @return Flow that emits a list of Genre objects.
     */
    fun getGenres(): Flow<List<com.jyotirmoy.musicly.data.model.Genre>>

    suspend fun getLyrics(
        song: Song,
        sourcePreference: LyricsSourcePreference = LyricsSourcePreference.EMBEDDED_FIRST,
        forceRefresh: Boolean = false
    ): Lyrics?

    suspend fun getLyricsFromRemote(song: Song): Result<Pair<Lyrics, String>>

    /**
     * Search for lyrics remotely, less specific than `getLyricsFromRemote` but more lenient
     * @param song The song to search lyrics for
     * @return The search query and the results
     */
    suspend fun searchRemoteLyrics(song: Song): Result<Pair<String, List<LyricsSearchResult>>>

    /**
     * Search for lyrics remotely using query provided, and not use song metadata
     * @param query The query for searching, typically song title and artist name
     * @return The search query and the results
     */
    suspend fun searchRemoteLyricsByQuery(title: String, artist: String? = null): Result<Pair<String, List<LyricsSearchResult>>>

    suspend fun updateLyrics(songId: Long, lyrics: String)

    suspend fun resetLyrics(songId: Long)

    suspend fun resetAllLyrics()

    fun getMusicFolders(): Flow<List<com.jyotirmoy.musicly.data.model.MusicFolder>>

    /**
     * Returns a merged list of local audio files and explicitly downloaded online songs.
     */
    fun getDownloadedSongs(): Flow<List<Song>>

    /**
     * Returns a list of songs currently present in the streaming cache.
     */
    fun getCachedSongs(): Flow<List<Song>>

    suspend fun deleteById(id: Long)
}
