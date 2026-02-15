package com.jyotirmoy.musicly.ui.glancewidget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidgetManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

class WidgetUpdateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "com.example.musicly.ACTION_WIDGET_UPDATE_PLAYBACK_STATE") return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Main + SupervisorJob()).launch {
            try {
                val glanceAppWidgetManager = GlanceAppWidgetManager(context)
                val glanceIds = glanceAppWidgetManager.getGlanceIds(MusiclyGlanceWidget::class.java)
                glanceIds.forEach { glanceId ->
                    MusiclyGlanceWidget().update(context, glanceId)
                }
            } catch (e: Exception) {
                Timber.tag("WidgetUpdateReceiver").e(e, "Error updating widgets")
            } finally {
                pendingResult.finish()
            }
        }
    }
}
