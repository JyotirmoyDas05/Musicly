package com.jyotirmoy.musicly.presentation.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jyotirmoy.musicly.data.model.OnlineMoodSection
import com.jyotirmoy.musicly.data.repository.YouTubeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class MoodPlaylistsUiState(
    val title: String = "",
    val sections: List<OnlineMoodSection> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class MoodPlaylistsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val youTubeRepository: YouTubeRepository,
) : ViewModel() {

    private val browseId: String = savedStateHandle["browseId"] ?: ""
    private val params: String? = savedStateHandle.get<String>("params")?.takeIf { it.isNotBlank() }
    private val moodTitle: String = savedStateHandle.get<String>("title")?.let {
        try { java.net.URLDecoder.decode(it, "UTF-8") } catch (_: Exception) { it }
    } ?: ""

    private val _uiState = MutableStateFlow(MoodPlaylistsUiState(title = moodTitle))
    val uiState: StateFlow<MoodPlaylistsUiState> = _uiState.asStateFlow()

    init {
        loadMoodPlaylists()
    }

    private fun loadMoodPlaylists() {
        if (browseId.isBlank()) {
            _uiState.update { it.copy(error = "Invalid mood/genre ID") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            youTubeRepository.browseMood(browseId, params).fold(
                onSuccess = { moodDetail ->
                    _uiState.update { state ->
                        state.copy(
                            title = moodDetail.title ?: state.title,
                            sections = moodDetail.sections,
                            isLoading = false,
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.localizedMessage ?: "Failed to load playlists",
                        )
                    }
                },
            )
        }
    }

    fun retry() {
        loadMoodPlaylists()
    }
}
