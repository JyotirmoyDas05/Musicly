package com.jyotirmoy.musicly.ui.glancewidget

import android.content.Context
import android.content.Intent
import com.jyotirmoy.musicly.MainActivity

object IntentProvider {
    fun mainActivityIntent(context: Context): Intent {
        val intent = Intent(context, MainActivity::class.java)
        // ACTION_MAIN and CATEGORY_LAUNCHER are typical for starting the main activity.
        // If the app is already running, these flags help bring it to the front.
        intent.action = Intent.ACTION_MAIN
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        // FLAG_ACTIVITY_NEW_TASK is necessary if started from a non-Activity context (like an AppWidgetProvider).
        // FLAG_ACTIVITY_REORDER_TO_FRONT will bring the existing task to the front if it's already running,
        // instead of launching a new instance on top if launchMode allows it.
        // If MainActivity has launchMode="singleTop", onNewIntent will be called if it's already at the top.
        // If it has launchMode="singleTask" or "singleInstance", it will behave according to those modes.
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        return intent
    }
}
