package com.jyotirmoy.musicly.presentation.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jyotirmoy.musicly.data.model.OnlineContentItem
import com.jyotirmoy.musicly.data.model.OnlineSearchResult
import com.jyotirmoy.musicly.data.repository.YouTubeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class OnlineSearchFilter {
    ALL, SONGS, ALBUMS, ARTISTS, PLAYLISTS
}

@Immutable
data class OnlineSearchUiState(
    val query: String = "",
    val filter: OnlineSearchFilter = OnlineSearchFilter.ALL,
    val results: List<OnlineContentItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val continuation: String? = null,
    val searchHistory: List<String> = emptyList(),
    val suggestions: List<String> = emptyList(),
)

@HiltViewModel
class OnlineSearchViewModel @Inject constructor(
    private val youTubeRepository: YouTubeRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnlineSearchUiState())
    val uiState: StateFlow<OnlineSearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        // Observe search history
        viewModelScope.launch {
            youTubeRepository.observeSearchHistory().collect { history ->
                _uiState.update { it.copy(searchHistory = history) }
            }
        }
    }

    fun updateFilter(filter: OnlineSearchFilter) {
        _uiState.update { it.copy(filter = filter) }
        if (_uiState.value.query.isNotBlank()) {
            performSearch(_uiState.value.query)
        }
    }

    fun performSearch(query: String) {
        searchJob?.cancel()
        _uiState.update { it.copy(query = query) }

        if (query.isBlank()) {
            _uiState.update { it.copy(results = emptyList(), isLoading = false, error = null) }
            return
        }

        searchJob = viewModelScope.launch {
            delay(350) // debounce
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = when (_uiState.value.filter) {
                OnlineSearchFilter.ALL -> youTubeRepository.searchSongs(query)
                OnlineSearchFilter.SONGS -> youTubeRepository.searchSongs(query)
                OnlineSearchFilter.ALBUMS -> youTubeRepository.searchAlbums(query)
                OnlineSearchFilter.ARTISTS -> youTubeRepository.searchArtists(query)
                OnlineSearchFilter.PLAYLISTS -> youTubeRepository.searchPlaylists(query)
            }

            result.fold(
                onSuccess = { searchResult ->
                    _uiState.update {
                        it.copy(
                            results = searchResult.items,
                            continuation = searchResult.continuation,
                            isLoading = false,
                            error = null,
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.localizedMessage ?: "Search failed",
                        )
                    }
                }
            )
        }
    }

    fun loadMore() {
        val continuation = _uiState.value.continuation ?: return
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            youTubeRepository.searchContinuation(continuation).fold(
                onSuccess = { searchResult ->
                    _uiState.update {
                        it.copy(
                            results = it.results + searchResult.items,
                            continuation = searchResult.continuation,
                            isLoading = false,
                        )
                    }
                },
                onFailure = {
                    _uiState.update { it.copy(isLoading = false) }
                }
            )
        }
    }

    fun onSearchSubmitted(query: String) {
        if (query.isNotBlank()) {
            viewModelScope.launch {
                youTubeRepository.saveSearchQuery(query)
            }
        }
    }

    fun fetchSuggestions(query: String) {
        if (query.isBlank()) {
            _uiState.update { it.copy(suggestions = emptyList()) }
            return
        }
        viewModelScope.launch {
            youTubeRepository.searchSuggestions(query).fold(
                onSuccess = { suggestions ->
                    _uiState.update {
                        it.copy(suggestions = suggestions.queries)
                    }
                },
                onFailure = {
                    _uiState.update { it.copy(suggestions = emptyList()) }
                }
            )
        }
    }

    fun deleteSearchHistoryItem(query: String) {
        viewModelScope.launch {
            youTubeRepository.deleteSearchQuery(query)
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            youTubeRepository.clearSearchHistory()
        }
    }
}
