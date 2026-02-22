package com.jyotirmoy.musicly.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.jyotirmoy.musicly.R
import com.jyotirmoy.musicly.data.model.Song
import com.jyotirmoy.musicly.presentation.components.MiniPlayerHeight
import com.jyotirmoy.musicly.presentation.components.NavBarContentHeight
import com.jyotirmoy.musicly.presentation.components.subcomps.EnhancedSongListItem
import com.jyotirmoy.musicly.presentation.viewmodel.LibraryViewModel
import com.jyotirmoy.musicly.presentation.viewmodel.PlayerViewModel
import com.jyotirmoy.musicly.ui.theme.GoogleSansRounded
import com.jyotirmoy.musicly.ui.theme.LocalMusiclyDarkTheme
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryOfflineScreen(
    navController: NavController,
    title: String,
    songsFlow: Flow<List<Song>>,
    playerViewModel: PlayerViewModel = hiltViewModel<PlayerViewModel>(),
    libraryViewModel: LibraryViewModel = hiltViewModel<LibraryViewModel>()
) {
    val songs by songsFlow.collectAsState(initial = emptyList())
    val stablePlayerState by playerViewModel.stablePlayerStateInfrequent.collectAsState()
    val dm = LocalMusiclyDarkTheme.current
    
    val gradientColors = if (dm) {
        listOf(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), Color.Transparent)
    } else {
        listOf(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f), Color.Transparent)
    }

    val systemNavBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomBarHeightDp = NavBarContentHeight + systemNavBarInset
    val listState = rememberLazyListState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        fontFamily = GoogleSansRounded,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 24.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = gradientColors[0]
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Brush.verticalGradient(gradientColors))
        ) {
            if (songs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            painter = painterResource(id = R.drawable.rounded_music_off_24),
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No songs found",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp)
                        .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = bottomBarHeightDp + MiniPlayerHeight + 32.dp, top = 16.dp)
                ) {
                    items(songs, key = { it.id }) { song ->
                        EnhancedSongListItem(
                            song = song,
                            isCurrentSong = stablePlayerState.currentSong?.id == song.id,
                            isPlaying = stablePlayerState.currentSong?.id == song.id && stablePlayerState.isPlaying,
                            onMoreOptionsClick = {
                                playerViewModel.selectSongForInfo(song)
                                // Handle more options if needed, or unify with LibraryScreen
                            },
                            onClick = {
                                playerViewModel.showAndPlaySong(song, songs, title)
                            }
                        )
                    }
                }
            }
        }
    }
}
