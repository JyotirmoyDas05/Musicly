package com.jyotirmoy.musicly.domain.repository

import com.jyotirmoy.musicly.domain.model.DownloadState

interface DownloadRepository {
    suspend fun downloadApk(url: String, fileName: String, onProgress: (DownloadState) -> Unit)
    fun cancelDownload()
}
