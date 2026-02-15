package com.jyotirmoy.musicly

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.jyotirmoy.musicly.data.worker.SyncManager
import com.jyotirmoy.musicly.utils.CrashHandler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import android.os.StrictMode // Importar StrictMode
import coil.ImageLoader
import coil.ImageLoaderFactory
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import timber.log.Timber

@HiltAndroidApp
class MusiclyApplication : Application(), ImageLoaderFactory, Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var imageLoader: dagger.Lazy<ImageLoader>

    // ADD COMPANION OBJECT
    companion object {
        const val NOTIFICATION_CHANNEL_ID = "musicly_music_channel"
    }

    override fun onCreate() {
        super.onCreate()

        // Benchmark variant intentionally restarts/kills app process during tests.
        // Avoid persisting those events as user-facing crash reports.
        if (BuildConfig.BUILD_TYPE != "benchmark") {
            CrashHandler.install(this)
        }

//        if (BuildConfig.DEBUG) {
//            Timber.plant(Timber.DebugTree())
//            StrictMode.setThreadPolicy(
//                StrictMode.ThreadPolicy.Builder()
//                    .detectDiskReads()
//                    .detectDiskWrites()
//                    .detectNetwork()
//                    .penaltyLog()
//                    .build()
//            )
//            StrictMode.setVmPolicy(
//                StrictMode.VmPolicy.Builder()
//                    .detectLeakedSqlLiteObjects()
//                    .detectLeakedClosableObjects()
//                    .penaltyLog()
//                    .build()
//            )
//        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Musicly Music Playback",
                NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun newImageLoader(): ImageLoader {
        return imageLoader.get()
    }

    // 3. Override method to provide WorkManager configuration
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

}
