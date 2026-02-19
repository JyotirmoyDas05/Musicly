package com.jyotirmoy.musicly.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.annotation.OptIn
import androidx.core.graphics.drawable.toBitmap
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import coil.imageLoader
import coil.request.ImageRequest
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.withContext

/**
 * A [BitmapLoader] implementation that uses Coil to load images.
 * This is used by Media3 session to resolve artwork URIs into Bitmaps for notification and lock screen controls.
 */
@OptIn(UnstableApi::class)
class CoilBitmapLoader(private val context: Context) : BitmapLoader {

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> {
        return scope.future {
            BitmapFactory.decodeByteArray(data, 0, data.size)
                ?: throw IllegalArgumentException("Could not decode bitmap from byte array")
        }
    }

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> {
        return scope.future {
            val result = withContext(Dispatchers.Main) {
                val request = ImageRequest.Builder(context)
                    .data(uri)
                    .allowHardware(false) // Software bitmaps are required for notifications/remote views
                    .size(512, 512) // Rational size limit for notifications
                    .build()
                context.imageLoader.execute(request)
            }
            result.drawable?.toBitmap() ?: throw IllegalStateException("Coil returned null drawable for $uri")
        }
    }

    override fun supportsMimeType(mimeType: String): Boolean {
        return true
    }
}
