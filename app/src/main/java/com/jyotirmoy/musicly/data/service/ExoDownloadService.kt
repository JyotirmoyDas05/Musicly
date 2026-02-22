package com.jyotirmoy.musicly.data.service

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.Scheduler
import com.jyotirmoy.musicly.R
import com.jyotirmoy.musicly.data.service.player.DownloadUtil
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Foreground service that drives ExoPlayer downloads.
 * Declared in AndroidManifest.xml with FOREGROUND_SERVICE_TYPE_DATA_SYNC.
 */
@OptIn(UnstableApi::class)
@AndroidEntryPoint
class ExoDownloadService : DownloadService(
    NOTIFICATION_ID,
    DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL,
    CHANNEL_ID,
    R.string.download_notification_channel_name,
    0,
) {
    @Inject
    lateinit var downloadUtil: DownloadUtil

    override fun getDownloadManager(): DownloadManager = downloadUtil.downloadManager

    override fun getScheduler(): Scheduler? = null

    override fun getForegroundNotification(
        downloads: List<Download>,
        notMetRequirements: Int
    ): android.app.Notification {
        return downloadUtil.downloadNotificationHelper.buildProgressNotification(
            this,
            R.drawable.rounded_download_24,
            null,
            null,
            downloads,
            notMetRequirements
        )
    }

    companion object {
        const val CHANNEL_ID = "musicly_download_channel"
        const val NOTIFICATION_ID = 2000
    }
}
