package com.jyotirmoy.musicly.utils

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import java.io.File

fun openUrl(context: Context, url: String) {
    val uri = try { 
        url.toUri() 
    } catch (_: Throwable) { 
        Uri.parse(url) 
    }
    val intent = Intent(Intent.ACTION_VIEW, uri)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // As a last resort, do nothing
    }
}

fun Activity.installApk(apkFile: File) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(
            androidx.core.content.FileProvider.getUriForFile(
                this@installApk,
                "${packageName}.fileProvider",
                apkFile
            ),
            "application/vnd.android.package-archive"
        )
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
