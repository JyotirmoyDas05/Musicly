package com.jyotirmoy.musicly.data.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Log
import android.util.LruCache
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.datasource.HttpDataSource
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionResult
import coil.imageLoader
import coil.request.ImageRequest
import coil.size.Size
import android.os.Bundle
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionCommands
import androidx.media3.session.SessionError
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.jyotirmoy.musicly.MainActivity
import com.jyotirmoy.musicly.MusiclyApplication
import com.jyotirmoy.musicly.R
import com.jyotirmoy.musicly.data.model.PlayerInfo // Import new data class
import com.jyotirmoy.musicly.data.repository.MusicRepository
import com.jyotirmoy.musicly.data.service.player.YouTubeMediaSourceHelper
import com.jyotirmoy.musicly.ui.glancewidget.MusiclyGlanceWidget
import com.jyotirmoy.musicly.ui.glancewidget.PlayerActions
import com.jyotirmoy.musicly.ui.glancewidget.PlayerInfoStateDefinition
import com.jyotirmoy.musicly.utils.LogUtils
import com.jyotirmoy.musicly.utils.YTPlayerUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.io.ByteArrayOutputStream
import com.jyotirmoy.musicly.data.equalizer.EqualizerManager
import com.jyotirmoy.musicly.data.preferences.UserPreferencesRepository
import com.jyotirmoy.musicly.data.service.player.DualPlayerEngine
import com.jyotirmoy.musicly.data.service.player.TransitionController
import javax.inject.Inject

// Acciones personalizadas para compatibilidad con el widget existente


@UnstableApi
@AndroidEntryPoint
class MusicService : MediaSessionService() {

    @Inject
    lateinit var engine: DualPlayerEngine
    @Inject
    lateinit var controller: TransitionController
    @Inject
    lateinit var musicRepository: MusicRepository
    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository
    @Inject
    lateinit var equalizerManager: EqualizerManager
    @Inject
    lateinit var youTubeMediaSourceHelper: YouTubeMediaSourceHelper

    private var favoriteSongIds = emptySet<String>()
    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var keepPlayingInBackground = true
    private var isManualShuffleEnabled = false
    private var persistentShuffleEnabled = false

    // --- YouTube Error Recovery State ---
    private val currentMediaIdRetryCount = mutableMapOf<String, Int>()
    private val recentlyFailedSongs = mutableSetOf<String>()
    private var failedSongsClearJob: Job? = null
    private var consecutivePlaybackErr = 0
    private var retryJob: Job? = null
    // --- Counted Play State ---
    private var countedPlayActive = false
    private var countedPlayTarget = 0
    private var countedPlayCount = 0
    private var countedOriginalId: String? = null
    private var countedPlayListener: Player.Listener? = null

    companion object {
        private const val TAG = "MusicService_Musicly"
        const val NOTIFICATION_ID = 101
        const val ACTION_SLEEP_TIMER_EXPIRED = "com.jyotirmoy.musicly.ACTION_SLEEP_TIMER_EXPIRED"

        // YouTube error recovery constants
        private const val MAX_RETRY_PER_SONG = 3
        private const val RETRY_DELAY_MS = 1000L
        private const val MAX_CONSECUTIVE_ERR = 5
        private const val FAILED_SONGS_CLEAR_DELAY_MS = 5 * 60 * 1000L // 5 minutes

        /** YouTube video IDs are 11-char base64url strings. */
        private val YOUTUBE_ID_REGEX = Regex("^[A-Za-z0-9_-]{11}$")
    }

    override fun onCreate() {
        super.onCreate()
        
        // Ensure engine is ready (re-initialize if service was restarted)
        engine.initialize()

        engine.masterPlayer.addListener(playerListener)

        // Handle player swaps (crossfade) to keep MediaSession in sync
        engine.addPlayerSwapListener { newPlayer ->
            serviceScope.launch(Dispatchers.Main) {
                val oldPlayer = mediaSession?.player
                oldPlayer?.removeListener(playerListener)

                mediaSession?.player = newPlayer
                newPlayer.addListener(playerListener)

                Timber.tag("MusicService").d("Swapped MediaSession player to new instance.")
                requestWidgetFullUpdate(force = true)
                mediaSession?.let { refreshMediaSessionUi(it) }
            }
        }

        controller.initialize()

        // Restore equalizer state from preferences and attach to audio session.
        // This ensures the equalizer is active even before the user opens the EQ screen.
        serviceScope.launch {
            val eqEnabled = userPreferencesRepository.equalizerEnabledFlow.first()
            val presetName = userPreferencesRepository.equalizerPresetFlow.first()
            val customBands = userPreferencesRepository.equalizerCustomBandsFlow.first()
            val bassBoostEnabled = userPreferencesRepository.bassBoostEnabledFlow.first()
            val bassBoostStrength = userPreferencesRepository.bassBoostStrengthFlow.first()
            val virtualizerEnabled = userPreferencesRepository.virtualizerEnabledFlow.first()
            val virtualizerStrength = userPreferencesRepository.virtualizerStrengthFlow.first()
            val loudnessEnabled = userPreferencesRepository.loudnessEnhancerEnabledFlow.first()
            val loudnessStrength = userPreferencesRepository.loudnessEnhancerStrengthFlow.first()

            equalizerManager.restoreState(
                eqEnabled, presetName, customBands,
                bassBoostEnabled, bassBoostStrength,
                virtualizerEnabled, virtualizerStrength,
                loudnessEnabled, loudnessStrength
            )

            val sessionId = engine.getAudioSessionId()
            if (sessionId != 0) {
                equalizerManager.attachToAudioSession(sessionId)
            }

            // Re-attach equalizer whenever the active audio session changes (e.g. crossfade)
            engine.activeAudioSessionId.collect { newSessionId ->
                if (newSessionId != 0) {
                    equalizerManager.attachToAudioSession(newSessionId)
                }
            }
        }

        serviceScope.launch {
            userPreferencesRepository.keepPlayingInBackgroundFlow.collect { enabled ->
                keepPlayingInBackground = enabled
            }
        }

        serviceScope.launch {
            userPreferencesRepository.persistentShuffleEnabledFlow.collect { enabled ->
                persistentShuffleEnabled = enabled
            }
        }

        // Initialize shuffle state from preferences
        serviceScope.launch {
            val persistent = userPreferencesRepository.persistentShuffleEnabledFlow.first()
            if (persistent) {
                isManualShuffleEnabled = userPreferencesRepository.isShuffleOnFlow.first()
                mediaSession?.let { refreshMediaSessionUi(it) }
            }
        }

        val callback = object : MediaSession.Callback {
            override fun onConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo
            ): MediaSession.ConnectionResult {
                val defaultResult = super.onConnect(session, controller)
                val customCommands = listOf(
                    MusicNotificationProvider.CUSTOM_COMMAND_LIKE,
                    MusicNotificationProvider.CUSTOM_COMMAND_SHUFFLE_ON,
                    MusicNotificationProvider.CUSTOM_COMMAND_SHUFFLE_OFF,
                    MusicNotificationProvider.CUSTOM_COMMAND_SET_SHUFFLE_STATE,
                    MusicNotificationProvider.CUSTOM_COMMAND_CYCLE_REPEAT_MODE,
                    MusicNotificationProvider.CUSTOM_COMMAND_COUNTED_PLAY
                ).map { SessionCommand(it, Bundle.EMPTY) }

                val sessionCommandsBuilder = SessionCommands.Builder()
                    .addSessionCommands(defaultResult.availableSessionCommands.commands)
                customCommands.forEach { sessionCommandsBuilder.add(it) }

                return MediaSession.ConnectionResult.accept(
                    sessionCommandsBuilder.build(),
                    defaultResult.availablePlayerCommands
                )
            }

            override fun onCustomCommand(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                customCommand: SessionCommand,
                args: Bundle
            ): ListenableFuture<SessionResult> {
                Timber.tag("MusicService")
                    .d("onCustomCommand received: ${customCommand.customAction}")
                when (customCommand.customAction) {
                    MusicNotificationProvider.CUSTOM_COMMAND_COUNTED_PLAY -> {
                        val count = args.getInt("count", 1)
                        startCountedPlay(count)
                    }
                    MusicNotificationProvider.CUSTOM_COMMAND_CANCEL_COUNTED_PLAY -> {
                        stopCountedPlay()
                    }
                    MusicNotificationProvider.CUSTOM_COMMAND_SHUFFLE_ON -> {
                        Timber.tag("MusicService")
                            .d("Executing SHUFFLE_ON. Current shuffleMode: ${session.player.shuffleModeEnabled}")
                        updateManualShuffleState(session, enabled = true, broadcast = true)
                    }
                    MusicNotificationProvider.CUSTOM_COMMAND_SHUFFLE_OFF -> {
                        Timber.tag("MusicService")
                            .d("Executing SHUFFLE_OFF. Current shuffleMode: ${session.player.shuffleModeEnabled}")
                        updateManualShuffleState(session, enabled = false, broadcast = true)
                    }
                    MusicNotificationProvider.CUSTOM_COMMAND_SET_SHUFFLE_STATE -> {
                        val enabled = args.getBoolean(
                            MusicNotificationProvider.EXTRA_SHUFFLE_ENABLED,
                            false
                        )
                        updateManualShuffleState(session, enabled = enabled, broadcast = false)
                    }
                    MusicNotificationProvider.CUSTOM_COMMAND_CYCLE_REPEAT_MODE -> {
                        val currentMode = session.player.repeatMode
                        val newMode = when (currentMode) {
                            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
                            Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
                            else -> Player.REPEAT_MODE_OFF
                        }
                        session.player.repeatMode = newMode
                        refreshMediaSessionUi(session)
                    }
                    MusicNotificationProvider.CUSTOM_COMMAND_LIKE -> {
                        val songId = session.player.currentMediaItem?.mediaId ?: return@onCustomCommand Futures.immediateFuture(SessionResult(
                            SessionError.ERROR_UNKNOWN))
                        Timber.tag("MusicService").d("Executing LIKE for songId: $songId")
                        val isCurrentlyFavorite = favoriteSongIds.contains(songId)
                        val targetFavoriteState = !isCurrentlyFavorite
                        favoriteSongIds = if (isCurrentlyFavorite) {
                            favoriteSongIds - songId
                        } else {
                            favoriteSongIds + songId
                        }

                        refreshMediaSessionUi(session)

                        serviceScope.launch {
                            Timber.tag("MusicService").d("Toggling favorite status for $songId")
                            musicRepository.setFavoriteStatus(songId, targetFavoriteState)
                            userPreferencesRepository.setFavoriteSong(songId, targetFavoriteState)
                            Timber.tag("MusicService")
                                .d("Toggled favorite status. Updating notification.")
                            refreshMediaSessionUi(session)
                        }
                    }
                }
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
        }

        mediaSession = MediaSession.Builder(this, engine.masterPlayer)
            .setSessionActivity(getOpenAppPendingIntent())
            .setCallback(callback)
            .setBitmapLoader(com.jyotirmoy.musicly.utils.CoilBitmapLoader(this))
            .build()

        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this)
                .build()
                .also { it.setSmallIcon(R.drawable.monochrome_player) }
        )
        mediaSession?.let { refreshMediaSessionUi(it) }

        serviceScope.launch {
            userPreferencesRepository.favoriteSongIdsFlow.collect { ids ->
                Timber.tag("MusicService")
                    .d("favoriteSongIdsFlow collected. New ids size: ${ids.size}")
                val oldIds = favoriteSongIds
                favoriteSongIds = ids
                val currentSongId = mediaSession?.player?.currentMediaItem?.mediaId
                if (currentSongId != null) {
                    val wasFavorite = oldIds.contains(currentSongId)
                    val isFavorite = ids.contains(currentSongId)
                    if (wasFavorite != isFavorite) {
                        Timber.tag("MusicService")
                            .d("Favorite status changed for current song. Updating notification.")
                        mediaSession?.let { refreshMediaSessionUi(it) }
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action?.let { action ->
            val player = mediaSession?.player ?: return@let
            when (action) {
                PlayerActions.PLAY_PAUSE -> player.playWhenReady = !player.playWhenReady
                PlayerActions.NEXT -> player.seekToNext()
                PlayerActions.PREVIOUS -> player.seekToPrevious()
                PlayerActions.PLAY_FROM_QUEUE -> {
                    val songId = intent.getLongExtra("song_id", -1L)
                    if (songId != -1L) {
                        val timeline = player.currentTimeline
                        if (!timeline.isEmpty) {
                            val window = androidx.media3.common.Timeline.Window()
                            for (i in 0 until timeline.windowCount) {
                                timeline.getWindow(i, window)
                                if (window.mediaItem.mediaId.toLongOrNull() == songId) {
                                    player.seekTo(i, C.TIME_UNSET)
                                    player.play()
                                    break
                                }
                            }
                        }
                    }
                }
                ACTION_SLEEP_TIMER_EXPIRED -> {
                    Timber.tag(TAG).d("Sleep timer expired action received. Pausing player.")
                    player.pause()
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            requestWidgetFullUpdate()
            mediaSession?.let { refreshMediaSessionUi(it) }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            Timber.tag(TAG).d("Playback state changed: $playbackState")
            mediaSession?.let { refreshMediaSessionUi(it) }

            // Reset error recovery state on successful playback
            if (playbackState == Player.STATE_READY) {
                consecutivePlaybackErr = 0
                retryJob?.cancel()
                engine.masterPlayer.currentMediaItem?.mediaId?.let { mediaId ->
                    resetRetryCount(mediaId)
                }
            }
        }

        override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
            requestWidgetFullUpdate(force = true)
            mediaSession?.let { refreshMediaSessionUi(it) }
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            Timber.tag("MusicService")
                .d("playerListener.onShuffleModeEnabledChanged: $shuffleModeEnabled")
            mediaSession?.let { refreshMediaSessionUi(it) }
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            mediaSession?.let { refreshMediaSessionUi(it) }
        }

        override fun onPlayerError(error: PlaybackException) {
            Timber.tag(TAG).e(error, "Player error occurred")
            handlePlayerError(error)
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        val allowBackground = runBlocking { userPreferencesRepository.keepPlayingInBackgroundFlow.first() }

        if (!allowBackground) {
            player?.apply {
                playWhenReady = false
                stop()
                clearMediaItems()
            }
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            super.onTaskRemoved(rootIntent)
            return
        }

        if (player == null || !player.playWhenReady || player.mediaItemCount == 0 || player.playbackState == Player.STATE_ENDED) {
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        mediaSession?.run {
            release()
            mediaSession = null
        }
        engine.release()
        controller.release()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun getOpenAppPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("ACTION_SHOW_PLAYER", true) // Signal to MainActivity to show the player
        }
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    // --- LÓGICA PARA ACTUALIZACIÓN DE WIDGETS Y DATOS ---
    private var debouncedWidgetUpdateJob: Job? = null
    private val WIDGET_STATE_DEBOUNCE_MS = 300L

    private fun requestWidgetFullUpdate(force: Boolean = false) {
        debouncedWidgetUpdateJob?.cancel()
        debouncedWidgetUpdateJob = serviceScope.launch {
            if (!force) {
                delay(WIDGET_STATE_DEBOUNCE_MS)
            }
            processWidgetUpdateInternal()
        }
    }

    private suspend fun processWidgetUpdateInternal() {
        val playerInfo = buildPlayerInfo()
        updateGlanceWidgets(playerInfo)
    }

    private suspend fun buildPlayerInfo(): PlayerInfo {
        val player = engine.masterPlayer
        val currentItem = withContext(Dispatchers.Main) { player.currentMediaItem }
        val isPlaying = withContext(Dispatchers.Main) { player.isPlaying }
        val currentPosition = withContext(Dispatchers.Main) { player.currentPosition }
        val totalDuration = withContext(Dispatchers.Main) { player.duration.coerceAtLeast(0) }

        val title = currentItem?.mediaMetadata?.title?.toString().orEmpty()
        val artist = currentItem?.mediaMetadata?.artist?.toString().orEmpty()
        val mediaId = currentItem?.mediaId
        val artworkUri = currentItem?.mediaMetadata?.artworkUri
        val artworkData = currentItem?.mediaMetadata?.artworkData

        val (artBytes, artUriString) = getAlbumArtForWidget(artworkData, artworkUri)

        val isFavorite = false
//        val isFavorite = mediaId?.let {
//            //musicRepository.getFavoriteSongs().firstOrNull()?.any { song -> song.id.toString() == it }
//        } ?: false

        val queueItems = mutableListOf<com.jyotirmoy.musicly.data.model.QueueItem>()
        val timeline = withContext(Dispatchers.Main) { player.currentTimeline }
        if (!timeline.isEmpty) {
            val window = androidx.media3.common.Timeline.Window()
            val currentWindowIndex = withContext(Dispatchers.Main) { player.currentMediaItemIndex }

            // Empezar desde la siguiente canción en la cola
            val startIndex = if (currentWindowIndex + 1 < timeline.windowCount) currentWindowIndex + 1 else 0

            // Limitar el número de elementos de la cola a 4
            val endIndex = (startIndex + 4).coerceAtMost(timeline.windowCount)
            for (i in startIndex until endIndex) {
                timeline.getWindow(i, window)
                val mediaItem = window.mediaItem
                val songId = mediaItem.mediaId.toLongOrNull()
                if (songId != null) {
                    val (artBytes, _) = getAlbumArtForWidget(
                        embeddedArt = mediaItem.mediaMetadata?.artworkData,
                        artUri = mediaItem.mediaMetadata?.artworkUri
                    )
                    queueItems.add(
                        com.jyotirmoy.musicly.data.model.QueueItem(
                            id = songId,
                            albumArtBitmapData = artBytes
                        )
                    )
                }
            }
        }

        return PlayerInfo(
            songTitle = title,
            artistName = artist,
            isPlaying = isPlaying,
            albumArtUri = artUriString,
            albumArtBitmapData = artBytes,
            currentPositionMs = currentPosition,
            totalDurationMs = totalDuration,
            isFavorite = isFavorite,
            queue = queueItems
        )
    }

    private val widgetArtByteArrayCache = LruCache<String, ByteArray>(5)

    private suspend fun getAlbumArtForWidget(embeddedArt: ByteArray?, artUri: Uri?): Pair<ByteArray?, String?> = withContext(Dispatchers.IO) {
        if (embeddedArt != null && embeddedArt.isNotEmpty()) {
            return@withContext embeddedArt to artUri?.toString()
        }
        val uri = artUri ?: return@withContext null to null
        val artUriString = uri.toString()
        val cachedArt = widgetArtByteArrayCache.get(artUriString)
        if (cachedArt != null) {
            return@withContext cachedArt to artUriString
        }
        val loadedArt = loadBitmapDataFromUri(uri = uri, context = baseContext)
        if (loadedArt != null) {
            widgetArtByteArrayCache.put(artUriString, loadedArt)
        }
        return@withContext loadedArt to artUriString
    }

    private suspend fun updateGlanceWidgets(playerInfo: PlayerInfo) = withContext(Dispatchers.IO) {
        try {
            val glanceManager = GlanceAppWidgetManager(applicationContext)
            val glanceIds = glanceManager.getGlanceIds(MusiclyGlanceWidget::class.java)
            if (glanceIds.isNotEmpty()) {
                glanceIds.forEach { id ->
                    updateAppWidgetState(applicationContext, PlayerInfoStateDefinition, id) { playerInfo }
                }
                MusiclyGlanceWidget().update(applicationContext, glanceIds.first())
                Log.d(TAG, "Widget actualizado: ${playerInfo.songTitle}")
            } else {
                Log.w(TAG, "No se encontraron widgets para actualizar")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al actualizar el widget", e)
        }
    }

    private suspend fun loadBitmapDataFromUri(context: Context, uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val request = ImageRequest.Builder(context).data(uri).size(Size(256, 256)).allowHardware(false).build()
            val drawable = context.imageLoader.execute(request).drawable
            drawable?.let {
                val bitmap = it.toBitmap(256, 256)
                val stream = ByteArrayOutputStream()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 80, stream)
                } else {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                }
                stream.toByteArray()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fallo al cargar bitmap desde URI: $uri", e)
            null
        }
    }

    fun isSongFavorite(songId: String?): Boolean {
        return songId != null && favoriteSongIds.contains(songId)
    }

    fun isManualShuffleEnabled(): Boolean {
        return isManualShuffleEnabled
    }

    private fun refreshMediaSessionUi(session: MediaSession) {
        val buttons = buildMediaButtonPreferences(session)
        session.setMediaButtonPreferences(buttons)
        onUpdateNotification(session)
    }

    private fun updateManualShuffleState(
        session: MediaSession,
        enabled: Boolean,
        broadcast: Boolean
    ) {
        val changed = isManualShuffleEnabled != enabled
        isManualShuffleEnabled = enabled
        
        if (persistentShuffleEnabled) {
            serviceScope.launch {
                userPreferencesRepository.setShuffleOn(enabled)
            }
        }

        if (broadcast && changed) {
            val args = Bundle().apply {
                putBoolean(MusicNotificationProvider.EXTRA_SHUFFLE_ENABLED, enabled)
            }
            session.broadcastCustomCommand(
                SessionCommand(MusicNotificationProvider.CUSTOM_COMMAND_SET_SHUFFLE_STATE, Bundle.EMPTY),
                args
            )
        }
        refreshMediaSessionUi(session)
    }

    private fun buildMediaButtonPreferences(session: MediaSession): List<CommandButton> {
        val player = session.player
        val songId = player.currentMediaItem?.mediaId
        val isFavorite = isSongFavorite(songId)
        val likeIcon = if (isFavorite) R.drawable.round_favorite_24 else R.drawable.round_favorite_border_24
        val likeButton = CommandButton.Builder()
            .setDisplayName("Like")
            .setIconResId(likeIcon)
            .setSessionCommand(SessionCommand(MusicNotificationProvider.CUSTOM_COMMAND_LIKE, Bundle.EMPTY))
            .build()

        val shuffleOn = isManualShuffleEnabled
        val shuffleCommandAction = if (shuffleOn) {
            MusicNotificationProvider.CUSTOM_COMMAND_SHUFFLE_OFF
        } else {
            MusicNotificationProvider.CUSTOM_COMMAND_SHUFFLE_ON
        }
        val shuffleIcon = if (shuffleOn) R.drawable.rounded_shuffle_on_24 else R.drawable.rounded_shuffle_24
        val shuffleButton = CommandButton.Builder()
            .setDisplayName("Shuffle")
            .setIconResId(shuffleIcon)
            .setSessionCommand(SessionCommand(shuffleCommandAction, Bundle.EMPTY))
            .build()

        val repeatIcon = when (player.repeatMode) {
            Player.REPEAT_MODE_ONE -> R.drawable.rounded_repeat_one_on_24
            Player.REPEAT_MODE_ALL -> R.drawable.rounded_repeat_on_24
            else -> R.drawable.rounded_repeat_24
        }
        val repeatButton = CommandButton.Builder()
            .setDisplayName("Repeat")
            .setIconResId(repeatIcon)
            .setSessionCommand(SessionCommand(MusicNotificationProvider.CUSTOM_COMMAND_CYCLE_REPEAT_MODE, Bundle.EMPTY))
            .build()

        return listOf(likeButton, shuffleButton, repeatButton)
    }

    // ------------------------
    // Counted Play Controls
    // ------------------------
    fun startCountedPlay(count: Int) {
        val player = engine.masterPlayer
        val currentItem = player.currentMediaItem ?: return

        stopCountedPlay()  // reset previous

        countedPlayTarget = count
        countedPlayCount = 1
        countedOriginalId = currentItem.mediaId
        countedPlayActive = true

        // Force repeat-one
        player.repeatMode = Player.REPEAT_MODE_ONE

        val listener = object : Player.Listener {

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                if (!countedPlayActive) return

                if (reason == Player.DISCONTINUITY_REASON_AUTO_TRANSITION) {
                    countedPlayCount++

                    if (countedPlayCount > countedPlayTarget) {
                        player.pause()
                        stopCountedPlay()
                        return
                    }
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (!countedPlayActive) return

                // If user manually changes the song -> cancel
                if (mediaItem?.mediaId != countedOriginalId) {
                    stopCountedPlay()
                }
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                // Prevent user from disabling repeat-one
                if (countedPlayActive && repeatMode != Player.REPEAT_MODE_ONE) {
                    player.repeatMode = Player.REPEAT_MODE_ONE
                }
            }
        }

        countedPlayListener = listener
        player.addListener(listener)
    }

    fun stopCountedPlay() {
        if (!countedPlayActive) return

        countedPlayActive = false
        countedPlayTarget = 0
        countedPlayCount = 0
        countedOriginalId = null

        countedPlayListener?.let {
            engine.masterPlayer.removeListener(it)
        }
        countedPlayListener = null

        // Restore normal repeat mode (OFF)
        engine.masterPlayer.repeatMode = Player.REPEAT_MODE_OFF
    }

    // ============================================================
    // YouTube Error Recovery
    // ============================================================

    /**
     * Returns true if the given mediaId looks like a YouTube video ID.
     * Local songs use numeric IDs from MediaStore, so they won't match.
     */
    private fun isYouTubeMediaId(mediaId: String?): Boolean {
        return mediaId != null && YOUTUBE_ID_REGEX.matches(mediaId)
    }

    // ---- Error detection helpers ----

    private fun getHttpResponseCode(error: PlaybackException): Int? {
        var cause: Throwable? = error
        while (cause != null) {
            if (cause is HttpDataSource.InvalidResponseCodeException) {
                return cause.responseCode
            }
            cause = cause.cause
        }
        return null
    }

    private fun isExpiredUrlError(error: PlaybackException): Boolean {
        return getHttpResponseCode(error) == 403
    }

    private fun isRangeNotSatisfiableError(error: PlaybackException): Boolean {
        return getHttpResponseCode(error) == 416
    }

    private fun isPageReloadError(error: PlaybackException): Boolean {
        val keywords = listOf("page needs to be reloaded", "reload", "page must be reloaded")
        val messages = listOfNotNull(
            error.message?.lowercase(),
            error.cause?.message?.lowercase(),
            error.cause?.cause?.message?.lowercase(),
        )
        return messages.any { msg -> keywords.any { keyword -> msg.contains(keyword) } }
    }

    private fun isNetworkRelatedError(error: PlaybackException): Boolean {
        if (isExpiredUrlError(error) || isRangeNotSatisfiableError(error) || isPageReloadError(error)) {
            return false
        }
        val errorCode = error.errorCode
        if (errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
            errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT
        ) {
            return true
        }
        var cause: Throwable? = error
        while (cause != null) {
            if (cause is java.net.ConnectException || cause is java.net.UnknownHostException) {
                return true
            }
            cause = cause.cause
        }
        return false
    }

    // ---- Retry tracking ----

    private fun hasExceededRetryLimit(mediaId: String): Boolean {
        return (currentMediaIdRetryCount[mediaId] ?: 0) >= MAX_RETRY_PER_SONG
    }

    private fun incrementRetryCount(mediaId: String) {
        currentMediaIdRetryCount[mediaId] = (currentMediaIdRetryCount[mediaId] ?: 0) + 1
    }

    private fun resetRetryCount(mediaId: String) {
        currentMediaIdRetryCount.remove(mediaId)
        recentlyFailedSongs.remove(mediaId)
    }

    private fun markSongAsFailed(mediaId: String) {
        recentlyFailedSongs.add(mediaId)
        currentMediaIdRetryCount.remove(mediaId)

        failedSongsClearJob?.cancel()
        failedSongsClearJob = serviceScope.launch {
            delay(FAILED_SONGS_CLEAR_DELAY_MS)
            recentlyFailedSongs.clear()
        }
    }

    // ---- Cache invalidation ----

    private fun performAggressiveCacheClear(mediaId: String) {
        youTubeMediaSourceHelper.clearUrlCache(mediaId)
        youTubeMediaSourceHelper.clearPlayerCache(mediaId)
        YTPlayerUtils.forceRefreshForVideo(mediaId)
    }

    // ---- Main error dispatcher ----

    private fun handlePlayerError(error: PlaybackException) {
        val player = engine.masterPlayer
        val mediaId = player.currentMediaItem?.mediaId

        // Only apply YouTube-specific recovery for YouTube content
        if (!isYouTubeMediaId(mediaId)) {
            Timber.tag(TAG).w("Non-YouTube playback error for mediaId=%s, skipping recovery", mediaId)
            return
        }

        val id = mediaId!! // Safe: isYouTubeMediaId checks non-null

        // Check retry limit first
        if (hasExceededRetryLimit(id)) {
            Timber.tag(TAG).w("Retry limit exceeded for %s. Marking as failed.", id)
            markSongAsFailed(id)
            handleFinalFailure()
            return
        }

        // Clear caches aggressively for all errors
        performAggressiveCacheClear(id)

        // Dispatch by error type
        when {
            isRangeNotSatisfiableError(error) -> handleRangeNotSatisfiableError(id)
            isPageReloadError(error) -> handlePageReloadError(id)
            isExpiredUrlError(error) -> handleExpiredUrlError(id)
            isNetworkRelatedError(error) -> handleNetworkError(id)
            else -> handleGenericError(id)
        }
    }

    // ---- Individual error handlers ----

    private fun handleExpiredUrlError(mediaId: String) {
        Timber.tag(TAG).d("Handling 403 (expired URL) for %s", mediaId)
        incrementRetryCount(mediaId)

        retryJob?.cancel()
        retryJob = serviceScope.launch {
            delay(RETRY_DELAY_MS)
            val player = engine.masterPlayer
            val pos = player.currentPosition
            player.seekTo(pos) // Forces URL re-resolution
            player.prepare()
            player.play()
        }
    }

    private fun handleRangeNotSatisfiableError(mediaId: String) {
        Timber.tag(TAG).d("Handling 416 (range not satisfiable) for %s", mediaId)
        incrementRetryCount(mediaId)

        retryJob?.cancel()
        retryJob = serviceScope.launch {
            delay(RETRY_DELAY_MS)
            val player = engine.masterPlayer
            player.seekTo(0) // Reset to start to avoid range issues
            player.prepare()
            player.play()
        }
    }

    private fun handlePageReloadError(mediaId: String) {
        Timber.tag(TAG).d("Handling page reload error for %s", mediaId)
        incrementRetryCount(mediaId)

        retryJob?.cancel()
        retryJob = serviceScope.launch {
            delay(RETRY_DELAY_MS * 2) // Longer delay for rate-limiting
            val player = engine.masterPlayer
            val pos = player.currentPosition
            player.seekTo(pos)
            player.prepare()
            player.play()
        }
    }

    private fun handleNetworkError(mediaId: String) {
        Timber.tag(TAG).d("Handling network error for %s. Waiting for connectivity.", mediaId)
        incrementRetryCount(mediaId)

        retryJob?.cancel()
        retryJob = serviceScope.launch {
            // Wait longer for network errors — the connection might recover
            delay(RETRY_DELAY_MS * 3)
            val player = engine.masterPlayer
            val pos = player.currentPosition
            player.seekTo(pos)
            player.prepare()
            player.play()
        }
    }

    private fun handleGenericError(mediaId: String) {
        Timber.tag(TAG).d("Handling generic IO error for %s", mediaId)
        incrementRetryCount(mediaId)

        retryJob?.cancel()
        retryJob = serviceScope.launch {
            delay(RETRY_DELAY_MS)
            val player = engine.masterPlayer
            val pos = player.currentPosition
            player.seekTo(pos)
            player.prepare()
            player.play()
        }
    }

    // ---- Final failure (retries exhausted) ----

    private fun handleFinalFailure() {
        // Auto-skip to next or stop
        skipOnError()
    }

    private fun skipOnError() {
        val player = engine.masterPlayer
        consecutivePlaybackErr += 2

        val nextWindowIndex = player.nextMediaItemIndex
        if (consecutivePlaybackErr <= MAX_CONSECUTIVE_ERR && nextWindowIndex != C.INDEX_UNSET) {
            Timber.tag(TAG).d("Skipping to next song (consecutive errors: %d)", consecutivePlaybackErr)
            player.seekTo(nextWindowIndex, C.TIME_UNSET)
            player.prepare()
            player.play()
        } else {
            // Too many consecutive skips — force stop to prevent runaway
            Timber.tag(TAG).w("Too many consecutive errors (%d). Stopping playback.", consecutivePlaybackErr)
            player.pause()
            consecutivePlaybackErr = 0
        }
    }

}
