package com.jyotirmoy.musicly.domain.usecase

import com.jyotirmoy.musicly.domain.model.DownloadState
import com.jyotirmoy.musicly.domain.repository.DownloadRepository
import javax.inject.Inject

class DownloadApkUseCase @Inject constructor(
    private val repo: DownloadRepository
) {
    suspend operator fun invoke(
        url: String,
        fileName: String,
        onProgress: (DownloadState) -> Unit
    ) {
        repo.downloadApk(url, fileName, onProgress)
    }

    fun cancel() {
        repo.cancelDownload()
    }
}
