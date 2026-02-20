package com.jyotirmoy.musicly.data.repository

import android.content.Context
import android.util.Log
import com.jyotirmoy.musicly.domain.model.DownloadState
import com.jyotirmoy.musicly.domain.repository.DownloadRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readRawBytes
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import java.io.File
import javax.inject.Inject

class DownloadRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val httpClient: HttpClient
) : DownloadRepository {

    override suspend fun downloadApk(
        url: String,
        fileName: String,
        onProgress: (DownloadState) -> Unit
    ) {
        try {
            onProgress(DownloadState.Started)

            val response: HttpResponse = httpClient.get(url)

            if (response.status != HttpStatusCode.OK) {
                onProgress(DownloadState.Error("Failed to download: ${response.status}"))
                return
            }

            // Read the entire response as bytes
            val bytes = response.readRawBytes()
            
            val file = File(context.getExternalFilesDir(null), fileName)
            file.outputStream().use { output ->
                output.write(bytes)
            }

            Log.d("DownloadRepository", "Download complete: ${file.absolutePath}")
            onProgress(DownloadState.Success(file))

        } catch (e: CancellationException) {
            Log.d("DownloadRepository", "Download cancelled")
            onProgress(DownloadState.Cancelled)
        } catch (e: Exception) {
            Log.e("DownloadRepository", "Download error: ${e.message}", e)
            onProgress(DownloadState.Error(e.message ?: "Unknown error"))
        }
    }

    override fun cancelDownload() {
        Log.d("DownloadRepository", "Cancelling download")
    }
}
