package com.jyotirmoy.musicly.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.jyotirmoy.musicly.data.database.MusicDao
import com.jyotirmoy.musicly.data.database.SearchHistoryDao
import com.jyotirmoy.musicly.data.database.SearchHistoryEntity
import com.jyotirmoy.musicly.data.database.toEntity
import com.jyotirmoy.musicly.data.database.toSearchHistoryItem
import com.jyotirmoy.musicly.data.database.toSong
import com.jyotirmoy.musicly.data.database.toAlbum
import com.jyotirmoy.musicly.data.database.toArtist
import com.jyotirmoy.musicly.data.database.OnlineDao
import com.jyotirmoy.musicly.data.database.toSong as onlineToSong
import com.jyotirmoy.musicly.data.model.Album
import com.jyotirmoy.musicly.data.model.Artist
import com.jyotirmoy.musicly.data.model.Genre
import com.jyotirmoy.musicly.data.model.Lyrics
import com.jyotirmoy.musicly.data.model.LyricsSourcePreference
import com.jyotirmoy.musicly.data.model.MusicFolder
import com.jyotirmoy.musicly.data.model.Playlist
import com.jyotirmoy.musicly.data.model.SearchFilterType
import com.jyotirmoy.musicly.data.model.SearchHistoryItem
import com.jyotirmoy.musicly.data.model.SearchResultItem
import com.jyotirmoy.musicly.data.model.Song
import com.jyotirmoy.musicly.data.model.SortOption
import com.jyotirmoy.musicly.data.preferences.UserPreferencesRepository
import com.jyotirmoy.musicly.utils.LogUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import androidx.media3.datasource.cache.SimpleCache
import com.jyotirmoy.musicly.di.DownloadCache
import com.jyotirmoy.musicly.di.PlayerCache
import kotlinx.coroutines.delay

@Singleton
class MusicRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val searchHistoryDao: SearchHistoryDao,
    private val musicDao: MusicDao,
    private val lyricsRepository: LyricsRepository,
    private val songRepository: SongRepository,
    private val favoritesDao: com.jyotirmoy.musicly.data.database.FavoritesDao,
    private val onlineDao: OnlineDao,
    private val artistImageRepository: ArtistImageRepository,
    private val folderTreeBuilder: FolderTreeBuilder,
    @PlayerCache private val playerCache: SimpleCache,
    @DownloadCache private val downloadCache: SimpleCache
) : MusicRepository {

    override fun getAudioFiles(): Flow<List<Song>> = songRepository.getSongs()

    override fun getPaginatedSongs(sortOption: SortOption): Flow<PagingData<Song>> {
        return combine(
            userPreferencesRepository.allowedDirectoriesFlow,
            userPreferencesRepository.isFolderFilterActiveFlow
        ) { allowedDirs, filterActive ->
            allowedDirs to filterActive
        }.flatMapLatest { pair ->
            val allowedDirs = pair.first
            val filterActive = pair.second
            Pager(
                config = PagingConfig(pageSize = 50, enablePlaceholders = true),
                pagingSourceFactory = { musicDao.getSongsPaginated(allowedDirs.toList(), filterActive, sortOption.storageKey) }
            ).flow.map { pagingData -> pagingData.map { it.toSong() } }
        }
    }

    override fun getPaginatedFavoriteSongs(sortOption: SortOption): Flow<PagingData<Song>> {
        return combine(
            userPreferencesRepository.allowedDirectoriesFlow,
            userPreferencesRepository.isFolderFilterActiveFlow,
            onlineDao.observeFavoriteSongs(),
            musicDao.getFavoriteSongsListFlow(emptyList(), false) // Dummy flow to trigger on local db changes
        ) { allowedDirs, filterActive, onlineFavorites, _ ->
            // Fetch local favorites directly
            val localFavorites = musicDao.getFavoriteSongsList(allowedDirs.toList(), filterActive).map { it.toSong() }

            val onlineSongs = onlineFavorites.map { it.onlineToSong() }

            // Deduping: remote online songs which are already matched to local favorites.
            // (Note: Local ids are Long, while Online ids are String. Since they don't overlap,
            // we will just combine them. We sort the list together.)
            val combined = (localFavorites + onlineSongs).sortedByDescending { it.dateAdded }

            PagingData.from(combined)
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun getFavoriteSongsOnce(): List<Song> = withContext(Dispatchers.IO) {
        val allowedDirs = userPreferencesRepository.allowedDirectoriesFlow.first()
        val filterActive = userPreferencesRepository.isFolderFilterActiveFlow.first()
        musicDao.getFavoriteSongsList(allowedDirs.toList(), filterActive).map { it.toSong() }
    }

    override fun getFavoriteSongCountFlow(): Flow<Int> {
        return combine(
            userPreferencesRepository.allowedDirectoriesFlow,
            userPreferencesRepository.isFolderFilterActiveFlow
        ) { allowedDirs, filterActive ->
            musicDao.getFavoriteSongCount(allowedDirs.toList(), filterActive)
        }.flatMapLatest { it }
    }

    override fun getSongCountFlow(): Flow<Int> = musicDao.getSongCount()

    override suspend fun getRandomSongs(limit: Int): List<Song> = withContext(Dispatchers.IO) {
        val allowedDirs = userPreferencesRepository.allowedDirectoriesFlow.first()
        val filterActive = userPreferencesRepository.isFolderFilterActiveFlow.first()
        musicDao.getRandomSongs(limit, allowedDirs.toList(), filterActive).map { it.toSong() }
    }

    override fun getAlbums(): Flow<List<Album>> {
        return combine(
            userPreferencesRepository.allowedDirectoriesFlow,
            userPreferencesRepository.isFolderFilterActiveFlow
        ) { allowedDirs, filterActive ->
            musicDao.getAlbums(allowedDirs.toList(), filterActive).map { albums ->
                albums.map { it.toAlbum() }
            }
        }.flatMapLatest { it }.flowOn(Dispatchers.IO)
    }

    override fun getArtists(): Flow<List<Artist>> {
        return combine(
            userPreferencesRepository.allowedDirectoriesFlow,
            userPreferencesRepository.isFolderFilterActiveFlow
        ) { allowedDirs, filterActive ->
            musicDao.getArtistsWithSongCountsFiltered(allowedDirs.toList(), filterActive).map { artists ->
                artists.map { it.toArtist() }
            }
        }.flatMapLatest { it }.flowOn(Dispatchers.IO)
    }

    override fun getAlbumById(id: Long): Flow<Album?> {
        return musicDao.getAlbumById(id).map { it?.toAlbum() }.flowOn(Dispatchers.IO)
    }

    override fun getSongsForAlbum(albumId: Long): Flow<List<Song>> {
        return musicDao.getSongsByAlbumId(albumId).map { it.map { entity -> entity.toSong() } }.flowOn(Dispatchers.IO)
    }

    override fun getSongsForArtist(artistId: Long): Flow<List<Song>> {
        return musicDao.getSongsByArtistId(artistId).map { it.map { entity -> entity.toSong() } }.flowOn(Dispatchers.IO)
    }

    override suspend fun getAllUniqueAudioDirectories(): Set<String> {
        return withContext(Dispatchers.IO) {
            musicDao.getDistinctParentDirectories().toSet()
        }
    }

    override fun getAllUniqueAlbumArtUris(): Flow<List<Uri>> {
        return musicDao.getAllUniqueAlbumArtUrisFromSongs().map { list -> list.mapNotNull { Uri.parse(it) } }
    }

    override fun searchSongs(query: String): Flow<List<Song>> {
        return combine(
            userPreferencesRepository.allowedDirectoriesFlow,
            userPreferencesRepository.isFolderFilterActiveFlow
        ) { allowedDirs, filterActive ->
            musicDao.searchSongs(query, allowedDirs.toList(), filterActive).map { list -> list.map { it.toSong() } }
        }.flatMapLatest { it }.flowOn(Dispatchers.IO)
    }

    override fun searchAlbums(query: String): Flow<List<Album>> {
        return combine(
            userPreferencesRepository.allowedDirectoriesFlow,
            userPreferencesRepository.isFolderFilterActiveFlow
        ) { allowedDirs, filterActive ->
            musicDao.searchAlbums(query, allowedDirs.toList(), filterActive).map { list -> list.map { it.toAlbum() } }
        }.flatMapLatest { it }.flowOn(Dispatchers.IO)
    }

    override fun searchArtists(query: String): Flow<List<Artist>> {
        return combine(
            userPreferencesRepository.allowedDirectoriesFlow,
            userPreferencesRepository.isFolderFilterActiveFlow
        ) { allowedDirs, filterActive ->
            musicDao.searchArtists(query, allowedDirs.toList(), filterActive).map { list -> list.map { it.toArtist() } }
        }.flatMapLatest { it }.flowOn(Dispatchers.IO)
    }

    override suspend fun searchPlaylists(query: String): List<Playlist> {
        return emptyList() // Not implemented yet
    }

    override fun searchAll(query: String, filterType: SearchFilterType): Flow<List<SearchResultItem>> {
        if (query.isBlank()) return flowOf(emptyList())

        val playlistsFlow = flowOf(emptyList<Playlist>())

        return when (filterType) {
            SearchFilterType.ALL -> {
                combine(
                    searchSongs(query),
                    searchAlbums(query),
                    searchArtists(query),
                    playlistsFlow
                ) { songs, albums, artists, playlists ->
                    mutableListOf<SearchResultItem>().apply {
                        songs.forEach { add(SearchResultItem.SongItem(it)) }
                        albums.forEach { add(SearchResultItem.AlbumItem(it)) }
                        artists.forEach { add(SearchResultItem.ArtistItem(it)) }
                        playlists.forEach { add(SearchResultItem.PlaylistItem(it)) }
                    }
                }
            }
            SearchFilterType.SONGS -> searchSongs(query).map { songs -> songs.map { SearchResultItem.SongItem(it) } }
            SearchFilterType.ALBUMS -> searchAlbums(query).map { albums -> albums.map { SearchResultItem.AlbumItem(it) } }
            SearchFilterType.ARTISTS -> searchArtists(query).map { artists -> artists.map { SearchResultItem.ArtistItem(it) } }
            SearchFilterType.PLAYLISTS -> playlistsFlow.map { playlists -> playlists.map { SearchResultItem.PlaylistItem(it) } }
        }.flowOn(Dispatchers.Default)
    }

    override suspend fun addSearchHistoryItem(query: String) {
        withContext(Dispatchers.IO) {
            searchHistoryDao.deleteByQuery(query)
            searchHistoryDao.insert(SearchHistoryEntity(query = query, timestamp = System.currentTimeMillis()))
        }
    }

    override suspend fun addSearchHistoryItemObj(item: SearchHistoryItem) {
        withContext(Dispatchers.IO) {
            if (item.itemType == "query") {
                searchHistoryDao.deleteByQuery(item.query)
            } else if (item.itemId != null) {
                // Delete existing entry for this specific item ID to move it to top
                searchHistoryDao.deleteByItemId(item.itemId)
            } else {
                // Fallback
                searchHistoryDao.deleteByQuery(item.query)
            }
            // Always insert as a fresh entry with no ID (Room will auto-generate)
            searchHistoryDao.insert(item.copy(id = null, timestamp = System.currentTimeMillis()).toEntity())
        }
    }

    override suspend fun getRecentSearchHistory(limit: Int): List<SearchHistoryItem> {
        return withContext(Dispatchers.IO) {
            searchHistoryDao.getRecentSearches(limit).map { it.toSearchHistoryItem() }
        }
    }

    override fun observeRecentSearchHistory(): Flow<List<SearchHistoryItem>> {
        return searchHistoryDao.observeAll().map { entities ->
            entities.map { it.toSearchHistoryItem() }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun deleteSearchHistoryItemByQuery(query: String) {
        withContext(Dispatchers.IO) {
            searchHistoryDao.deleteByQuery(query)
        }
    }

    override suspend fun clearSearchHistory() {
        withContext(Dispatchers.IO) {
            searchHistoryDao.clearAll()
        }
    }

    override fun getMusicByGenre(genreId: String): Flow<List<Song>> {
        return userPreferencesRepository.mockGenresEnabledFlow.flatMapLatest { mockEnabled ->
            if (mockEnabled) {
                val genreName = "Mock"
                songRepository.getSongs().map { songs ->
                    songs.filter { it.genre.equals(genreName, ignoreCase = true) }
                }
            } else {
                combine(
                    userPreferencesRepository.allowedDirectoriesFlow,
                    userPreferencesRepository.isFolderFilterActiveFlow
                ) { allowedDirs, filterActive ->
                    if (genreId.equals("unknown", ignoreCase = true)) {
                        musicDao.getSongsWithNullGenre(allowedDirs.toList(), filterActive)
                    } else {
                        musicDao.getSongsByGenre(genreId, allowedDirs.toList(), filterActive)
                    }
                }.flatMapLatest { it }.map { list -> list.map { it.toSong() } }
            }
        }.flowOn(Dispatchers.IO)
    }

    override fun getSongsByIds(songIds: List<String>): Flow<List<Song>> {
        if (songIds.isEmpty()) return flowOf(emptyList())
        val longIds = songIds.mapNotNull { it.toLongOrNull() }
        if (longIds.isEmpty()) return flowOf(emptyList())
        
        return combine(
            userPreferencesRepository.allowedDirectoriesFlow,
            userPreferencesRepository.isFolderFilterActiveFlow
        ) { allowedDirs, filterActive ->
            musicDao.getSongsByIds(longIds, allowedDirs.toList(), filterActive)
        }.flatMapLatest { it }.map { entities ->
            val songMap = entities.associate { it.id.toString() to it.toSong() }
            songIds.mapNotNull { songMap[it] }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun getSongByPath(path: String): Song? {
        return withContext(Dispatchers.IO) {
            musicDao.getSongByPath(path)?.toSong()
        }
    }

    override suspend fun invalidateCachesDependentOnAllowedDirectories() {
        Log.i("MusicRepo", "invalidateCachesDependentOnAllowedDirectories called. Reactive flows will update automatically.")
    }

    override suspend fun getAllSongsOnce(): List<Song> = withContext(Dispatchers.IO) {
        val allowedDirs = userPreferencesRepository.allowedDirectoriesFlow.first()
        val filterActive = userPreferencesRepository.isFolderFilterActiveFlow.first()
        musicDao.getAllSongs(allowedDirs.toList(), filterActive).first().map { it.toSong() }
    }

    override suspend fun getAllAlbumsOnce(): List<Album> = withContext(Dispatchers.IO) {
        val allowedDirs = userPreferencesRepository.allowedDirectoriesFlow.first()
        val filterActive = userPreferencesRepository.isFolderFilterActiveFlow.first()
        musicDao.getAllAlbumsList(allowedDirs.toList(), filterActive).map { it.toAlbum() }
    }

    override suspend fun getAllArtistsOnce(): List<Artist> = withContext(Dispatchers.IO) {
        val allowedDirs = userPreferencesRepository.allowedDirectoriesFlow.first()
        val filterActive = userPreferencesRepository.isFolderFilterActiveFlow.first()
        musicDao.getArtists(allowedDirs.toList(), filterActive).first().map { it.toArtist() }
    }

    override suspend fun setFavoriteStatus(songId: String, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        val id = songId.toLongOrNull() ?: return@withContext
        if (isFavorite) {
            favoritesDao.setFavorite(com.jyotirmoy.musicly.data.database.FavoritesEntity(songId = id, isFavorite = true))
        } else {
            favoritesDao.removeFavorite(id)
        }
        musicDao.setFavoriteStatus(id, isFavorite)
    }

    override suspend fun getFavoriteSongIdsOnce(): Set<String> = withContext(Dispatchers.IO) {
        favoritesDao.getFavoriteSongIdsOnce().map { it.toString() }.toSet()
    }

    override suspend fun toggleFavoriteStatus(songId: String): Boolean = withContext(Dispatchers.IO) {
        val id = songId.toLongOrNull() ?: return@withContext false
        val isFav = favoritesDao.isFavorite(id) ?: false
        val newFav = !isFav
        setFavoriteStatus(songId, newFav)
        return@withContext newFav
    }

    override fun getSong(songId: String): Flow<Song?> {
        val id = songId.toLongOrNull() ?: return flowOf(null)
        return musicDao.getSongById(id).map { it?.toSong() }.flowOn(Dispatchers.IO)
    }

    override fun getArtistById(artistId: Long): Flow<Artist?> {
        return musicDao.getArtistById(artistId).map { it?.toArtist() }.flowOn(Dispatchers.IO)
    }

    override fun getArtistsForSong(songId: Long): Flow<List<Artist>> {
        return musicDao.getArtistsForSong(songId).map { it.map { entity -> entity.toArtist() } }.flowOn(Dispatchers.IO)
    }

    override fun getGenres(): Flow<List<Genre>> {
        return songRepository.getSongs().map { songs ->
            val genresMap = songs.groupBy { song ->
                song.genre?.trim()?.takeIf { it.isNotBlank() } ?: "Unknown"
            }

            val dynamicGenres = genresMap.keys.mapNotNull { genreName ->
                val id = if (genreName.equals("Unknown", ignoreCase = true)) {
                    "unknown"
                } else {
                    genreName.lowercase().replace(" ", "_").replace("/", "_")
                }
                val colorInt = genreName.hashCode()
                val lightColorHex = "#${(colorInt and 0x00FFFFFF).toString(16).padStart(6, '0').uppercase()}"
                val darkColorHex = "#${((colorInt xor 0xFFFFFF) and 0x00FFFFFF).toString(16).padStart(6, '0').uppercase()}"

                Genre(
                    id = id,
                    name = genreName,
                    lightColorHex = lightColorHex,
                    onLightColorHex = "#000000",
                    darkColorHex = darkColorHex,
                    onDarkColorHex = "#FFFFFF"
                )
            }.sortedBy { it.name.lowercase() }

            val unknownGenre = dynamicGenres.find { it.id == "unknown" }
            if (unknownGenre != null) {
                (dynamicGenres.filterNot { it.id == "unknown" } + unknownGenre)
            } else {
                dynamicGenres
            }
        }.conflate().flowOn(Dispatchers.IO)
    }

    override suspend fun getLyrics(song: Song, sourcePreference: LyricsSourcePreference, forceRefresh: Boolean): Lyrics? {
        return lyricsRepository.getLyrics(song, sourcePreference, forceRefresh)
    }

    override suspend fun getLyricsFromRemote(song: Song): Result<Pair<Lyrics, String>> {
        return lyricsRepository.fetchFromRemote(song)
    }

    override suspend fun searchRemoteLyrics(song: Song): Result<Pair<String, List<LyricsSearchResult>>> {
        return lyricsRepository.searchRemote(song)
    }

    override suspend fun searchRemoteLyricsByQuery(title: String, artist: String?): Result<Pair<String, List<LyricsSearchResult>>> {
        return lyricsRepository.searchRemoteByQuery(title, artist)
    }

    override suspend fun updateLyrics(songId: Long, lyrics: String) {
        lyricsRepository.updateLyrics(songId, lyrics)
    }

    override suspend fun resetLyrics(songId: Long) {
        lyricsRepository.resetLyrics(songId)
    }

    override suspend fun resetAllLyrics() {
        lyricsRepository.resetAllLyrics()
    }

    override fun getMusicFolders(): Flow<List<MusicFolder>> {
        return combine(
            songRepository.getSongs(),
            userPreferencesRepository.allowedDirectoriesFlow,
            userPreferencesRepository.blockedDirectoriesFlow,
            userPreferencesRepository.isFolderFilterActiveFlow,
            userPreferencesRepository.foldersSourceFlow
        ) { songs, allowedDirs, blockedDirs, isFolderFilterActive, folderSource ->
            folderTreeBuilder.buildFolderTree(
                songs = songs,
                allowedDirs = allowedDirs,
                blockedDirs = blockedDirs,
                isFolderFilterActive = isFolderFilterActive,
                folderSource = folderSource,
                context = context
            )
        }.conflate().flowOn(Dispatchers.IO)
    }

    override fun getDownloadedSongs(): Flow<List<Song>> {
        return combine(
            songRepository.getSongs(),
            onlineDao.observeDownloadedSongs()
        ) { localSongs, onlineDownloadedEntites ->
            val onlineSongs = onlineDownloadedEntites.map { it.onlineToSong() }
            (localSongs + onlineSongs).sortedBy { it.title.lowercase() }
        }.flowOn(Dispatchers.Default)
    }

    override fun getCachedSongs(): Flow<List<Song>> = kotlinx.coroutines.flow.flow {
        while (true) {
            val cachedIds = playerCache.keys.toSet()
            val downloadedIds = downloadCache.keys.toSet()
            val pureCacheIds = cachedIds.subtract(downloadedIds)

            if (pureCacheIds.isNotEmpty()) {
                val songs = onlineDao.getOnlineSongsByIds(pureCacheIds.toList())
                val completeSongs = songs.filter { ptr ->
                    // Technically we don't have lengths stored explicitly, so we just assume fully cached or partially
                    // Since ExoPlayer retains it, we just accept it as cached.
                    playerCache.isCached(ptr.id, 0, 1) // Just check if anything is cached
                }.map { it.onlineToSong() }
                emit(completeSongs)
            } else {
                emit(emptyList())
            }

            delay(2000)
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun deleteById(id: Long) {
        musicDao.deleteById(id)
    }
}
