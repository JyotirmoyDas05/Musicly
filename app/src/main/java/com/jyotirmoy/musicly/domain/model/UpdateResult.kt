package com.jyotirmoy.musicly.domain.model

sealed class UpdateResult {
    data class Success(val release: GitHubRelease, val isUpdateAvailable: Boolean) : UpdateResult()
    object NetworkError : UpdateResult()
    object Timeout : UpdateResult()
    object UnknownError : UpdateResult()
}
