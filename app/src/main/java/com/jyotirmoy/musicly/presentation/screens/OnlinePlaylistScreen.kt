package com.jyotirmoy.musicly.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.jyotirmoy.musicly.R
import com.jyotirmoy.musicly.data.model.MediaMetadata
import com.jyotirmoy.musicly.presentation.components.SmartImage
import com.jyotirmoy.musicly.presentation.components.ShimmerBox
import com.jyotirmoy.musicly.presentation.navigation.Screen
import com.jyotirmoy.musicly.presentation.viewmodel.OnlinePlaylistViewModel
import com.jyotirmoy.musicly.presentation.viewmodel.PlayerViewModel
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OnlinePlaylistScreen(
    navController: NavHostController,
    playerViewModel: PlayerViewModel,
    viewModel: OnlinePlaylistViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val stablePlayerState by playerViewModel.stablePlayerState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = uiState.title.ifBlank { "Playlist" },
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->

        when {
            uiState.isLoading -> {
                OnlinePlaylistLoadingPlaceholder(
                    modifier = Modifier.padding(innerPadding),
                )
            }

            uiState.error != null && uiState.songs.isEmpty() -> {
                OnlinePlaylistErrorState(
                    error = uiState.error!!,
                    onRetry = { viewModel.retry() },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = innerPadding.calculateTopPadding(),
                        bottom = 100.dp,
                    ),
                ) {
                    // Playlist header with thumbnail and metadata
                    item(key = "playlist_header") {
                        OnlinePlaylistHeader(
                            thumbnailUrl = uiState.thumbnailUrl,
                            title = uiState.title,
                            author = uiState.author,
                            songCount = uiState.songCount,
                            totalSongsLoaded = uiState.songs.size,
                            onPlayClick = {
                                if (uiState.songs.isNotEmpty()) {
                                    playerViewModel.playOnlineSong(
                                        uiState.songs.first(),
                                        uiState.songs,
                                        uiState.title,
                                    )
                                }
                            },
                            onShuffleClick = {
                                if (uiState.songs.isNotEmpty()) {
                                    val shuffled = uiState.songs.shuffled()
                                    playerViewModel.playOnlineSong(
                                        shuffled.first(),
                                        shuffled,
                                        uiState.title,
                                    )
                                }
                            },
                        )
                    }

                    // Song list
                    itemsIndexed(
                        items = uiState.songs,
                        key = { index, song -> "playlist_song_${song.id}_$index" },
                    ) { index, song ->
                        OnlinePlaylistSongItem(
                            song = song,
                            index = index + 1,
                            isCurrentSong = stablePlayerState.currentSong?.id == song.id,
                            onClick = {
                                playerViewModel.playOnlineSong(
                                    song,
                                    uiState.songs,
                                    uiState.title,
                                )
                            },
                            onArtistClick = { artistId ->
                                navController.navigate(Screen.ArtistDetail.createRoute(artistId))
                            },
                        )
                    }
                }
            }
        }
    }
}

// --- Playlist Header ---

private val headerArtShape = AbsoluteSmoothCornerShape(
    cornerRadiusTL = 24.dp, smoothnessAsPercentTL = 60,
    cornerRadiusTR = 24.dp, smoothnessAsPercentTR = 60,
    cornerRadiusBR = 24.dp, smoothnessAsPercentBR = 60,
    cornerRadiusBL = 24.dp, smoothnessAsPercentBL = 60,
)

@Composable
private fun OnlinePlaylistHeader(
    thumbnailUrl: String?,
    title: String,
    author: String?,
    songCount: String?,
    totalSongsLoaded: Int,
    onPlayClick: () -> Unit,
    onShuffleClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Album art
        SmartImage(
            model = thumbnailUrl,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            shape = headerArtShape,
            modifier = Modifier
                .size(240.dp)
                .clip(headerArtShape),
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Title
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        // Author
        if (!author.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = author,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Song count
        val countText = buildString {
            if (songCount != null) {
                append("$songCount songs")
            } else if (totalSongsLoaded > 0) {
                append("$totalSongsLoaded songs")
            }
            val totalDuration = totalSongsLoaded // placeholder, could compute from songs
        }
        if (countText.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = countText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Play and Shuffle buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Shuffle button
            FilledTonalIconButton(
                onClick = onShuffleClick,
                modifier = Modifier.size(52.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Shuffle,
                    contentDescription = "Shuffle",
                    modifier = Modifier.size(24.dp),
                )
            }

            // Play button (larger, primary)
            FilledTonalIconButton(
                onClick = onPlayClick,
                modifier = Modifier.size(72.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "Play all",
                    modifier = Modifier.size(36.dp),
                )
            }

            // Placeholder for symmetry (could be a menu/more button)
            Box(modifier = Modifier.size(52.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// --- Song Item ---

private val songArtShape = AbsoluteSmoothCornerShape(
    cornerRadiusTL = 10.dp, smoothnessAsPercentTL = 60,
    cornerRadiusTR = 10.dp, smoothnessAsPercentTR = 60,
    cornerRadiusBR = 10.dp, smoothnessAsPercentBR = 60,
    cornerRadiusBL = 10.dp, smoothnessAsPercentBL = 60,
)

@Composable
private fun OnlinePlaylistSongItem(
    song: MediaMetadata,
    index: Int,
    isCurrentSong: Boolean,
    onClick: () -> Unit,
    onArtistClick: (String) -> Unit,
) {
    val highlightColor = if (isCurrentSong) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    } else {
        Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(highlightColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Track number
        Text(
            text = "$index",
            style = MaterialTheme.typography.bodySmall,
            color = if (isCurrentSong) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.width(32.dp),
        )

        // Thumbnail
        SmartImage(
            model = song.thumbnailUrl,
            contentDescription = song.title,
            contentScale = ContentScale.Crop,
            shape = songArtShape,
            modifier = Modifier
                .size(48.dp)
                .clip(songArtShape),
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Title and artist
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isCurrentSong) FontWeight.Bold else FontWeight.Medium,
                color = if (isCurrentSong) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            val artistText = song.displayArtist
            if (artistText.isNotBlank()) {
                Text(
                    text = artistText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // Duration
        song.duration?.let { seconds ->
            val minutes = seconds / 60
            val secs = seconds % 60
            Text(
                text = "%d:%02d".format(minutes, secs),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        // Explicit badge
        if (song.explicit) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "E",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(3.dp),
                    )
                    .padding(horizontal = 4.dp, vertical = 1.dp),
            )
        }
    }
}

// --- Loading Placeholder ---

@Composable
private fun OnlinePlaylistLoadingPlaceholder(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Thumbnail shimmer
        ShimmerBox(
            modifier = Modifier
                .size(240.dp)
                .clip(headerArtShape),
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Title shimmer
        ShimmerBox(
            modifier = Modifier
                .width(200.dp)
                .height(24.dp)
                .clip(RoundedCornerShape(8.dp)),
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Author shimmer
        ShimmerBox(
            modifier = Modifier
                .width(120.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(6.dp)),
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Song list shimmers
        repeat(8) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ShimmerBox(
                    modifier = Modifier
                        .width(32.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp)),
                )
                Spacer(modifier = Modifier.width(8.dp))
                ShimmerBox(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(songArtShape),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp)),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(4.dp)),
                    )
                }
            }
        }
    }
}

// --- Error State ---

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun OnlinePlaylistErrorState(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.rounded_sentiment_dissatisfied_24),
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Couldn't load playlist",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 32.dp),
        )

        Spacer(modifier = Modifier.height(24.dp))

        FilledTonalButton(onClick = onRetry) {
            Text("Retry")
        }
    }
}
