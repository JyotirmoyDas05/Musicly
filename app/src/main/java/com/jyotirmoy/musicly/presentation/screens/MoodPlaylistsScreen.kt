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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
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
import com.jyotirmoy.musicly.data.model.OnlineMoodSection
import com.jyotirmoy.musicly.presentation.components.SmartImage
import com.jyotirmoy.musicly.presentation.components.ShimmerBox
import com.jyotirmoy.musicly.presentation.navigation.Screen
import com.jyotirmoy.musicly.presentation.viewmodel.MoodPlaylistsViewModel
import com.jyotirmoy.musicly.presentation.viewmodel.PlayerViewModel
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MoodPlaylistsScreen(
    navController: NavHostController,
    playerViewModel: PlayerViewModel,
    moodColor: Long = 0L,
    viewModel: MoodPlaylistsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    // Derive accent color from the mood's stripe color
    val accentColor = remember(moodColor) {
        if (moodColor != 0L) {
            Color(moodColor.toInt())
        } else {
            null
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = uiState.title.ifBlank { "Mood" },
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
                    containerColor = accentColor?.copy(alpha = 0.15f) ?: Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->

        Box(modifier = Modifier.fillMaxSize()) {
            // Gradient accent at the top
            if (accentColor != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = 0.12f),
                                    Color.Transparent,
                                ),
                            )
                        ),
                )
            }

            when {
                uiState.isLoading -> {
                    MoodLoadingPlaceholder(
                        modifier = Modifier.padding(innerPadding),
                    )
                }

                uiState.error != null && uiState.sections.isEmpty() -> {
                    MoodErrorState(
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
                        verticalArrangement = Arrangement.spacedBy(28.dp),
                    ) {
                        uiState.sections.forEachIndexed { sectionIndex, section ->
                            item(key = "mood_section_$sectionIndex") {
                                MoodSectionRow(
                                    section = section,
                                    sectionIndex = sectionIndex,
                                    onItemClick = { item ->
                                        handleMoodItemClick(item, navController, playerViewModel)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MoodSectionRow(
    section: OnlineMoodSection,
    sectionIndex: Int,
    onItemClick: (OnlineContentItem) -> Unit,
) {
    Column {
        if (!section.title.isNullOrBlank()) {
            Text(
                text = section.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(
                items = section.items,
                key = { item -> "mood_s${sectionIndex}_${item.id}_${section.items.indexOf(item)}" },
            ) { item ->
                MoodPlaylistCard(
                    item = item,
                    onClick = { onItemClick(item) },
                )
            }
        }
    }
}

private val moodCardShape = AbsoluteSmoothCornerShape(
    cornerRadiusTL = 20.dp, smoothnessAsPercentTL = 60,
    cornerRadiusTR = 20.dp, smoothnessAsPercentTR = 60,
    cornerRadiusBR = 20.dp, smoothnessAsPercentBR = 60,
    cornerRadiusBL = 20.dp, smoothnessAsPercentBL = 60,
)

@Composable
private fun MoodPlaylistCard(
    item: OnlineContentItem,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(170.dp)
            .clickable(onClick = onClick),
    ) {
        SmartImage(
            model = item.thumbnailUrl,
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            shape = moodCardShape,
            modifier = Modifier
                .size(170.dp)
                .clip(moodCardShape),
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
            is OnlineContentItem.PlaylistContent -> item.author ?: ""
            is OnlineContentItem.AlbumContent -> item.artists.joinToString(", ") { it.name }
            is OnlineContentItem.ArtistContent -> "Artist"
            is OnlineContentItem.SongContent -> item.artists.joinToString(", ") { it.name }
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
private fun MoodLoadingPlaceholder(
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        items(4) {
            Column {
                ShimmerBox(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .width(140.dp)
                        .height(20.dp)
                        .clip(RoundedCornerShape(8.dp)),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    repeat(3) {
                        Column(modifier = Modifier.width(170.dp)) {
                            ShimmerBox(
                                modifier = Modifier
                                    .size(170.dp)
                                    .clip(moodCardShape),
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
private fun MoodErrorState(
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
            text = "Couldn't load playlists",
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

private fun handleMoodItemClick(
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
            playerViewModel.playOnlineSong(metadata, listOf(metadata), "Mood")
        }
        is OnlineContentItem.PlaylistContent -> {
            // Navigate to the proper online playlist detail screen
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
