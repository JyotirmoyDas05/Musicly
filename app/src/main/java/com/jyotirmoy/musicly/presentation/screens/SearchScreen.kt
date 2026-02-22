package com.jyotirmoy.musicly.presentation.screens

import android.util.Log
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavHostController
import com.jyotirmoy.musicly.R
import com.jyotirmoy.musicly.data.model.Album
import com.jyotirmoy.musicly.data.model.Artist
import com.jyotirmoy.musicly.data.model.MediaMetadata
import com.jyotirmoy.musicly.data.model.MediaArtist
import com.jyotirmoy.musicly.data.model.MediaAlbum
import com.jyotirmoy.musicly.data.model.OnlineContentItem
import com.jyotirmoy.musicly.data.model.Playlist
import com.jyotirmoy.musicly.data.model.SearchFilterType
import com.jyotirmoy.musicly.data.model.SearchHistoryItem
import com.jyotirmoy.musicly.data.model.SearchResultItem
import com.jyotirmoy.musicly.data.model.Song
import com.jyotirmoy.musicly.data.model.toMediaMetadata
import com.jyotirmoy.musicly.presentation.components.MiniPlayerHeight
import com.jyotirmoy.musicly.presentation.components.NavBarContentHeight
import com.jyotirmoy.musicly.presentation.components.OnlineSongMenuContext
import com.jyotirmoy.musicly.presentation.components.OnlineSongOptionsBottomSheet
import com.jyotirmoy.musicly.presentation.components.PlaylistBottomSheet
import com.jyotirmoy.musicly.presentation.components.SmartImage
import com.jyotirmoy.musicly.presentation.components.SongInfoBottomSheet
import com.jyotirmoy.musicly.presentation.components.subcomps.EnhancedSongListItem
import com.jyotirmoy.musicly.presentation.navigation.Screen
import com.jyotirmoy.musicly.presentation.screens.search.components.GenreCategoriesGrid
import com.jyotirmoy.musicly.presentation.viewmodel.OnlineSearchFilter
import com.jyotirmoy.musicly.presentation.viewmodel.OnlineSearchUiState
import com.jyotirmoy.musicly.presentation.viewmodel.OnlineSearchViewModel
import com.jyotirmoy.musicly.presentation.viewmodel.PlayerViewModel
import com.jyotirmoy.musicly.presentation.viewmodel.PlaylistViewModel
import com.jyotirmoy.musicly.ui.theme.LocalMusiclyDarkTheme
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape
import timber.log.Timber

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SearchScreen(
    paddingValues: PaddingValues,
    playerViewModel: PlayerViewModel = hiltViewModel(),
    playlistViewModel: PlaylistViewModel = hiltViewModel(),
    onlineSearchViewModel: OnlineSearchViewModel = hiltViewModel(),
    navController: NavHostController,
    onSearchBarActiveChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var active by remember { mutableStateOf(false) }
    var isOnlineMode by remember { mutableStateOf(false) }
    val systemNavBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomBarHeightDp = NavBarContentHeight + systemNavBarInset
    
    var showPlaylistBottomSheet by remember { mutableStateOf(false) }
    var songsForPlaylist by remember { mutableStateOf<List<Song>>(emptyList()) }

    val uiState by playerViewModel.playerUiState.collectAsState()
    val currentFilter by remember { derivedStateOf { uiState.selectedSearchFilter } }
    val searchHistory = uiState.searchHistory
    
    val recentItems by remember(searchHistory) {
        derivedStateOf { searchHistory.filter { it.itemType != "query" } }
    }
    val recentQueries by remember(searchHistory) {
        derivedStateOf { searchHistory.filter { it.itemType == "query" } }
    }
    
    val genres by playerViewModel.genres.collectAsState()
    val stablePlayerState by playerViewModel.stablePlayerStateInfrequent.collectAsState()
    val favoriteSongIds by playerViewModel.favoriteSongIds.collectAsState()
    var showSongInfoBottomSheet by remember { mutableStateOf(false) }
    var selectedSongForInfo by remember { mutableStateOf<Song?>(null) }
    var showOnlineSongOptions by remember { mutableStateOf(false) }
    var selectedOnlineSong by remember { mutableStateOf<MediaMetadata?>(null) }
    val keyboardController = LocalSoftwareKeyboardController.current

    val onlineSearchState: OnlineSearchUiState by onlineSearchViewModel.uiState.collectAsState()
    val onlineRecentItems by remember(onlineSearchState.searchHistory) {
        derivedStateOf { onlineSearchState.searchHistory.filter { it.itemType != "query" } }
    }
    val onlineRecentQueries by remember(onlineSearchState.searchHistory) {
        derivedStateOf { onlineSearchState.searchHistory.filter { it.itemType == "query" } }
    }

    val handleSongMoreOptionsClick: (Song) -> Unit = { song ->
        selectedSongForInfo = song
        playerViewModel.selectSongForInfo(song)
        showSongInfoBottomSheet = true
    }

    val handleOnlineSongMoreOptionsClick: (OnlineContentItem.SongContent) -> Unit = { song ->
        selectedOnlineSong = song.toMediaMetadata()
        showOnlineSongOptions = true
    }

    LaunchedEffect(Unit) {
        playerViewModel.searchNavDoubleTapEvents.collect {
            active = true
        }
    }

    // Perform search whenever searchQuery, active state, or filter changes
    LaunchedEffect(searchQuery, active, currentFilter) {
        if (searchQuery.isNotBlank()) {
            playerViewModel.performSearch(searchQuery)
        } else if (active) {
            playerViewModel.performSearch("")
        }
    }

    // Trigger online search when in online mode
    LaunchedEffect(searchQuery, active, isOnlineMode, onlineSearchState.currentFilter) {
        if (isOnlineMode && searchQuery.isNotBlank()) {
            onlineSearchViewModel.performSearch(searchQuery)
        }
    }

    val searchbarHorizontalPadding by animateDpAsState(
        targetValue = if (!active) 24.dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh),
        label = "searchbarHorizontalPadding"
    )

    val searchbarCornerRadius = 28.dp
    val dm = LocalMusiclyDarkTheme.current

    val gradientColors = if (dm) {
        listOf(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), Color.Transparent)
    } else {
        listOf(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f), Color.Transparent)
    }.toImmutableList()

    val gradientBrush = remember(gradientColors) {
        Brush.verticalGradient(colors = gradientColors)
    }

    val colorScheme = MaterialTheme.colorScheme

    LaunchedEffect(active, keyboardController) {
        onSearchBarActiveChange(active)
        if (active) {
            delay(90L)
            keyboardController?.show()
        } else {
            keyboardController?.hide()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            active = false
            onSearchBarActiveChange(false)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(modifier = Modifier.fillMaxWidth().height(280.dp).background(gradientBrush))

        Column(modifier = Modifier.fillMaxSize()) {
            val safePadding = maxOf(0.dp, searchbarHorizontalPadding)

            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = safePadding)) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onSearch = {
                        if (searchQuery.isNotBlank()) {
                            playerViewModel.onSearchQuerySubmitted(searchQuery)
                            onlineSearchViewModel.onSearchSubmitted(searchQuery)
                        }
                        active = false
                    },
                    active = active,
                    onActiveChange = { active = it },
                    modifier = Modifier.fillMaxWidth().animateContentSize().clip(RoundedCornerShape(searchbarCornerRadius)),
                    placeholder = {
                        Text("Search...", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                    },
                    leadingIcon = {
                        Icon(imageVector = Icons.Rounded.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(48.dp).padding(end = 10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                            ) {
                                Icon(imageVector = Icons.Rounded.Close, contentDescription = "Clear", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    },
                    colors = SearchBarDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        dividerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        inputFieldColors = TextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            cursorColor = MaterialTheme.colorScheme.primary
                        )
                    ),
                    content = {
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                                SegmentedButton(
                                    selected = !isOnlineMode,
                                    onClick = { isOnlineMode = false },
                                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                    label = { Text("Local") }
                                )
                                SegmentedButton(
                                    selected = isOnlineMode,
                                    onClick = { isOnlineMode = true },
                                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                    label = { Text("Online") }
                                )
                            }

                            if (!isOnlineMode) {
                                FlowRow(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    SearchFilterChip(SearchFilterType.ALL, currentFilter, playerViewModel)
                                    SearchFilterChip(SearchFilterType.SONGS, currentFilter, playerViewModel)
                                    SearchFilterChip(SearchFilterType.ALBUMS, currentFilter, playerViewModel)
                                    SearchFilterChip(SearchFilterType.ARTISTS, currentFilter, playerViewModel)
                                    SearchFilterChip(SearchFilterType.PLAYLISTS, currentFilter, playerViewModel)
                                }

                                if (searchQuery.isBlank() && active && recentQueries.isNotEmpty()) {
                                    val rememberedOnHistoryClick: (String) -> Unit = remember(playerViewModel) {
                                        { query -> searchQuery = query }
                                    }
                                    val rememberedOnHistoryDelete: (String) -> Unit = remember(playerViewModel) {
                                        { query -> playerViewModel.deleteSearchHistoryItem(query) }
                                    }
                                    val rememberedOnClearAllHistory: () -> Unit = remember(playerViewModel) {
                                        { playerViewModel.clearSearchHistory() }
                                    }
                                    SearchHistoryList(
                                        historyItems = recentQueries,
                                        onHistoryClick = rememberedOnHistoryClick,
                                        onHistoryDelete = rememberedOnHistoryDelete,
                                        onClearAllHistory = rememberedOnClearAllHistory
                                    )
                                } else if (searchQuery.isNotBlank() && uiState.searchResults.isEmpty()) {
                                    EmptySearchResults(searchQuery = searchQuery, colorScheme = colorScheme)
                                } else if (uiState.searchResults.isNotEmpty()) {
                                    val rememberedOnItemSelected: (SearchHistoryItem?) -> Unit = remember(searchQuery, playerViewModel) {
                                        { item ->
                                            if (item != null) {
                                                playerViewModel.onSearchItemClicked(item)
                                            } else if (searchQuery.isNotBlank()) {
                                                playerViewModel.onSearchQuerySubmitted(searchQuery)
                                            }
                                            active = false
                                        }
                                    }
                                    SearchResultsList(
                                        results = uiState.searchResults,
                                        playerViewModel = playerViewModel,
                                        onItemSelected = rememberedOnItemSelected,
                                        currentPlayingSongId = stablePlayerState.currentSong?.id,
                                        isPlaying = stablePlayerState.isPlaying,
                                        onSongMoreOptionsClick = handleSongMoreOptionsClick,
                                        navController = navController
                                    )
                                }
                            } else {
                                FlowRow(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OnlineFilterChip(OnlineSearchFilter.ALL, onlineSearchState.currentFilter, onlineSearchViewModel)
                                    OnlineFilterChip(OnlineSearchFilter.SONGS, onlineSearchState.currentFilter, onlineSearchViewModel)
                                    OnlineFilterChip(OnlineSearchFilter.ALBUMS, onlineSearchState.currentFilter, onlineSearchViewModel)
                                    OnlineFilterChip(OnlineSearchFilter.ARTISTS, onlineSearchState.currentFilter, onlineSearchViewModel)
                                    OnlineFilterChip(OnlineSearchFilter.PLAYLISTS, onlineSearchState.currentFilter, onlineSearchViewModel)
                                }

                                if (searchQuery.isBlank() && active && onlineRecentQueries.isNotEmpty()) {
                                    val rememberedOnHistoryClick: (String) -> Unit = remember(onlineSearchViewModel) {
                                        { query -> searchQuery = query }
                                    }
                                    val rememberedOnHistoryDelete: (String) -> Unit = remember(onlineSearchViewModel) {
                                        { query -> onlineSearchViewModel.deleteSearchHistoryItem(query) }
                                    }
                                    val rememberedOnClearAllHistory: () -> Unit = remember(onlineSearchViewModel) {
                                        { onlineSearchViewModel.clearSearchHistory() }
                                    }
                                    SearchHistoryList(
                                        historyItems = onlineRecentQueries,
                                        onHistoryClick = rememberedOnHistoryClick,
                                        onHistoryDelete = rememberedOnHistoryDelete,
                                        onClearAllHistory = rememberedOnClearAllHistory
                                    )
                                } else if (onlineSearchState.isLoading && onlineSearchState.results.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                        androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(32.dp), color = MaterialTheme.colorScheme.primary)
                                    }
                                } else if (searchQuery.isNotBlank() && onlineSearchState.results.isEmpty() && !onlineSearchState.isLoading) {
                                    EmptySearchResults(searchQuery = searchQuery, colorScheme = colorScheme)
                                } else if (onlineSearchState.results.isNotEmpty()) {
                                    OnlineSearchResultsList(
                                        results = onlineSearchState.results,
                                        navController = navController,
                                        playerViewModel = playerViewModel,
                                        onItemSelected = { item ->
                                            if (item != null) {
                                                onlineSearchViewModel.onSearchItemClicked(item)
                                            } else if (searchQuery.isNotBlank()) {
                                                onlineSearchViewModel.onSearchSubmitted(searchQuery)
                                            }
                                            active = false
                                        },
                                        onMoreOptionsClick = handleOnlineSongMoreOptionsClick
                                    )
                                }
                            }
                        }
                    }
                )
            }

            if (!active) {
                if (searchQuery.isBlank()) {
                    Box {
                        val combinedGenres = remember(genres, onlineSearchState.moodsAndGenres) {
                            val list = (genres + onlineSearchState.moodsAndGenres).toMutableList()
                            if (list.isEmpty()) {
                                list.add(com.jyotirmoy.musicly.data.model.Genre(id = "music", name = "Music", lightColorHex = "#6200EE", darkColorHex = "#6200EE"))
                            }
                            list.distinctBy { it.id }
                        }
                        val allRecentItems = remember(recentItems, onlineRecentItems) {
                            (recentItems + onlineRecentItems).sortedByDescending { it.timestamp }
                                .distinctBy { "${it.itemType}_${it.itemId ?: it.query}" }
                        }
                        
                        val handleRecentItemClick: (SearchHistoryItem) -> Unit = remember(navController, playerViewModel) {
                            { item ->
                                when (item.itemType) {
                                    "song" -> {
                                        if (item.itemId != null) {
                                            val metadata = MediaMetadata(
                                                id = item.itemId,
                                                title = item.query,
                                                artists = listOf(MediaArtist(id = null, name = item.subtitle ?: "Unknown Artist")),
                                                duration = 0,
                                                thumbnailUrl = item.thumbnailUrl
                                            )
                                            playerViewModel.playOnlineSong(metadata, listOf(metadata), "Recent")
                                        }
                                    }
                                    "album" -> if (item.itemId != null) navController.navigate(Screen.AlbumDetail.createRoute(item.itemId))
                                    "artist" -> if (item.itemId != null) navController.navigate(Screen.ArtistDetail.createRoute(item.itemId))
                                    "playlist" -> {
                                        if (item.itemId != null) {
                                            if (item.itemId.toLongOrNull() != null) navController.navigate(Screen.PlaylistDetail.createRoute(item.itemId))
                                            else navController.navigate(Screen.OnlinePlaylistDetail.createRoute(item.itemId))
                                        }
                                    }
                                    else -> {
                                        searchQuery = item.query
                                        active = true
                                        playerViewModel.onSearchQuerySubmitted(searchQuery)
                                    }
                                }
                            }
                        }

                        GenreCategoriesGrid(
                            genres = combinedGenres,
                            recentItems = allRecentItems,
                            onRecentItemClick = handleRecentItemClick,
                            onGenreClick = { genre ->
                                val encodedGenreId = java.net.URLEncoder.encode(genre.id, "UTF-8")
                                if (genre.id.startsWith("FE")) {
                                     navController.navigate(Screen.MoodDetail.createRoute(genre.id, genre.name))
                                } else {
                                     navController.navigate(Screen.GenreDetail.createRoute(encodedGenreId))
                                }
                            },
                            playerViewModel = playerViewModel,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                } else {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        if (!isOnlineMode) {
                            FlowRow(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                SearchFilterChip(SearchFilterType.ALL, currentFilter, playerViewModel)
                                SearchFilterChip(SearchFilterType.SONGS, currentFilter, playerViewModel)
                                SearchFilterChip(SearchFilterType.ALBUMS, currentFilter, playerViewModel)
                                SearchFilterChip(SearchFilterType.ARTISTS, currentFilter, playerViewModel)
                                SearchFilterChip(SearchFilterType.PLAYLISTS, currentFilter, playerViewModel)
                            }
                            SearchResultsList(
                                results = uiState.searchResults,
                                playerViewModel = playerViewModel,
                                onItemSelected = { _ -> },
                                currentPlayingSongId = stablePlayerState.currentSong?.id,
                                isPlaying = stablePlayerState.isPlaying,
                                onSongMoreOptionsClick = handleSongMoreOptionsClick,
                                navController = navController
                            )
                        } else {
                            FlowRow(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OnlineFilterChip(OnlineSearchFilter.ALL, onlineSearchState.currentFilter, onlineSearchViewModel)
                                OnlineFilterChip(OnlineSearchFilter.SONGS, onlineSearchState.currentFilter, onlineSearchViewModel)
                                OnlineFilterChip(OnlineSearchFilter.ALBUMS, onlineSearchState.currentFilter, onlineSearchViewModel)
                                OnlineFilterChip(OnlineSearchFilter.ARTISTS, onlineSearchState.currentFilter, onlineSearchViewModel)
                                OnlineFilterChip(OnlineSearchFilter.PLAYLISTS, onlineSearchState.currentFilter, onlineSearchViewModel)
                            }
                            OnlineSearchResultsList(
                                results = onlineSearchState.results,
                                navController = navController,
                                playerViewModel = playerViewModel,
                                onItemSelected = { _ -> },
                                onMoreOptionsClick = handleOnlineSongMoreOptionsClick
                            )
                        }
                    }
                }
            }
        }
    }

    val downloads by playerViewModel.downloads.collectAsState()
    val downloadState = selectedOnlineSong?.id?.let { downloads[it] }
    val downloadProgress = downloadState?.percentDownloaded?.div(100f)
    val isDownloading = downloadState?.state == androidx.media3.exoplayer.offline.Download.STATE_DOWNLOADING
    val isDownloaded = downloadState?.state == androidx.media3.exoplayer.offline.Download.STATE_COMPLETED

    if (showOnlineSongOptions && selectedOnlineSong != null) {
        val song = selectedOnlineSong!!
        OnlineSongOptionsBottomSheet(
            metadata = song,
            menuContext = OnlineSongMenuContext.SEARCH,
            onDismiss = { showOnlineSongOptions = false },
            onPlaySong = {
                playerViewModel.playOnlineSong(song, listOf(song), "Search")
                showOnlineSongOptions = false
            },
            onAddToQueue = {
                playerViewModel.addOnlineSongToQueue(song)
                showOnlineSongOptions = false
            },
            onPlayNext = {
                playerViewModel.addOnlineSongNextToQueue(song)
                showOnlineSongOptions = false
            },
            onStartRadio = {
                playerViewModel.startRadio(song)
                showOnlineSongOptions = false
            },
            onAddToPlaylist = {
                Toast.makeText(context, "Add to Playlist not implemented yet", Toast.LENGTH_SHORT).show()
                showOnlineSongOptions = false
            },
            onDownload = {
                if (!isDownloading) {
                    playerViewModel.downloadOnlineSong(
                        songId = song.id,
                        title = song.title,
                        artist = song.displayArtist,
                        album = song.album?.title,
                        thumbnailUrl = song.thumbnailUrl
                    )
                }
            },
            isDownloaded = isDownloaded,
            isDownloading = isDownloading,
            downloadProgress = downloadProgress,
            onDeleteDownloaded = {
                playerViewModel.deleteDownloadedOnlineSong(song.id, song.title)
                showOnlineSongOptions = false
            },
            onNavigateToArtist = { artistId ->
                navController.navigate(Screen.ArtistDetail.createRoute(artistId))
                showOnlineSongOptions = false
            },
            onNavigateToAlbum = { albumId ->
                navController.navigate(Screen.AlbumDetail.createRoute(albumId))
                showOnlineSongOptions = false
            }
        )
    }

    if (showSongInfoBottomSheet && selectedSongForInfo != null) {
        val currentSong = selectedSongForInfo
        val isFavorite = remember(currentSong?.id, favoriteSongIds) {
            derivedStateOf { currentSong?.let { favoriteSongIds.contains(it.id) } }
        }.value ?: false

        if (currentSong != null) {
            SongInfoBottomSheet(
                song = currentSong,
                isFavorite = isFavorite,
                removeFromListTrigger = { searchQuery = "$searchQuery " },
                onToggleFavorite = { playerViewModel.toggleFavoriteSpecificSong(currentSong) },
                onDismiss = { showSongInfoBottomSheet = false },
                onPlaySong = {
                    playerViewModel.showAndPlaySong(currentSong)
                    showSongInfoBottomSheet = false
                },
                onAddToQueue = {
                    playerViewModel.addSongToQueue(currentSong)
                    showSongInfoBottomSheet = false
                },
                onAddNextToQueue = {
                    playerViewModel.addSongNextToQueue(currentSong)
                    showSongInfoBottomSheet = false
                },
                onAddToPlayList = { 
                    songsForPlaylist = listOf(currentSong)
                    showPlaylistBottomSheet = true 
                },
                onDeleteFromDevice = playerViewModel::deleteFromDevice,
                onNavigateToAlbum = {
                    navController.navigate(Screen.AlbumDetail.createRoute(currentSong.albumId))
                    showSongInfoBottomSheet = false
                },
                onNavigateToArtist = {
                    navController.navigate(Screen.ArtistDetail.createRoute(currentSong.artistId))
                    showSongInfoBottomSheet = false
                },
                onEditSong = { newTitle, newArtist, newAlbum, newGenre, newLyrics, newTrackNumber, coverArtUpdate ->
                    playerViewModel.editSongMetadata(currentSong, newTitle, newArtist, newAlbum, newGenre, newLyrics, newTrackNumber, coverArtUpdate)
                },
                generateAiMetadata = { fields -> playerViewModel.generateAiMetadata(currentSong, fields) },
            )
        }
    }
    
    if (showPlaylistBottomSheet && songsForPlaylist.isNotEmpty()) {
        val playlistUiState by playlistViewModel.uiState.collectAsState()
        PlaylistBottomSheet(
            playlistUiState = playlistUiState,
            songs = songsForPlaylist,
            onDismiss = { showPlaylistBottomSheet = false },
            bottomBarHeight = bottomBarHeightDp,
            playerViewModel = playerViewModel,
        )
    }
}

@Composable
fun SearchResultSectionHeader(title: String) {
    Text(text = title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 4.dp))
}

@Composable
fun SearchHistoryList(
    historyItems: List<SearchHistoryItem>,
    onHistoryClick: (String) -> Unit,
    onHistoryDelete: (String) -> Unit,
    onClearAllHistory: () -> Unit
) {
    Column {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Recent Searches", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            if (historyItems.isNotEmpty()) {
                TextButton(onClick = onClearAllHistory) { Text("Clear All") }
            }
        }
        LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(top = 8.dp)) {
            items(historyItems, key = { "history_${it.id ?: it.query}" }) { item ->
                SearchHistoryListItem(item = item, onHistoryClick = onHistoryClick, onHistoryDelete = onHistoryDelete)
            }
        }
    }
}

@Composable
fun SearchHistoryListItem(item: SearchHistoryItem, onHistoryClick: (String) -> Unit, onHistoryDelete: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = { onHistoryClick(item.query) })
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
                imageVector = Icons.Rounded.History,
                contentDescription = "History Icon",
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = item.query,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        IconButton(onClick = { onHistoryDelete(item.query) }) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Delete history item",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun EmptySearchResults(searchQuery: String, colorScheme: ColorScheme) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = Icons.Rounded.Search, contentDescription = "No results", modifier = Modifier.size(80.dp).padding(bottom = 16.dp), tint = colorScheme.primary.copy(alpha = 0.6f))
        Text(text = if (searchQuery.isNotBlank()) "No results for \"$searchQuery\"" else "Nothing found", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Try a different search term or check your filters.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f), textAlign = TextAlign.Center)
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun SearchResultsList(
    results: List<SearchResultItem>,
    playerViewModel: PlayerViewModel,
    onItemSelected: (SearchHistoryItem?) -> Unit,
    currentPlayingSongId: String?,
    isPlaying: Boolean,
    onSongMoreOptionsClick: (Song) -> Unit,
    navController: NavHostController
) {
    val localDensity = LocalDensity.current
    val keyboardController = LocalSoftwareKeyboardController.current

    if (results.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Text("No results found.", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    val groupedResults = results.groupBy { item ->
        when (item) {
            is SearchResultItem.SongItem -> SearchFilterType.SONGS
            is SearchResultItem.AlbumItem -> SearchFilterType.ALBUMS
            is SearchResultItem.ArtistItem -> SearchFilterType.ARTISTS
            is SearchResultItem.PlaylistItem -> SearchFilterType.PLAYLISTS
        }
    }

    val sectionOrder = listOf(SearchFilterType.SONGS, SearchFilterType.ALBUMS, SearchFilterType.ARTISTS, SearchFilterType.PLAYLISTS)
    val imePadding = WindowInsets.ime.getBottom(localDensity).dp
    val systemBarPaddingBottom = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding() + 94.dp

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 8.dp, bottom = if (imePadding <= 8.dp) (MiniPlayerHeight + systemBarPaddingBottom) else imePadding)) {
        sectionOrder.forEach { filterType ->
            val itemsForSection = groupedResults[filterType] ?: emptyList()
            if (itemsForSection.isNotEmpty()) {
                item(key = "header_${filterType.name}") { SearchResultSectionHeader(title = filterType.name.lowercase().replaceFirstChar { it.titlecase() } + "s") }
                items(count = itemsForSection.size, key = { index ->
                    val item = itemsForSection[index]
                    when (item) {
                        is SearchResultItem.SongItem -> "song_${item.song.id}"
                        is SearchResultItem.AlbumItem -> "album_${item.album.id}"
                        is SearchResultItem.ArtistItem -> "artist_${item.artist.id}"
                        is SearchResultItem.PlaylistItem -> "playlist_${item.playlist.id}_$index"
                    }
                }) { index ->
                    val item = itemsForSection[index]
                    Box(modifier = Modifier.padding(bottom = 12.dp)) {
                        when (item) {
                            is SearchResultItem.SongItem -> {
                                EnhancedSongListItem(
                                    song = item.song,
                                    isPlaying = isPlaying,
                                    isCurrentSong = currentPlayingSongId == item.song.id,
                                    onMoreOptionsClick = onSongMoreOptionsClick,
                                    onClick = {
                                        playerViewModel.showAndPlaySong(item.song)
                                        keyboardController?.hide()
                                        onItemSelected(SearchHistoryItem(
                                            query = item.song.title,
                                            timestamp = System.currentTimeMillis(),
                                            itemType = "song",
                                            itemId = item.song.id,
                                            subtitle = item.song.displayArtist,
                                            thumbnailUrl = item.song.albumArtUriString
                                        ))
                                    }
                                )
                            }
                            is SearchResultItem.AlbumItem -> {
                                SearchResultAlbumItem(
                                    album = item.album,
                                    onPlayClick = {
                                        playerViewModel.playAlbum(item.album)
                                        keyboardController?.hide()
                                    },
                                    onOpenClick = {
                                        navController.navigate(Screen.AlbumDetail.createRoute(item.album.id))
                                        onItemSelected(SearchHistoryItem(
                                            query = item.album.title,
                                            timestamp = System.currentTimeMillis(),
                                            itemType = "album",
                                            itemId = item.album.id.toString(),
                                            subtitle = item.album.artist,
                                            thumbnailUrl = item.album.albumArtUriString
                                        ))
                                    }
                                )
                            }
                            is SearchResultItem.ArtistItem -> {
                                SearchResultArtistItem(
                                    artist = item.artist,
                                    onPlayClick = {
                                        playerViewModel.playArtist(item.artist)
                                        keyboardController?.hide()
                                    },
                                    onOpenClick = {
                                        navController.navigate(Screen.ArtistDetail.createRoute(item.artist.id))
                                        onItemSelected(SearchHistoryItem(
                                            query = item.artist.name,
                                            timestamp = System.currentTimeMillis(),
                                            itemType = "artist",
                                            itemId = item.artist.id.toString(),
                                            subtitle = "Artist",
                                            thumbnailUrl = null
                                        ))
                                    }
                                )
                            }
                            is SearchResultItem.PlaylistItem -> {
                                SearchResultPlaylistItem(
                                    playlist = item.playlist,
                                    onPlayClick = {
                                        keyboardController?.hide()
                                    },
                                    onOpenClick = {
                                        navController.navigate(Screen.PlaylistDetail.createRoute(item.playlist.id))
                                        onItemSelected(SearchHistoryItem(
                                            query = item.playlist.name,
                                            timestamp = System.currentTimeMillis(),
                                            itemType = "playlist",
                                            itemId = item.playlist.id.toString(),
                                            subtitle = "Playlist",
                                            thumbnailUrl = null
                                        ))
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResultAlbumItem(album: Album, onOpenClick: () -> Unit, onPlayClick: () -> Unit) {
    val itemShape = AbsoluteSmoothCornerShape(26.dp, 60)
    Card(onClick = onOpenClick, shape = itemShape, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            SmartImage(model = album.albumArtUriString, contentDescription = album.title, modifier = Modifier.size(56.dp).clip(itemShape))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = album.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = album.artist, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FilledIconButton(onClick = onPlayClick, modifier = Modifier.size(40.dp), shape = CircleShape, colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f), contentColor = MaterialTheme.colorScheme.onSecondary)) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = "Play Album", modifier = Modifier.size(24.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResultArtistItem(artist: Artist, onOpenClick: () -> Unit, onPlayClick: () -> Unit) {
    val itemShape = AbsoluteSmoothCornerShape(26.dp, 60)
    Card(onClick = onOpenClick, shape = itemShape, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painter = painterResource(id = R.drawable.rounded_artist_24), contentDescription = "Artist", modifier = Modifier.size(56.dp).background(MaterialTheme.colorScheme.tertiaryContainer, CircleShape).padding(12.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(text = artist.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = "${artist.songCount} Songs", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FilledIconButton(onClick = onPlayClick, modifier = Modifier.size(40.dp), shape = CircleShape, colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f), contentColor = MaterialTheme.colorScheme.onTertiary)) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = "Play Artist", modifier = Modifier.size(24.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResultPlaylistItem(playlist: Playlist, onOpenClick: () -> Unit, onPlayClick: () -> Unit) {
    val itemShape = AbsoluteSmoothCornerShape(26.dp, 60)
    Card(onClick = onOpenClick, shape = itemShape, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Rounded.PlaylistPlay, contentDescription = "Playlist", modifier = Modifier.size(56.dp).clip(itemShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)).padding(8.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(text = playlist.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            FilledIconButton(onClick = onPlayClick, modifier = Modifier.size(40.dp), shape = CircleShape, colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f), contentColor = MaterialTheme.colorScheme.onPrimary)) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = "Play Playlist", modifier = Modifier.size(24.dp))
            }
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun SearchFilterChip(filterType: SearchFilterType, currentFilter: SearchFilterType, playerViewModel: PlayerViewModel, modifier: Modifier = Modifier) {
    val selected = filterType == currentFilter
    FilterChip(
        selected = selected,
        onClick = { playerViewModel.updateSearchFilter(filterType) },
        label = { Text(filterType.name.lowercase().replaceFirstChar { it.titlecase() }) },
        modifier = modifier,
        shape = CircleShape,
        border = BorderStroke(width = 0.dp, color = Color.Transparent),
        colors = FilterChipDefaults.filterChipColors(
            containerColor =  MaterialTheme.colorScheme.secondaryContainer,
            labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
        ),
        leadingIcon = if (selected) { { Icon(painter = painterResource(R.drawable.rounded_check_circle_24), contentDescription = "Selected", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(FilterChipDefaults.IconSize)) } } else null
    )
}

@Composable
fun OnlineFilterChip(filterType: OnlineSearchFilter, currentFilter: OnlineSearchFilter, viewModel: OnlineSearchViewModel, modifier: Modifier = Modifier) {
    val selected = filterType == currentFilter
    FilterChip(
        selected = selected,
        onClick = { viewModel.updateFilter(filterType) },
        label = { Text(filterType.name.lowercase().replaceFirstChar { it.titlecase() }) },
        modifier = modifier,
        shape = CircleShape,
        border = BorderStroke(width = 0.dp, color = Color.Transparent),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
        ),
        leadingIcon = if (selected) { { Icon(painter = painterResource(R.drawable.rounded_check_circle_24), contentDescription = "Selected", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(FilterChipDefaults.IconSize)) } } else null
    )
}

@Composable
fun OnlineSearchResultsList(
    results: List<OnlineContentItem>,
    navController: NavHostController,
    playerViewModel: PlayerViewModel,
    onItemSelected: (SearchHistoryItem?) -> Unit,
    onMoreOptionsClick: (OnlineContentItem.SongContent) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    LazyColumn(contentPadding = PaddingValues(bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(count = results.size, key = { index -> "${results[index].id}_$index" }) { index ->
            val item = results[index]
            Box(modifier = Modifier.padding(bottom = 4.dp)) {
                when (item) {
                    is OnlineContentItem.SongContent -> {
                        OnlineSearchSongItem(
                            song = item,
                            onClick = {
                                val metadata = item.toMediaMetadata()
                                playerViewModel.playOnlineSong(metadata, listOf(metadata), "Search")
                                keyboardController?.hide()
                                onItemSelected(SearchHistoryItem(
                                    query = item.title,
                                    timestamp = System.currentTimeMillis(),
                                    itemType = "song",
                                    itemId = item.id,
                                    subtitle = item.artists.joinToString { it.name },
                                    thumbnailUrl = item.thumbnailUrl
                                ))
                            },
                            onMoreOptionsClick = { onMoreOptionsClick(item) }
                        )
                    }
                    is OnlineContentItem.AlbumContent -> {
                        OnlineSearchAlbumItem(album = item, onClick = {
                            navController.navigate(Screen.AlbumDetail.createRoute(item.browseId))
                            onItemSelected(SearchHistoryItem(
                                query = item.title,
                                timestamp = System.currentTimeMillis(),
                                itemType = "album",
                                itemId = item.browseId,
                                subtitle = item.artists.joinToString { it.name },
                                thumbnailUrl = item.thumbnailUrl
                            ))
                        })
                    }
                    is OnlineContentItem.ArtistContent -> {
                        OnlineSearchArtistItem(artist = item, onClick = {
                            navController.navigate(Screen.ArtistDetail.createRoute(item.id))
                            onItemSelected(SearchHistoryItem(
                                query = item.title,
                                timestamp = System.currentTimeMillis(),
                                itemType = "artist",
                                itemId = item.id,
                                subtitle = "Artist",
                                thumbnailUrl = item.thumbnailUrl
                            ))
                        })
                    }
                    is OnlineContentItem.PlaylistContent -> {
                        OnlineSearchPlaylistItem(playlist = item, onClick = {
                            navController.navigate(Screen.OnlinePlaylistDetail.createRoute(item.id))
                            onItemSelected(SearchHistoryItem(
                                query = item.title,
                                timestamp = System.currentTimeMillis(),
                                itemType = "playlist",
                                itemId = item.id,
                                subtitle = item.author,
                                thumbnailUrl = item.thumbnailUrl
                            ))
                        })
                    }
                    else -> {}
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OnlineSearchSongItem(song: OnlineContentItem.SongContent, onClick: () -> Unit, onMoreOptionsClick: () -> Unit) {
    val shape = AbsoluteSmoothCornerShape(26.dp, 60)
    Card(onClick = onClick, shape = shape, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            SmartImage(model = song.thumbnailUrl, contentDescription = song.title, contentScale = ContentScale.Crop, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(text = song.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = song.artists.joinToString(", ") { it.name }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = onMoreOptionsClick, modifier = Modifier.size(40.dp).padding(end = 4.dp)) { Icon(imageVector = Icons.Rounded.MoreVert, contentDescription = "More options", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            FilledIconButton(onClick = onClick, modifier = Modifier.size(40.dp), shape = CircleShape, colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f), contentColor = MaterialTheme.colorScheme.onPrimary)) { Icon(Icons.Rounded.PlayArrow, contentDescription = "Play", modifier = Modifier.size(24.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OnlineSearchAlbumItem(album: OnlineContentItem.AlbumContent, onClick: () -> Unit) {
    val shape = AbsoluteSmoothCornerShape(26.dp, 60)
    Card(onClick = onClick, shape = shape, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            SmartImage(model = album.thumbnailUrl, contentDescription = album.title, contentScale = ContentScale.Crop, modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(text = album.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = album.artists.joinToString(", ") { it.name }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (album.year != null) { Text(text = "Album · ${album.year}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OnlineSearchArtistItem(artist: OnlineContentItem.ArtistContent, onClick: () -> Unit) {
    val shape = AbsoluteSmoothCornerShape(26.dp, 60)
    Card(onClick = onClick, shape = shape, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            SmartImage(model = artist.thumbnailUrl, contentDescription = artist.title, contentScale = ContentScale.Crop, modifier = Modifier.size(48.dp).clip(CircleShape))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(text = artist.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = "Artist", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OnlineSearchPlaylistItem(playlist: OnlineContentItem.PlaylistContent, onClick: () -> Unit) {
    val shape = AbsoluteSmoothCornerShape(26.dp, 60)
    Card(onClick = onClick, shape = shape, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            SmartImage(model = playlist.thumbnailUrl, contentDescription = playlist.title, contentScale = ContentScale.Crop, modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(text = playlist.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (playlist.author != null) { Text(text = playlist.author, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
        }
    }
}
