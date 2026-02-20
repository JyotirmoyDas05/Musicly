package com.jyotirmoy.musicly.presentation.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.ReportDrawnWhen
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import com.jyotirmoy.musicly.R
import com.jyotirmoy.musicly.data.model.MediaMetadata
import com.jyotirmoy.musicly.data.model.OnlineContentItem
import com.jyotirmoy.musicly.data.model.Song
import com.jyotirmoy.musicly.data.model.toMediaMetadata
import com.jyotirmoy.musicly.presentation.components.AlbumArtCollage
import com.jyotirmoy.musicly.presentation.components.BetaInfoBottomSheet
import com.jyotirmoy.musicly.presentation.components.ChangelogBottomSheet
import com.jyotirmoy.musicly.presentation.components.DailyMixSection
import com.jyotirmoy.musicly.presentation.components.HomeGradientTopBar
import com.jyotirmoy.musicly.presentation.components.HomeOptionsBottomSheet
import com.jyotirmoy.musicly.presentation.components.MiniPlayerHeight
import com.jyotirmoy.musicly.presentation.components.OnlineContentSection
import com.jyotirmoy.musicly.presentation.components.RecentlyPlayedSection
import com.jyotirmoy.musicly.presentation.components.RecentlyPlayedSectionMinSongsToShow
import com.jyotirmoy.musicly.presentation.components.SmartImage
import com.jyotirmoy.musicly.presentation.components.StatsOverviewCard
import com.jyotirmoy.musicly.presentation.components.subcomps.AutoSizingTextToFill
import com.jyotirmoy.musicly.presentation.components.subcomps.PlayingEqIcon
import com.jyotirmoy.musicly.presentation.model.mapRecentlyPlayedSongs
import com.jyotirmoy.musicly.presentation.navigation.Screen
import com.jyotirmoy.musicly.presentation.viewmodel.HomePageUiState
import com.jyotirmoy.musicly.presentation.viewmodel.HomeViewModel
import com.jyotirmoy.musicly.presentation.viewmodel.PlayerViewModel
import com.jyotirmoy.musicly.presentation.viewmodel.UpdateViewModel
import com.jyotirmoy.musicly.presentation.viewmodel.SettingsViewModel
import com.jyotirmoy.musicly.presentation.viewmodel.StatsViewModel
import com.jyotirmoy.musicly.presentation.components.UpdateBottomSheet
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape
import com.jyotirmoy.musicly.presentation.components.QuickPicksGridSection

sealed class HybridMixItem {
    data class Local(val song: Song) : HybridMixItem()
    data class Online(val item: OnlineContentItem.SongContent) : HybridMixItem()
}

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    paddingValuesParent: PaddingValues,
    playerViewModel: PlayerViewModel = hiltViewModel(),
    homeViewModel: HomeViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    updateViewModel: UpdateViewModel = hiltViewModel(),
    onOpenSidebar: () -> Unit
) {
    val context = LocalContext.current
    val isBenchmarkMode = remember {
        (context as? Activity)?.intent?.getBooleanExtra("is_benchmark", false) ?: false
    }
    val statsViewModel: StatsViewModel = hiltViewModel()
    val homePageUiState by homeViewModel.uiState.collectAsState()

    val allSongs by playerViewModel.allSongsFlow.collectAsState(initial = emptyList())
    val dailyMixSongs by playerViewModel.dailyMixSongs.collectAsState()
    val curatedYourMixSongs by playerViewModel.yourMixSongs.collectAsState()
    val playbackHistory by playerViewModel.playbackHistory.collectAsState()

    val yourMixSongsList = remember(curatedYourMixSongs, dailyMixSongs, allSongs) {
        when {
            curatedYourMixSongs.isNotEmpty() -> curatedYourMixSongs
            dailyMixSongs.isNotEmpty() -> dailyMixSongs
            else -> allSongs.toImmutableList()
        }
    }
    val recentlyPlayedSongs = remember(playbackHistory, allSongs) {
        mapRecentlyPlayedSongs(
            playbackHistory = playbackHistory,
            songs = allSongs,
            maxItems = 64
        )
    }
    val recentlyPlayedQueue = remember(recentlyPlayedSongs) {
        recentlyPlayedSongs.map { it.song }.toImmutableList()
    }

    val onlineQuickPicks = remember(homePageUiState) {
        val state = homePageUiState
        if (state is HomePageUiState.Success) {
            val sections = state.homePage.sections
            sections.flatMap { it.items }.filterIsInstance<OnlineContentItem.SongContent>()
        } else emptyList()
    }

    val hybridMixList = remember(yourMixSongsList, onlineQuickPicks) {
        val result = mutableListOf<HybridMixItem>()
        val maxLen = maxOf(yourMixSongsList.size, onlineQuickPicks.size)
        for (i in 0 until maxLen) {
            if (i < yourMixSongsList.size) result.add(HybridMixItem.Local(yourMixSongsList[i]))
            if (i < onlineQuickPicks.size) result.add(HybridMixItem.Online(onlineQuickPicks[i]))
        }
        result.toImmutableList()
    }

    ReportDrawnWhen {
        yourMixSongsList.isNotEmpty() || onlineQuickPicks.isNotEmpty() || isBenchmarkMode
    }

    val yourMixSong: String = "Today's Mix for you"

    val stablePlayerState by playerViewModel.stablePlayerState.collectAsState()
    val currentSong = stablePlayerState.currentSong
    val isShuffleEnabled = stablePlayerState.isShuffleEnabled

    val bottomPadding = if (currentSong != null) MiniPlayerHeight else 0.dp

    var showOptionsBottomSheet by remember { mutableStateOf(false) }
    var showChangelogBottomSheet by remember { mutableStateOf(false) }
    var showBetaInfoBottomSheet by remember { mutableStateOf(false) }
    var showUpdateBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val betaSheetState = rememberModalBottomSheetState()
    val updateSheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    val weeklyStats by statsViewModel.weeklyOverview.collectAsState()
    val isUpdateAvailable by updateViewModel.isUpdateAvailable.collectAsState()

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                HomeGradientTopBar(
                    onNavigationIconClick = {
                        navController.navigate(Screen.Settings.route)
                    },
                    onMoreOptionsClick = {
                        showChangelogBottomSheet = true
                    },
                    onBetaClick = {
                        showBetaInfoBottomSheet = true
                    },
                    onUpdateClick = {
                        showUpdateBottomSheet = true
                    },
                    showUpdateButton = isUpdateAvailable,
                    onMenuClick = {
                        // onOpenSidebar()
                    }
                )
            }
        ) { innerPadding ->
            LazyColumn(
                state = rememberLazyListState(),
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    bottom = paddingValuesParent.calculateBottomPadding()
                            + 38.dp + bottomPadding
                ),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                if (homePageUiState is HomePageUiState.Success) {
                    val homePageData = (homePageUiState as HomePageUiState.Success).homePage
                    // chips is not available in OnlineHomePage currently, only OnlineHomeSection
                    // For now, let's skip the chips item until model is updated or use a mock
                }

                item(key = "your_mix_header") {
                    YourMixHeader(
                        song = yourMixSong,
                        isShuffleEnabled = isShuffleEnabled,
                        onPlayShuffled = {
                            if (yourMixSongsList.isNotEmpty()) {
                                playerViewModel.playSongsShuffled(
                                    songsToPlay = yourMixSongsList,
                                    queueName = "Your Mix"
                                )
                            }
                        }
                    )
                }

                if (hybridMixList.isNotEmpty()) {
                    item(key = "album_art_collage") {
                        AlbumArtCollage(
                            modifier = Modifier.fillMaxWidth(),
                            items = hybridMixList,
                            getImageUrl = { item ->
                                when (item) {
                                    is HybridMixItem.Local -> item.song.albumArtUriString
                                    is HybridMixItem.Online -> item.item.thumbnailUrl
                                }
                            },
                            padding = 14.dp,
                            height = 400.dp,
                            onItemClick = { itemToPlay ->
                                when (itemToPlay) {
                                    is HybridMixItem.Local -> playerViewModel.showAndPlaySong(itemToPlay.song, yourMixSongsList, "Your Mix")
                                    is HybridMixItem.Online -> {
                                        val metadata = itemToPlay.item.toMediaMetadata()
                                        playerViewModel.playOnlineSong(metadata, listOf(metadata), "Your Mix")
                                    }
                                }
                            }
                        )
                    }
                }

                if (onlineQuickPicks.isNotEmpty()) {
                    item(key = "quick_picks_grid") {
                        QuickPicksGridSection(
                            items = onlineQuickPicks.toImmutableList(),
                            onItemClick = { itemToPlay ->
                                val metadata = itemToPlay.toMediaMetadata()
                                playerViewModel.playOnlineSong(metadata, listOf(metadata), "Quick Picks")
                            }
                        )
                    }
                }

                if (homePageUiState is HomePageUiState.Success) {
                    val sections = (homePageUiState as HomePageUiState.Success).homePage.sections
                    sections.forEachIndexed { index, section ->
                        item(key = "online_section_$index") {
                            OnlineContentSection(
                                section = section,
                                onItemClick = { item ->
                                    when (item) {
                                        is OnlineContentItem.SongContent -> {
                                            val metadata = item.toMediaMetadata()
                                            playerViewModel.playOnlineSong(
                                                metadata = metadata,
                                                contextMetadata = listOf(metadata),
                                                queueName = section.title
                                            )
                                        }
                                        is OnlineContentItem.AlbumContent -> {
                                            navController.navigate(Screen.AlbumDetail.createRoute(item.browseId))
                                        }
                                        is OnlineContentItem.PlaylistContent -> {
                                            navController.navigate(Screen.OnlinePlaylistDetail.createRoute(item.id))
                                        }
                                        is OnlineContentItem.ArtistContent -> {
                                            navController.navigate(Screen.ArtistDetail.createRoute(item.id))
                                        }
                                        else -> {}
                                    }
                                }
                            )
                        }
                    }
                }

                if (dailyMixSongs.isNotEmpty()) {
                    item(key = "daily_mix_section") {
                        DailyMixSection(
                            songs = dailyMixSongs.take(4).toImmutableList(),
                            onClickOpen = {
                                navController.navigate(Screen.DailyMixScreen.route)
                            },
                            playerViewModel = playerViewModel
                        )
                    }
                }

                if (recentlyPlayedSongs.size >= RecentlyPlayedSectionMinSongsToShow) {
                    item(key = "recently_played_section") {
                        RecentlyPlayedSection(
                            songs = recentlyPlayedSongs,
                            onSongClick = { songToPlay ->
                                if (recentlyPlayedQueue.isNotEmpty()) {
                                    playerViewModel.playSongs(
                                        songsToPlay = recentlyPlayedQueue,
                                        startSong = songToPlay,
                                        queueName = "Recently Played"
                                    )
                                }
                            },
                            onOpenAllClick = {
                                navController.navigate(Screen.RecentlyPlayed.route)
                            },
                            currentSongId = currentSong?.id,
                            contentPadding = PaddingValues(start = 8.dp, end = 24.dp)
                        )
                    }
                }

                item(key = "listening_stats_preview") {
                    StatsOverviewCard(
                        summary = weeklyStats,
                        onClick = { navController.navigate(Screen.Stats.route) }
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .height(170.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Transparent,
                            0.2f to Color.Transparent,
                            0.8f to MaterialTheme.colorScheme.surfaceContainerLowest,
                            1.0f to MaterialTheme.colorScheme.surfaceContainerLowest
                        )
                    )
                )
        ) {}
    }

    if (showOptionsBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showOptionsBottomSheet = false },
            sheetState = sheetState
        ) {
            HomeOptionsBottomSheet(
                onNavigateToMashup = {
                    scope.launch {
                        sheetState.hide()
                    }.invokeOnCompletion {
                        if (!sheetState.isVisible) {
                            showOptionsBottomSheet = false
                            navController.navigate(Screen.DJSpace.route)
                        }
                    }
                }
            )
        }
    }
    if (showChangelogBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showChangelogBottomSheet = false },
            sheetState = sheetState
        ) {
            ChangelogBottomSheet()
        }
    }
    if (showBetaInfoBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBetaInfoBottomSheet = false },
            sheetState = betaSheetState
        ) {
            BetaInfoBottomSheet()
        }
    }
    if (showUpdateBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showUpdateBottomSheet = false },
            sheetState = updateSheetState
        ) {
            UpdateBottomSheet(
                viewModel = updateViewModel,
                onDismiss = {
                    scope.launch {
                        updateSheetState.hide()
                    }.invokeOnCompletion {
                        if (!updateSheetState.isVisible) {
                            showUpdateBottomSheet = false
                        }
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun YourMixHeader(
    song: String,
    isShuffleEnabled: Boolean = false,
    onPlayShuffled: () -> Unit
) {
    val buttonCorners = 68.dp
    val colors = MaterialTheme.colorScheme
    val titleStyle = rememberYourMixTitleStyle()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(256.dp)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 48.dp, start = 12.dp)
        ) {
            Text(
                text = "Your\nMix",
                style = titleStyle,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
            )
            Text(
                text = song,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        LargeExtendedFloatingActionButton(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp),
            onClick = onPlayShuffled,
            containerColor = if (isShuffleEnabled) colors.primary else colors.tertiaryContainer,
            contentColor = if (isShuffleEnabled) colors.onPrimary else colors.onTertiaryContainer,
            shape = AbsoluteSmoothCornerShape(
                cornerRadiusTL = buttonCorners,
                smoothnessAsPercentTR = 60,
                cornerRadiusBR = buttonCorners,
                smoothnessAsPercentTL = 60,
                cornerRadiusBL = buttonCorners,
                smoothnessAsPercentBR = 60,
                cornerRadiusTR = buttonCorners,
                smoothnessAsPercentBL = 60,
            )
        ) {
            Icon(
                painter = painterResource(R.drawable.rounded_shuffle_24),
                contentDescription = "Shuffle Play",
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

@Composable
fun SongListItemFavs(
    modifier: Modifier = Modifier,
    cardCorners: Dp = 12.dp,
    title: String,
    artist: String,
    albumArtUrl: String?,
    isPlaying: Boolean,
    isCurrentSong: Boolean,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val containerColor = if (isCurrentSong) colors.primaryContainer.copy(alpha = 0.46f) else colors.surfaceContainer
    val contentColor = if (isCurrentSong) colors.primary else colors.onSurface

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(cardCorners),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(0.9f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SmartImage(
                    model = albumArtUrl,
                    contentDescription = "Album art for $title",
                    contentScale = ContentScale.Crop,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isCurrentSong) FontWeight.Bold else FontWeight.Normal,
                        color = contentColor,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = artist, style = MaterialTheme.typography.bodyMedium,
                        color = contentColor.copy(alpha = 0.7f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            if (isCurrentSong) {
                PlayingEqIcon(
                    modifier = Modifier
                        .weight(0.1f)
                        .padding(start = 8.dp)
                        .size(width = 18.dp, height = 16.dp),
                    color = colors.primary,
                    isPlaying = isPlaying
                )
            }
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun SongListItemFavsWrapper(
    song: Song,
    playerViewModel: PlayerViewModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stablePlayerState by playerViewModel.stablePlayerStateInfrequent.collectAsState()
    val currentPlayingSong = stablePlayerState.currentSong
    val isPlaying = stablePlayerState.isPlaying
    val isThisSongPlaying = remember(song.id, currentPlayingSong?.id) {
        song.id == currentPlayingSong?.id
    }

    SongListItemFavs(
        modifier = modifier,
        cardCorners = 0.dp,
        title = song.title,
        artist = song.displayArtist,
        albumArtUrl = song.albumArtUriString,
        isPlaying = isPlaying,
        isCurrentSong = isThisSongPlaying,
        onClick = onClick
    )
}

@OptIn(ExperimentalTextApi::class)
@Composable
private fun rememberYourMixTitleStyle(): TextStyle {
    return remember {
        TextStyle(
            fontFamily = FontFamily(
                Font(
                    resId = R.font.gflex_variable,
                    variationSettings = FontVariation.Settings(
                        FontVariation.weight(636),
                        FontVariation.width(152f),
                        FontVariation.Setting("ROND", 50f),
                        FontVariation.Setting("XTRA", 520f),
                        FontVariation.Setting("YOPQ", 90f),
                        FontVariation.Setting("YTLC", 505f)
                    )
                )
            ),
            fontWeight = FontWeight(760),
            fontSize = 64.sp,
            lineHeight = 62.sp,
        )
    }
}
