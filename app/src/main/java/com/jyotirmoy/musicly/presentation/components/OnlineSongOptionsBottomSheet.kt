package com.jyotirmoy.musicly.presentation.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.MoreVert

import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jyotirmoy.musicly.data.model.MediaMetadata
import com.jyotirmoy.musicly.data.model.OnlineContentItem
import com.jyotirmoy.musicly.presentation.components.subcomps.AutoSizingTextToFill
import com.jyotirmoy.musicly.utils.formatDuration
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

/**
 * Context in which the online song options bottom sheet is displayed.
 * Different contexts show different options.
 */
enum class OnlineSongMenuContext {
    /** Shown when tapping 3-dots on a search result song */
    SEARCH,
    /** Shown when tapping options on the currently playing song in the player */
    PLAYER,
    /** Shown when tapping 3-dots on a song in the queue */
    QUEUE,
}

/**
 * Options bottom sheet for online YouTube Music songs.
 *
 * Follows the same M3 Expressive design language as [SongInfoBottomSheet] but with
 * actions appropriate for online songs (no Delete, no Edit, has Share YT Music link, Start Radio).
 *
 * Different options are shown based on [menuContext]:
 * - SEARCH: Play, Add to Playlist, Download, Add to Queue, Play Next, Start Radio, Share
 * - PLAYER: Start Radio, Add to Playlist, Download, Share
 * - QUEUE: Play Next, Add to Playlist, Download, Share, Remove from Queue
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OnlineSongOptionsBottomSheet(
    metadata: MediaMetadata,
    menuContext: OnlineSongMenuContext = OnlineSongMenuContext.SEARCH,
    onDismiss: () -> Unit,
    onPlaySong: () -> Unit,
    onAddToQueue: () -> Unit,
    onPlayNext: () -> Unit,
    onStartRadio: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onDownload: () -> Unit,
    onRemoveFromQueue: (() -> Unit)? = null,
    onNavigateToArtist: ((String) -> Unit)? = null,
    onNavigateToAlbum: ((String) -> Unit)? = null,
) {
    val context = LocalContext.current
    val ytMusicLink = "https://music.youtube.com/watch?v=${metadata.id}"

    val evenCornerRadiusElems = 26.dp

    val albumArtShape = AbsoluteSmoothCornerShape(
        cornerRadiusTR = evenCornerRadiusElems, smoothnessAsPercentBR = 60, cornerRadiusBR = evenCornerRadiusElems,
        smoothnessAsPercentTL = 60, cornerRadiusTL = evenCornerRadiusElems, smoothnessAsPercentBL = 60,
        cornerRadiusBL = evenCornerRadiusElems, smoothnessAsPercentTR = 60
    )
    val playButtonShape = AbsoluteSmoothCornerShape(
        cornerRadiusTR = evenCornerRadiusElems, smoothnessAsPercentBR = 60, cornerRadiusBR = evenCornerRadiusElems,
        smoothnessAsPercentTL = 60, cornerRadiusTL = evenCornerRadiusElems, smoothnessAsPercentBL = 60,
        cornerRadiusBL = evenCornerRadiusElems, smoothnessAsPercentTR = 60
    )
    val listItemShape = AbsoluteSmoothCornerShape(
        cornerRadiusTR = 20.dp, smoothnessAsPercentBR = 60, cornerRadiusBR = 20.dp,
        smoothnessAsPercentTL = 60, cornerRadiusTL = 20.dp, smoothnessAsPercentBL = 60,
        cornerRadiusBL = 20.dp, smoothnessAsPercentTR = 60
    )

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { true }
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Box(
            modifier = Modifier
                .animateContentSize(
                    animationSpec = tween(durationMillis = 200),
                    alignment = Alignment.BottomCenter
                )
                .fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header: Album art + title
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SmartImage(
                            model = metadata.thumbnailUrl,
                            contentDescription = "Album Art",
                            shape = albumArtShape,
                            modifier = Modifier.size(80.dp),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Column {
                                AutoSizingTextToFill(
                                    modifier = Modifier.padding(end = 4.dp),
                                    fontWeight = FontWeight.Light,
                                    text = metadata.title
                                )
                                Text(
                                    text = metadata.displayArtist,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(4.dp)) }

                // Quick action buttons row (context-dependent)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        when (menuContext) {
                            OnlineSongMenuContext.SEARCH -> {
                                // Play button
                                MediumExtendedFloatingActionButton(
                                    modifier = Modifier
                                        .weight(0.5f)
                                        .fillMaxHeight(),
                                    onClick = {
                                        onPlaySong()
                                        onDismiss()
                                    },
                                    elevation = FloatingActionButtonDefaults.elevation(0.dp),
                                    shape = playButtonShape,
                                    icon = {
                                        Icon(Icons.Rounded.PlayArrow, contentDescription = "Play song")
                                    },
                                    text = {
                                        Text(
                                            modifier = Modifier.padding(end = 10.dp),
                                            text = "Play"
                                        )
                                    }
                                )
                                // Add to Playlist
                                FilledTonalIconButton(
                                    modifier = Modifier
                                        .weight(0.25f)
                                        .fillMaxHeight(),
                                    onClick = {
                                        onAddToPlaylist()
                                        onDismiss()
                                    },
                                    shape = CircleShape,
                                ) {
                                    Icon(
                                        modifier = Modifier.size(FloatingActionButtonDefaults.LargeIconSize),
                                        imageVector = Icons.Rounded.PlaylistAdd,
                                        contentDescription = "Add to Playlist"
                                    )
                                }
                                // Download
                                FilledTonalIconButton(
                                    modifier = Modifier
                                        .weight(0.25f)
                                        .fillMaxHeight(),
                                    onClick = {
                                        onDownload()
                                        onDismiss()
                                    },
                                    shape = CircleShape,
                                ) {
                                    Icon(
                                        modifier = Modifier.size(FloatingActionButtonDefaults.LargeIconSize),
                                        imageVector = Icons.Rounded.Download,
                                        contentDescription = "Download"
                                    )
                                }
                            }
                            OnlineSongMenuContext.PLAYER -> {
                                // Start Radio (seamless)
                                MediumExtendedFloatingActionButton(
                                    modifier = Modifier
                                        .weight(0.5f)
                                        .fillMaxHeight(),
                                    onClick = {
                                        onStartRadio()
                                        onDismiss()
                                    },
                                    elevation = FloatingActionButtonDefaults.elevation(0.dp),
                                    shape = playButtonShape,
                                    icon = {
                                        Icon(Icons.Rounded.Radio, contentDescription = "Start Radio")
                                    },
                                    text = {
                                        Text(
                                            modifier = Modifier.padding(end = 10.dp),
                                            text = "Radio"
                                        )
                                    }
                                )
                                // Add to Playlist
                                FilledTonalIconButton(
                                    modifier = Modifier
                                        .weight(0.25f)
                                        .fillMaxHeight(),
                                    onClick = {
                                        onAddToPlaylist()
                                        onDismiss()
                                    },
                                    shape = CircleShape,
                                ) {
                                    Icon(
                                        modifier = Modifier.size(FloatingActionButtonDefaults.LargeIconSize),
                                        imageVector = Icons.Rounded.PlaylistAdd,
                                        contentDescription = "Add to Playlist"
                                    )
                                }
                                // Download
                                FilledTonalIconButton(
                                    modifier = Modifier
                                        .weight(0.25f)
                                        .fillMaxHeight(),
                                    onClick = {
                                        onDownload()
                                        onDismiss()
                                    },
                                    shape = CircleShape,
                                ) {
                                    Icon(
                                        modifier = Modifier.size(FloatingActionButtonDefaults.LargeIconSize),
                                        imageVector = Icons.Rounded.Download,
                                        contentDescription = "Download"
                                    )
                                }
                            }
                            OnlineSongMenuContext.QUEUE -> {
                                // Play Next
                                MediumExtendedFloatingActionButton(
                                    modifier = Modifier
                                        .weight(0.5f)
                                        .fillMaxHeight(),
                                    onClick = {
                                        onPlayNext()
                                        onDismiss()
                                    },
                                    elevation = FloatingActionButtonDefaults.elevation(0.dp),
                                    shape = playButtonShape,
                                    icon = {
                                        Icon(Icons.Rounded.PlayArrow, contentDescription = "Play Next")
                                    },
                                    text = {
                                        Text(
                                            modifier = Modifier.padding(end = 10.dp),
                                            text = "Play Next"
                                        )
                                    }
                                )
                                // Add to Playlist
                                FilledTonalIconButton(
                                    modifier = Modifier
                                        .weight(0.25f)
                                        .fillMaxHeight(),
                                    onClick = {
                                        onAddToPlaylist()
                                        onDismiss()
                                    },
                                    shape = CircleShape,
                                ) {
                                    Icon(
                                        modifier = Modifier.size(FloatingActionButtonDefaults.LargeIconSize),
                                        imageVector = Icons.Rounded.PlaylistAdd,
                                        contentDescription = "Add to Playlist"
                                    )
                                }
                                // Download
                                FilledTonalIconButton(
                                    modifier = Modifier
                                        .weight(0.25f)
                                        .fillMaxHeight(),
                                    onClick = {
                                        onDownload()
                                        onDismiss()
                                    },
                                    shape = CircleShape,
                                ) {
                                    Icon(
                                        modifier = Modifier.size(FloatingActionButtonDefaults.LargeIconSize),
                                        imageVector = Icons.Rounded.Download,
                                        contentDescription = "Download"
                                    )
                                }
                            }
                        }
                    }
                }

                // Queue buttons (Add to Queue / Play Next) - shown in SEARCH context
                if (menuContext == OnlineSongMenuContext.SEARCH) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FilledTonalButton(
                                modifier = Modifier
                                    .weight(0.6f)
                                    .heightIn(min = 66.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                ),
                                contentPadding = PaddingValues(horizontal = 0.dp),
                                shape = CircleShape,
                                onClick = {
                                    onAddToQueue()
                                    onDismiss()
                                }
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.QueueMusic,
                                    contentDescription = "Add to Queue"
                                )
                                Spacer(Modifier.width(14.dp))
                                Text("Add to Queue")
                            }
                            FilledTonalButton(
                                modifier = Modifier
                                    .weight(0.4f)
                                    .heightIn(min = 66.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiary,
                                    contentColor = MaterialTheme.colorScheme.onTertiary
                                ),
                                contentPadding = PaddingValues(horizontal = 0.dp),
                                shape = CircleShape,
                                onClick = {
                                    onPlayNext()
                                    onDismiss()
                                }
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.QueueMusic,
                                    contentDescription = "Play Next"
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Next")
                            }
                        }
                    }
                }

                // Start Radio + context-specific action
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (menuContext != OnlineSongMenuContext.PLAYER) {
                            // Start Radio button (in SEARCH and QUEUE contexts)
                            FilledTonalButton(
                                modifier = Modifier
                                    .weight(0.5f)
                                    .heightIn(min = 66.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                shape = CircleShape,
                                onClick = {
                                    onStartRadio()
                                    onDismiss()
                                }
                            ) {
                                Icon(Icons.Rounded.Radio, contentDescription = "Start Radio")
                                Spacer(Modifier.width(8.dp))
                                Text("Radio")
                            }
                        } else {
                            // In PLAYER context, we already have Radio as the main button.
                            // Show "Share" here instead.
                            FilledTonalButton(
                                modifier = Modifier
                                    .weight(0.5f)
                                    .heightIn(min = 66.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                shape = CircleShape,
                                onClick = {
                                    shareYtMusicLink(context, ytMusicLink, metadata.title)
                                    onDismiss()
                                }
                            ) {
                                Icon(Icons.Rounded.Share, contentDescription = "Share")
                                Spacer(Modifier.width(8.dp))
                                Text("Share")
                            }
                        }

                        if (menuContext == OnlineSongMenuContext.QUEUE && onRemoveFromQueue != null) {
                            // Remove from Queue button
                            FilledTonalButton(
                                modifier = Modifier
                                    .weight(0.5f)
                                    .heightIn(min = 66.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                ),
                                shape = CircleShape,
                                onClick = {
                                    onRemoveFromQueue()
                                    onDismiss()
                                }
                            ) {
                                Icon(Icons.Rounded.RemoveCircleOutline, contentDescription = "Remove")
                                Spacer(Modifier.width(8.dp))
                                Text("Remove")
                            }
                        } else if (menuContext == OnlineSongMenuContext.PLAYER) {
                            // Add to Queue in player context
                            FilledTonalButton(
                                modifier = Modifier
                                    .weight(0.5f)
                                    .heightIn(min = 66.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                ),
                                shape = CircleShape,
                                onClick = {
                                    onAddToQueue()
                                    onDismiss()
                                }
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = "Add to Queue")
                                Spacer(Modifier.width(8.dp))
                                Text("Queue")
                            }
                        } else if (menuContext == OnlineSongMenuContext.SEARCH) {
                           // In SEARCH context, we have Radio on left. Right slot can be Share.
                            FilledTonalButton(
                                modifier = Modifier
                                    .weight(0.5f)
                                    .heightIn(min = 66.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                shape = CircleShape,
                                onClick = {
                                    shareYtMusicLink(context, ytMusicLink, metadata.title)
                                    onDismiss()
                                }
                            ) {
                                Icon(Icons.Rounded.Share, contentDescription = "Share")
                                Spacer(Modifier.width(8.dp))
                                Text("Share")
                            }
                        }
                    }
                }

                // Details section
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Duration
                if (metadata.duration != null) {
                    item {
                        ListItem(
                            modifier = Modifier.clip(shape = listItemShape),
                            headlineContent = { Text("Duration") },
                            supportingContent = { Text(formatDuration(metadata.duration.toLong() * 1000)) },
                            leadingContent = {
                                Icon(Icons.Rounded.Schedule, contentDescription = "Duration icon")
                            }
                        )
                    }
                }

                // Artist
                if (metadata.artists.isNotEmpty()) {
                    item {
                        ListItem(
                            modifier = Modifier
                                .clip(shape = listItemShape)
                                .then(
                                    if (onNavigateToArtist != null && metadata.artists.firstOrNull()?.id != null) {
                                        Modifier.let { mod ->
                                            val artistId = metadata.artists.first().id!!
                                            mod
                                        }
                                    } else Modifier
                                ),
                            headlineContent = { Text("Artist") },
                            supportingContent = { Text(metadata.displayArtist) },
                            leadingContent = {
                                Icon(Icons.Rounded.Person, contentDescription = "Artist icon")
                            }
                        )
                    }
                }

                // Album
                if (metadata.album != null) {
                    item {
                        ListItem(
                            modifier = Modifier.clip(shape = listItemShape),
                            headlineContent = { Text("Album") },
                            supportingContent = { Text(metadata.album.title) },
                            leadingContent = {
                                Icon(Icons.Rounded.Album, contentDescription = "Album icon")
                            }
                        )
                    }
                }

                // Bottom spacer for navigation bar
                item {
                    Spacer(Modifier.height(80.dp))
                }
            }
        }
    }
}

/** Share a YouTube Music link via the system share sheet. */
private fun shareYtMusicLink(context: Context, link: String, title: String) {
    try {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, link)
            putExtra(Intent.EXTRA_SUBJECT, title)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Song Link"))
    } catch (e: Exception) {
        Toast.makeText(context, "Could not share link: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}
