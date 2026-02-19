package com.jyotirmoy.musicly.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jyotirmoy.musicly.data.model.Album
import com.jyotirmoy.musicly.data.model.MediaMetadata
import com.jyotirmoy.musicly.data.model.OnlineAlbumDetail
import com.jyotirmoy.musicly.data.model.Song
import com.jyotirmoy.musicly.data.repository.MusicRepository
import com.jyotirmoy.musicly.data.repository.YouTubeRepository
import com.jyotirmoy.musicly.R
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlbumDetailUiState(
    val album: Album? = null,
    val songs: List<Song> = emptyList(),
    /** Online album detail — set when navigating with a YouTube browseId */
    val onlineAlbum: OnlineAlbumDetail? = null,
    /** Online songs — set when navigating with a YouTube browseId */
    val onlineSongs: List<MediaMetadata> = emptyList(),
    val isOnline: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val musicRepository: MusicRepository,
    private val youTubeRepository: YouTubeRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlbumDetailUiState())
    val uiState: StateFlow<AlbumDetailUiState> = _uiState.asStateFlow()

    init {
        val albumIdString: String? = savedStateHandle.get("albumId")
        if (albumIdString != null) {
            val albumId = albumIdString.toLongOrNull()
            if (albumId != null) {
                // Local album (numeric ID from MediaStore)
                loadAlbumData(albumId)
            } else {
                // Online album (YouTube browseId like "MPREb_...")
                loadOnlineAlbumData(albumIdString)
            }
        } else {
            _uiState.update { it.copy(error = context.getString(R.string.album_id_not_found), isLoading = false) }
        }
    }

    private fun loadAlbumData(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val albumDetailsFlow = musicRepository.getAlbumById(id)
                val albumSongsFlow = musicRepository.getSongsForAlbum(id)

                combine(albumDetailsFlow, albumSongsFlow) { album, songs ->
                    if (album != null) {
                        AlbumDetailUiState(
                            album = album,
                            songs = songs.sortedBy { it.trackNumber },
                            isLoading = false
                        )
                    } else {
                        AlbumDetailUiState(
                            error = context.getString(R.string.album_not_found),
                            isLoading = false
                        )
                    }
                }
                    .catch { e ->
                        emit(
                            AlbumDetailUiState(
                                error = context.getString(R.string.error_loading_album, e.localizedMessage ?: ""),
                                isLoading = false
                            )
                        )
                    }
                    .collect { newState ->
                        _uiState.value = newState
                    }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = context.getString(R.string.error_loading_album, e.localizedMessage ?: ""),
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun loadOnlineAlbumData(browseId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, isOnline = true) }

            youTubeRepository.getAlbumDetails(browseId).fold(
                onSuccess = { detail ->
                    _uiState.update {
                        it.copy(
                            onlineAlbum = detail,
                            onlineSongs = detail.songs,
                            isLoading = false,
                            isOnline = true,
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            error = "Failed to load album: ${e.localizedMessage ?: "Unknown error"}",
                            isLoading = false,
                        )
                    }
                }
            )
        }
    }

    fun update(songs: List<Song>) {
        _uiState.update {
            it.copy(
                isLoading = false,
                songs = songs
            )
        }
    }
}
