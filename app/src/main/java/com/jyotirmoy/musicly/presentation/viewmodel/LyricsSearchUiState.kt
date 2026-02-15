package com.jyotirmoy.musicly.presentation.viewmodel

import com.jyotirmoy.musicly.data.model.Lyrics
import com.jyotirmoy.musicly.data.repository.LyricsSearchResult

sealed interface LyricsSearchUiState {
    object Idle : LyricsSearchUiState
    object Loading : LyricsSearchUiState
    data class PickResult(val query: String, val results: List<LyricsSearchResult>) : LyricsSearchUiState
    data class Success(val lyrics: Lyrics) : LyricsSearchUiState
    data class NotFound(val message: String, val allowManualSearch: Boolean = true) : LyricsSearchUiState
    data class Error(val message: String, val query: String? = null) : LyricsSearchUiState
}
