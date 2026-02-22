package com.jyotirmoy.musicly.data.service.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.DatabaseProvider
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import com.jyotirmoy.musicly.data.database.OnlineDao
import com.jyotirmoy.musicly.di.DownloadCache
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import timber.log.Timber
import java.util.concurrent.Executor
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton wrapper around ExoPlayer's [DownloadManager] for explicit user downloads.
 *
 * Lifecycle: inject in long-lived components (e.g. Application or a Hilt Singleton).
 * Call [release] when the component is destroyed.
 */
@OptIn(UnstableApi::class)
@Singleton
class DownloadUtil @Inject constructor(
    @ApplicationContext private val context: Context,
    @DownloadCache val downloadCache: SimpleCache,
    val databaseProvider: StandaloneDatabaseProvider,
    private val onlineDao: OnlineDao,
    private val youTubeMediaSourceHelper: YouTubeMediaSourceHelper,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _downloads = MutableStateFlow<Map<String, Download>>(emptyMap())
    val downloads = _downloads.asStateFlow()

    val downloadNotificationHelper =
        DownloadNotificationHelper(context, "musicly_download_channel")

    val downloadManager: DownloadManager by lazy {
        DownloadManager(
            context,
            databaseProvider,
            downloadCache,
            youTubeMediaSourceHelper.createDataSourceFactory(),
            Executor(Runnable::run),
        ).apply {
            maxParallelDownloads = 3
            addListener(object : DownloadManager.Listener {
                override fun onDownloadChanged(
                    downloadManager: DownloadManager,
                    download: Download,
                    finalException: Exception?,
                ) {
                    _downloads.update { map ->
                        map.toMutableMap().apply { set(download.request.id, download) }
                    }
                    syncStateToDb(download)
                }

                override fun onDownloadRemoved(
                    downloadManager: DownloadManager,
                    download: Download,
                ) {
                    _downloads.update { map ->
                        map.toMutableMap().apply { remove(download.request.id) }
                    }
                    scope.launch {
                        onlineDao.updateDownloadState(download.request.id, false, null)
                    }
                }
            })
        }
    }

    init {
        // Restore downloads map from the download index on startup
        scope.launch {
            val result = mutableMapOf<String, Download>()
            val cursor = downloadManager.downloadIndex.getDownloads()
            while (cursor.moveToNext()) {
                result[cursor.download.request.id] = cursor.download
            }
            _downloads.value = result
        }
    }

    private fun syncStateToDb(download: Download) {
        scope.launch {
            when (download.state) {
                Download.STATE_COMPLETED -> {
                    onlineDao.updateDownloadState(
                        download.request.id,
                        true,
                        System.currentTimeMillis(),
                    )
                    Timber.d("Download complete: %s", download.request.id)
                }
                Download.STATE_FAILED,
                Download.STATE_REMOVING -> {
                    onlineDao.updateDownloadState(download.request.id, false, null)
                }
                else -> {}
            }
        }
    }

    /** Whether a specific song is fully downloaded. */
    fun isDownloaded(songId: String): Boolean {
        val download = _downloads.value[songId]
        return download?.state == Download.STATE_COMPLETED
    }

    /** Download progress [0..1] for the given song, or null if not downloading. */
    fun progressFor(songId: String): Float? {
        val download = _downloads.value[songId] ?: return null
        return when (download.state) {
            Download.STATE_DOWNLOADING -> download.percentDownloaded / 100f
            Download.STATE_COMPLETED -> 1f
            else -> null
        }
    }

    fun release() {
        scope.cancel()
    }
}
