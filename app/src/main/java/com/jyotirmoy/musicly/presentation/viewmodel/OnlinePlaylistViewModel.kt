package com.jyotirmoy.musicly.presentation.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jyotirmoy.musicly.data.model.MediaMetadata
import com.jyotirmoy.musicly.data.repository.YouTubeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@Immutable
data class OnlinePlaylistUiState(
    val playlistId: String = "",
    val title: String = "",
    val author: String? = null,
    val authorId: String? = null,
    val thumbnailUrl: String? = null,
    val songCount: String? = null,
    val songs: List<MediaMetadata> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class OnlinePlaylistViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val youTubeRepository: YouTubeRepository,
) : ViewModel() {

    private val playlistId: String = savedStateHandle["playlistId"] ?: ""

    private val _uiState = MutableStateFlow(OnlinePlaylistUiState(playlistId = playlistId))
    val uiState: StateFlow<OnlinePlaylistUiState> = _uiState.asStateFlow()

    /** Continuation token for loading more songs. Accessible for queue playback. */
    private var songsContinuation: String? = null
    private var continuation: String? = null

    init {
        fetchPlaylist()
    }

    private fun fetchPlaylist() {
        if (playlistId.isBlank()) {
            _uiState.update { it.copy(error = "Invalid playlist ID") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            youTubeRepository.getOnlinePlaylistDetails(playlistId).fold(
                onSuccess = { detail ->
                    songsContinuation = detail.songsContinuation
                    continuation = detail.continuation
                    _uiState.update { state ->
                        state.copy(
                            title = detail.title,
                            author = detail.author?.name,
                            authorId = detail.author?.id,
                            thumbnailUrl = detail.thumbnailUrl,
                            songCount = detail.songCount,
                            songs = detail.songs,
                            isLoading = false,
                        )
                    }
                    // Proactively load remaining songs in background
                    loadRemainingSongs()
                },
                onFailure = { error ->
                    Timber.tag("OnlinePlaylistVM").e(error, "Failed to load playlist $playlistId")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.localizedMessage ?: "Failed to load playlist",
                        )
                    }
                },
            )
        }
    }

    /**
     * Proactively loads all remaining songs in the background using continuation tokens,
     * following Metrolist's pattern. This ensures the full playlist is available for
     * queue playback without requiring manual "load more" actions.
     */
    private fun loadRemainingSongs() {
        val token = songsContinuation ?: continuation ?: return

        viewModelScope.launch {
            var currentToken: String? = token
            while (currentToken != null) {
                youTubeRepository.getOnlinePlaylistContinuation(currentToken).fold(
                    onSuccess = { continuationResult ->
                        _uiState.update { state ->
                            // Deduplicate by ID
                            val existingIds = state.songs.map { it.id }.toSet()
                            val newSongs = continuationResult.songs.filter { it.id !in existingIds }
                            state.copy(songs = state.songs + newSongs)
                        }
                        currentToken = continuationResult.continuation
                    },
                    onFailure = { error ->
                        Timber.tag("OnlinePlaylistVM")
                            .w(error, "Failed to load continuation, stopping background load")
                        currentToken = null
                    },
                )
            }
        }
    }

    fun retry() {
        fetchPlaylist()
    }
}
