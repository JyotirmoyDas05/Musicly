package com.jyotirmoy.musicly.data.repository

import android.content.Context
import android.net.ConnectivityManager
import com.jyotirmoy.musicly.data.database.FormatCacheEntity
import com.jyotirmoy.musicly.data.database.OnlineDao
import com.jyotirmoy.musicly.data.database.OnlineSearchHistoryEntity
import com.jyotirmoy.musicly.data.database.OnlineSongEntity
import com.jyotirmoy.musicly.data.database.toMediaMetadata
import com.jyotirmoy.musicly.data.database.toOnlineSongEntity
import com.jyotirmoy.musicly.data.model.MediaAlbum
import com.jyotirmoy.musicly.data.model.MediaArtist
import com.jyotirmoy.musicly.data.model.MediaMetadata
import com.jyotirmoy.musicly.data.model.OnlineAlbumDetail
import com.jyotirmoy.musicly.data.model.OnlineArtistDetail
import com.jyotirmoy.musicly.data.model.OnlineArtistSection
import com.jyotirmoy.musicly.data.model.OnlineContentItem
import com.jyotirmoy.musicly.data.model.OnlineHomePage
import com.jyotirmoy.musicly.data.model.OnlineHomeSection
import com.jyotirmoy.musicly.data.model.OnlineMoodDetail
import com.jyotirmoy.musicly.data.model.OnlineMoodSection
import com.jyotirmoy.musicly.data.model.OnlinePlaylistContinuation
import com.jyotirmoy.musicly.data.model.OnlinePlaylistDetail
import com.jyotirmoy.musicly.data.model.OnlineSearchResult
import com.jyotirmoy.musicly.data.model.toMediaMetadata
import com.jyotirmoy.musicly.data.preferences.AudioQuality
import com.jyotirmoy.musicly.utils.YTPlayerUtils
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.SearchSuggestions
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.YTItem
import com.metrolist.innertube.models.Artist as InnertubeArtist
import com.metrolist.innertube.models.BrowseEndpoint
import com.metrolist.innertube.models.WatchEndpoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [YouTubeRepository] that wraps the innertube [YouTube] API
 * and uses [OnlineDao] for local caching.
 */
@Singleton
class YouTubeRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val onlineDao: OnlineDao,
) : YouTubeRepository {

    private val connectivityManager: ConnectivityManager by lazy {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    /**
     * Current audio quality preference. Should be kept in sync with user settings.
     */
    @Volatile
    var audioQuality: AudioQuality = AudioQuality.AUTO

    // ---- Search ----

    override suspend fun searchSuggestions(query: String): Result<SearchSuggestions> {
        return YouTube.searchSuggestions(query)
    }

    override suspend fun searchSongs(query: String): Result<OnlineSearchResult> {
        return YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).map { result ->
            OnlineSearchResult(
                items = result.items.map { it.toOnlineContentItem() },
                continuation = result.continuation,
            )
        }
    }

    override suspend fun searchAlbums(query: String): Result<OnlineSearchResult> {
        return YouTube.search(query, YouTube.SearchFilter.FILTER_ALBUM).map { result ->
            OnlineSearchResult(
                items = result.items.map { it.toOnlineContentItem() },
                continuation = result.continuation,
            )
        }
    }

    override suspend fun searchArtists(query: String): Result<OnlineSearchResult> {
        return YouTube.search(query, YouTube.SearchFilter.FILTER_ARTIST).map { result ->
            OnlineSearchResult(
                items = result.items.map { it.toOnlineContentItem() },
                continuation = result.continuation,
            )
        }
    }

    override suspend fun searchPlaylists(query: String): Result<OnlineSearchResult> {
        return YouTube.search(query, YouTube.SearchFilter.FILTER_COMMUNITY_PLAYLIST).map { result ->
            OnlineSearchResult(
                items = result.items.map { it.toOnlineContentItem() },
                continuation = result.continuation,
            )
        }
    }

    override suspend fun searchContinuation(continuation: String): Result<OnlineSearchResult> {
        return YouTube.searchContinuation(continuation).map { result ->
            OnlineSearchResult(
                items = result.items.map { it.toOnlineContentItem() },
                continuation = result.continuation,
            )
        }
    }

    // ---- Content Details ----

    override suspend fun getAlbumDetails(browseId: String): Result<OnlineAlbumDetail> {
        return YouTube.album(browseId, withSongs = true).map { albumPage ->
            OnlineAlbumDetail(
                browseId = albumPage.album.browseId,
                playlistId = albumPage.album.playlistId,
                title = albumPage.album.title,
                artists = albumPage.album.artists?.map { MediaArtist(id = it.id, name = it.name) } ?: emptyList(),
                year = albumPage.album.year,
                thumbnailUrl = albumPage.album.thumbnail,
                songs = albumPage.songs.map { it.toMediaMetadata() },
                otherVersions = albumPage.otherVersions.map { otherAlbum ->
                    OnlineAlbumDetail(
                        browseId = otherAlbum.browseId,
                        playlistId = otherAlbum.playlistId,
                        title = otherAlbum.title,
                        artists = otherAlbum.artists?.map { MediaArtist(id = it.id, name = it.name) } ?: emptyList(),
                        year = otherAlbum.year,
                        thumbnailUrl = otherAlbum.thumbnail,
                    )
                },
            )
        }
    }

    override suspend fun getArtistDetails(browseId: String): Result<OnlineArtistDetail> {
        return YouTube.artist(browseId).map { artistPage ->
            OnlineArtistDetail(
                browseId = artistPage.artist.id,
                name = artistPage.artist.title,
                thumbnailUrl = artistPage.artist.thumbnail,
                description = artistPage.description,
                subscriberCountText = artistPage.subscriberCountText,
                sections = artistPage.sections.map { section ->
                    OnlineArtistSection(
                        title = section.title,
                        items = section.items.map { it.toOnlineContentItem() },
                        moreBrowseId = section.moreEndpoint?.browseId,
                    )
                },
            )
        }
    }

    // ---- Home / Explore ----

    override suspend fun getHome(continuation: String?): Result<OnlineHomePage> {
        return YouTube.home(continuation).map { homePage ->
            OnlineHomePage(
                sections = homePage.sections.map { section ->
                    OnlineHomeSection(
                        title = section.title,
                        label = section.label,
                        thumbnailUrl = section.thumbnail,
                        items = section.items.map { it.toOnlineContentItem() },
                    )
                },
                continuation = homePage.continuation,
            )
        }
    }

    override suspend fun getExplorePage(): Result<OnlineHomePage> {
        return YouTube.explore().map { explorePage ->
            val sections = mutableListOf<OnlineHomeSection>()

            if (explorePage.newReleaseAlbums.isNotEmpty()) {
                sections.add(
                    OnlineHomeSection(
                        title = "New Releases",
                        items = explorePage.newReleaseAlbums.map { it.toOnlineContentItem() },
                    )
                )
            }

            if (explorePage.moodAndGenres.isNotEmpty()) {
                // Mood & genres items have colored stripes and browse endpoints
                sections.add(
                    OnlineHomeSection(
                        title = "Moods & Genres",
                        items = explorePage.moodAndGenres.map { moodItem ->
                            OnlineContentItem.MoodContent(
                                id = moodItem.endpoint.browseId,
                                title = moodItem.title,
                                stripeColor = moodItem.stripeColor,
                                params = moodItem.endpoint.params,
                            )
                        },
                    )
                )
            }

            OnlineHomePage(sections = sections)
        }
    }

    override suspend fun browseMood(browseId: String, params: String?): Result<OnlineMoodDetail> {
        return YouTube.browse(browseId, params).map { browseResult ->
            OnlineMoodDetail(
                title = browseResult.title,
                sections = browseResult.items.map { section ->
                    OnlineMoodSection(
                        title = section.title,
                        items = section.items.map { it.toOnlineContentItem() },
                    )
                },
            )
        }
    }

    override suspend fun getOnlinePlaylistDetails(playlistId: String): Result<OnlinePlaylistDetail> {
        return YouTube.playlist(playlistId).map { playlistPage ->
            OnlinePlaylistDetail(
                id = playlistPage.playlist.id,
                title = playlistPage.playlist.title,
                author = playlistPage.playlist.author?.let {
                    MediaArtist(id = it.id, name = it.name)
                },
                thumbnailUrl = playlistPage.playlist.thumbnail,
                songCount = playlistPage.playlist.songCountText,
                songs = playlistPage.songs.map { it.toMediaMetadata() },
                songsContinuation = playlistPage.songsContinuation,
                continuation = playlistPage.continuation,
            )
        }
    }

    override suspend fun getOnlinePlaylistContinuation(continuation: String): Result<OnlinePlaylistContinuation> {
        return YouTube.playlistContinuation(continuation).map { continuationPage ->
            OnlinePlaylistContinuation(
                songs = continuationPage.songs.map { it.toMediaMetadata() },
                continuation = continuationPage.continuation,
            )
        }
    }

    // ---- Playback ----

    override suspend fun getStreamUrl(videoId: String): Result<String> {
        // Check format cache first for a non-expired URL
        val cached = onlineDao.getValidFormatCache(videoId)
        if (cached?.playbackUrl != null && cached.expiresAt > System.currentTimeMillis()) {
            Timber.tag("YouTubeRepo").d("Returning cached stream URL for %s", videoId)
            return Result.success(cached.playbackUrl)
        }

        // Resolve via YTPlayerUtils (the same path used by the MediaSourceFactory resolver)
        return runCatching {
            val playbackData = YTPlayerUtils.playerResponseForPlayback(
                videoId = videoId,
                audioQuality = audioQuality,
                connectivityManager = connectivityManager,
            ).getOrThrow()

            // Persist format metadata to Room
            val format = playbackData.format
            onlineDao.upsertFormatCache(
                FormatCacheEntity(
                    id = videoId,
                    itag = format.itag,
                    mimeType = format.mimeType,
                    bitrate = format.bitrate,
                    sampleRate = format.audioSampleRate,
                    contentLength = format.contentLength,
                    loudnessDb = playbackData.audioConfig?.loudnessDb?.toFloat(),
                    playbackUrl = playbackData.streamUrl,
                    expiresAt = System.currentTimeMillis() + (playbackData.streamExpiresInSeconds * 1000L),
                )
            )

            playbackData.streamUrl
        }
    }

    override suspend fun cacheFormat(format: FormatCacheEntity) {
        onlineDao.upsertFormatCache(format)
    }

    override suspend fun getCachedFormat(videoId: String): FormatCacheEntity? {
        return onlineDao.getValidFormatCache(videoId)
    }

    // ---- Local Cache ----

    override suspend fun cacheOnlineSong(metadata: MediaMetadata) {
        onlineDao.insertOnlineSongIfAbsent(metadata.toOnlineSongEntity())
    }

    override suspend fun getCachedSong(videoId: String): OnlineSongEntity? {
        return onlineDao.getOnlineSong(videoId)
    }

    override fun observeRecentlyPlayed(limit: Int): Flow<List<OnlineSongEntity>> {
        return onlineDao.observeRecentlyPlayed(limit)
    }

    override fun observeMostPlayed(limit: Int): Flow<List<OnlineSongEntity>> {
        return onlineDao.observeMostPlayed(limit)
    }

    override suspend fun recordPlay(videoId: String, durationMs: Long) {
        onlineDao.recordOnlinePlay(videoId, durationMs)
    }

    override suspend fun toggleFavorite(videoId: String, isFavorite: Boolean) {
        onlineDao.setOnlineFavorite(videoId, isFavorite)
    }

    override fun observeLibrarySongs(): Flow<List<OnlineSongEntity>> {
        return onlineDao.observeLibrarySongs()
    }

    override fun observeFavoriteOnlineSongs(): Flow<List<OnlineSongEntity>> {
        return onlineDao.observeFavoriteSongs()
    }

    // ---- Online Search History ----

    override suspend fun saveSearchQuery(query: String) {
        onlineDao.insertOnlineSearchHistory(
            OnlineSearchHistoryEntity(query = query, timestamp = System.currentTimeMillis())
        )
    }

    override fun observeSearchHistory(limit: Int): Flow<List<String>> {
        return onlineDao.observeOnlineSearchHistory(limit).map { entries ->
            entries.map { it.query }
        }
    }

    override suspend fun deleteSearchQuery(query: String) {
        onlineDao.deleteOnlineSearchHistoryByQuery(query)
    }

    override suspend fun clearSearchHistory() {
        onlineDao.clearOnlineSearchHistory()
    }

    // ---- Lyrics ----

    override suspend fun getLyrics(videoId: String): Result<String?> {
        // First get the next page to find the lyrics endpoint
        return runCatching {
            val nextResult = YouTube.next(WatchEndpoint(videoId = videoId)).getOrThrow()
            val lyricsEndpoint = nextResult.lyricsEndpoint ?: return@runCatching null
            YouTube.lyrics(lyricsEndpoint).getOrNull()
        }
    }

    // ---- Related ----

    override suspend fun getRelated(videoId: String): Result<List<MediaMetadata>> {
        return runCatching {
            val nextResult = YouTube.next(WatchEndpoint(videoId = videoId)).getOrThrow()
            val relatedEndpoint = nextResult.relatedEndpoint ?: return@runCatching emptyList()
            val relatedPage = YouTube.related(relatedEndpoint).getOrThrow()
            relatedPage.songs.map { it.toMediaMetadata() }
        }
    }

    // ---- Internal Mappers ----

    companion object {
        /**
         * Convert any [YTItem] to [OnlineContentItem] for unified UI consumption.
         */
        fun YTItem.toOnlineContentItem(): OnlineContentItem {
            return when (this) {
                is SongItem -> OnlineContentItem.SongContent(
                    id = id,
                    title = title,
                    thumbnailUrl = thumbnail,
                    artists = artists.map { MediaArtist(id = it.id, name = it.name) },
                    album = album?.let { MediaAlbum(id = it.id, title = it.name) },
                    duration = duration,
                    explicit = explicit,
                )
                is AlbumItem -> OnlineContentItem.AlbumContent(
                    id = id,
                    browseId = browseId,
                    playlistId = playlistId,
                    title = title,
                    thumbnailUrl = thumbnail,
                    artists = artists?.map { MediaArtist(id = it.id, name = it.name) } ?: emptyList(),
                    year = year,
                    explicit = explicit,
                )
                is ArtistItem -> OnlineContentItem.ArtistContent(
                    id = id,
                    title = title,
                    thumbnailUrl = thumbnail,
                )
                is PlaylistItem -> OnlineContentItem.PlaylistContent(
                    id = id,
                    title = title,
                    thumbnailUrl = thumbnail,
                    author = author?.name,
                    songCount = songCountText,
                )
            }
        }
    }
}
