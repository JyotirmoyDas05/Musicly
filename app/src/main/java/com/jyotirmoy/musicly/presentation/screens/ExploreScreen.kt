package com.jyotirmoy.musicly.presentation.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
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
import com.jyotirmoy.musicly.data.model.OnlineContentItem
import com.jyotirmoy.musicly.data.model.OnlineHomeSection
import com.jyotirmoy.musicly.presentation.components.MiniPlayerHeight
import com.jyotirmoy.musicly.presentation.components.SmartImage
import com.jyotirmoy.musicly.presentation.components.ShimmerBox
import com.jyotirmoy.musicly.presentation.navigation.Screen
import com.jyotirmoy.musicly.presentation.viewmodel.ExploreViewModel
import com.jyotirmoy.musicly.presentation.viewmodel.PlayerViewModel
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExploreScreen(
    navController: NavHostController,
    paddingValues: PaddingValues,
    playerViewModel: PlayerViewModel,
    exploreViewModel: ExploreViewModel = hiltViewModel(),
) {
    val uiState by exploreViewModel.uiState.collectAsState()
    val stablePlayerState by playerViewModel.stablePlayerStateInfrequent.collectAsState()
    val bottomPadding = if (stablePlayerState.currentSong != null) MiniPlayerHeight else 0.dp
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val lazyListState = rememberLazyListState()

    // Detect when user scrolls near the bottom to load more
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItem = lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()
            val totalItems = lazyListState.layoutInfo.totalItemsCount
            lastVisibleItem != null && lastVisibleItem.index >= totalItems - 3
        }
    }

    if (shouldLoadMore && !uiState.isLoadingMore && uiState.continuation != null) {
        exploreViewModel.loadMore()
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = "Explore",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
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

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                uiState.isLoading -> {
                    // Loading state
                    ExploreLoadingPlaceholder(
                        modifier = Modifier.padding(innerPadding),
                    )
                }

                uiState.error != null && uiState.homeSections.isEmpty() && uiState.exploreSections.isEmpty() -> {
                    // Full error state — nothing loaded
                    ExploreErrorState(
                        error = uiState.error!!,
                        onRetry = { exploreViewModel.retry() },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                    )
                }

                else -> {
                    // Content
                    val allSections = uiState.exploreSections + uiState.homeSections

                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                        contentPadding = PaddingValues(
                            top = innerPadding.calculateTopPadding(),
                            bottom = paddingValues.calculateBottomPadding() + 38.dp + bottomPadding,
                        ),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                    ) {
                        allSections.forEachIndexed { sectionIndex, section ->
                            item(key = "section_${sectionIndex}_${section.title}") {
                                ExploreSectionRow(
                                    section = section,
                                    sectionIndex = sectionIndex,
                                    onItemClick = { item -> handleItemClick(item, navController, playerViewModel) },
                                )
                            }
                        }

                        // Loading more indicator
                        if (uiState.isLoadingMore) {
                            item(key = "loading_more") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularWavyProgressIndicator(
                                        modifier = Modifier.size(32.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom fade gradient
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(170.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.surfaceContainerLowest,
                            ),
                        )
                    ),
            )
        }
    }
}

@Composable
private fun ExploreSectionRow(
    section: OnlineHomeSection,
    sectionIndex: Int,
    onItemClick: (OnlineContentItem) -> Unit,
) {
    // Check if this is a mood/genre section
    val isMoodSection = section.items.any { it is OnlineContentItem.MoodContent }

    Column {
        // Section header
        Text(
            text = section.title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )

        if (isMoodSection) {
            // Mood & Genres: 2-row horizontal scrolling grid of colored pill-shaped chips
            LazyHorizontalGrid(
                rows = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.height(116.dp),
            ) {
                items(
                    items = section.items,
                    key = { item -> "mood_${sectionIndex}_${item.id}_${section.items.indexOf(item)}" },
                ) { item ->
                    if (item is OnlineContentItem.MoodContent) {
                        MoodChipCard(
                            moodItem = item,
                            onClick = { onItemClick(item) },
                        )
                    }
                }
            }
        } else {
            // Standard content row (albums, songs, playlists, artists)
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(
                    items = section.items,
                    key = { item -> "s${sectionIndex}_${item.id}_${section.items.indexOf(item)}" },
                ) { item ->
                    ExploreContentCard(
                        item = item,
                        onClick = { onItemClick(item) },
                    )
                }
            }
        }
    }
}

/**
 * Expressive mood/genre chip with the YTMusic stripe color as background accent.
 */
@Composable
private fun MoodChipCard(
    moodItem: OnlineContentItem.MoodContent,
    onClick: () -> Unit,
) {
    val chipColor = if (moodItem.stripeColor != 0L) {
        Color(moodItem.stripeColor.toInt())
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }

    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = Modifier
            .width(160.dp)
            .height(50.dp)
            .clip(AbsoluteSmoothCornerShape(
                cornerRadiusTL = 16.dp, smoothnessAsPercentTL = 60,
                cornerRadiusTR = 16.dp, smoothnessAsPercentTR = 60,
                cornerRadiusBR = 16.dp, smoothnessAsPercentBR = 60,
                cornerRadiusBL = 16.dp, smoothnessAsPercentBL = 60,
            ))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        chipColor.copy(alpha = 0.85f),
                        chipColor.copy(alpha = 0.5f),
                    ),
                )
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = moodItem.title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private val cardShape = AbsoluteSmoothCornerShape(
    cornerRadiusTL = 16.dp, smoothnessAsPercentTL = 60,
    cornerRadiusTR = 16.dp, smoothnessAsPercentTR = 60,
    cornerRadiusBR = 16.dp, smoothnessAsPercentBR = 60,
    cornerRadiusBL = 16.dp, smoothnessAsPercentBL = 60,
)

@Composable
private fun ExploreContentCard(
    item: OnlineContentItem,
    onClick: () -> Unit,
) {
    val isWide = item is OnlineContentItem.PlaylistContent || item is OnlineContentItem.ArtistContent

    Column(
        modifier = Modifier
            .width(if (isWide) 160.dp else 148.dp)
            .clickable(onClick = onClick),
    ) {
        SmartImage(
            model = item.thumbnailUrl,
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            shape = if (item is OnlineContentItem.ArtistContent) {
                RoundedCornerShape(50)
            } else {
                cardShape
            },
            modifier = Modifier
                .size(if (isWide) 160.dp else 148.dp)
                .clip(
                    if (item is OnlineContentItem.ArtistContent) {
                        RoundedCornerShape(50)
                    } else {
                        cardShape
                    }
                ),
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        val subtitle = when (item) {
            is OnlineContentItem.SongContent -> item.artists.joinToString(", ") { it.name }
            is OnlineContentItem.AlbumContent -> item.artists.joinToString(", ") { it.name }
            is OnlineContentItem.ArtistContent -> "Artist"
            is OnlineContentItem.PlaylistContent -> item.author ?: ""
            is OnlineContentItem.MoodContent -> ""
        }
        if (subtitle.isNotEmpty()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ExploreLoadingPlaceholder(
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        items(4) { sectionIndex ->
            Column {
                // Section title shimmer
                ShimmerBox(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .width(120.dp)
                        .height(20.dp)
                        .clip(RoundedCornerShape(8.dp)),
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Row of card shimmers
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    repeat(3) {
                        Column(modifier = Modifier.width(148.dp)) {
                            ShimmerBox(
                                modifier = Modifier
                                    .size(148.dp)
                                    .clip(cardShape),
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            ShimmerBox(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            ShimmerBox(
                                modifier = Modifier
                                    .width(100.dp)
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ExploreErrorState(
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
            text = "Something went wrong",
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

private fun handleItemClick(
    item: OnlineContentItem,
    navController: NavHostController,
    playerViewModel: PlayerViewModel,
) {
    when (item) {
        is OnlineContentItem.AlbumContent -> {
            navController.navigate(Screen.AlbumDetail.createRoute(item.browseId))
        }
        is OnlineContentItem.ArtistContent -> {
            navController.navigate(Screen.ArtistDetail.createRoute(item.id))
        }
        is OnlineContentItem.SongContent -> {
            val metadata = item.toMediaMetadata()
            playerViewModel.playOnlineSong(metadata, listOf(metadata), "Explore")
        }
        is OnlineContentItem.PlaylistContent -> {
            navController.navigate(Screen.OnlinePlaylistDetail.createRoute(item.id))
        }
        is OnlineContentItem.MoodContent -> {
            navController.navigate(Screen.MoodDetail.createRoute(
                browseId = item.id,
                title = item.title,
                color = item.stripeColor,
                params = item.params,
            ))
        }
    }
}
