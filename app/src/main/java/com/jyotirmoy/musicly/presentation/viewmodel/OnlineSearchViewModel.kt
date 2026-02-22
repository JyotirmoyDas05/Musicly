package com.jyotirmoy.musicly.presentation.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jyotirmoy.musicly.data.model.OnlineContentItem
import com.jyotirmoy.musicly.data.model.OnlineSearchResult
import com.jyotirmoy.musicly.data.repository.YouTubeRepository
import com.jyotirmoy.musicly.data.repository.MusicRepository
import com.jyotirmoy.musicly.data.model.SearchHistoryItem
import com.jyotirmoy.musicly.data.model.Genre
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
    val currentFilter: OnlineSearchFilter = OnlineSearchFilter.ALL,
    val results: List<OnlineContentItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val continuation: String? = null,
    val searchHistory: List<SearchHistoryItem> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val moodsAndGenres: List<Genre> = emptyList()
)

@HiltViewModel
class OnlineSearchViewModel @Inject constructor(
    private val youTubeRepository: YouTubeRepository,
    private val musicRepository: MusicRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnlineSearchUiState())
    val uiState: StateFlow<OnlineSearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        // Observe search history from unified repository
        viewModelScope.launch {
            musicRepository.observeRecentSearchHistory().collect { history ->
                _uiState.update { it.copy(searchHistory = history.take(15)) }
            }
        }
        
        // Fetch Moods & Genres for the search tab
        viewModelScope.launch {
            try {
                // Try fetching explore page first as it's often more reliable/cached
                youTubeRepository.getExplorePage().onSuccess { homePage ->
                    val moodSection = homePage.sections.find { it.title.contains("Mood", ignoreCase = true) || it.title.contains("Genre", ignoreCase = true) }
                    val exploreGenres = moodSection?.items?.filterIsInstance<OnlineContentItem.MoodContent>()?.map {
                        val hexColor = try {
                            String.format("#%06X", (0xFFFFFF and it.stripeColor.toInt()))
                        } catch (e: Exception) {
                            "#6200EE"
                        }
                        Genre(
                            id = it.id,
                            name = it.title,
                            lightColorHex = hexColor,
                            darkColorHex = hexColor,
                            onLightColorHex = "#FFFFFF",
                            onDarkColorHex = "#FFFFFF",
                            thumbnailUrl = it.thumbnailUrl
                        )
                    } ?: emptyList()
                    
                        if (exploreGenres.isNotEmpty()) {
                            _uiState.update { it.copy(moodsAndGenres = exploreGenres) }
                        } else {
                            // Last resort: some default genres
                            val defaultGenres = listOf(
                                Genre(id = "remote_chill", name = "Chill", lightColorHex = "#4CAF50", darkColorHex = "#4CAF50"),
                                Genre(id = "remote_workout", name = "Workout", lightColorHex = "#FF5722", darkColorHex = "#FF5722"),
                                Genre(id = "remote_focus", name = "Focus", lightColorHex = "#2196F3", darkColorHex = "#2196F3"),
                                Genre(id = "remote_party", name = "Party", lightColorHex = "#E91E63", darkColorHex = "#E91E63"),
                                Genre(id = "remote_romance", name = "Romance", lightColorHex = "#F44336", darkColorHex = "#F44336"),
                                Genre(id = "remote_sleep", name = "Sleep", lightColorHex = "#9C27B0", darkColorHex = "#9C27B0"),
                                Genre(id = "remote_energetic", name = "Energetic", lightColorHex = "#FFC107", darkColorHex = "#FFC107"),
                                Genre(id = "remote_relax", name = "Relax", lightColorHex = "#00BCD4", darkColorHex = "#00BCD4"),
                                Genre(id = "remote_commute", name = "Commute", lightColorHex = "#607D8B", darkColorHex = "#607D8B")
                            )
                            _uiState.update { it.copy(moodsAndGenres = defaultGenres) }
                        }

                }
                
                // Then try the dedicated mood & genres call to get even more items
                youTubeRepository.getMoodAndGenres().onSuccess { mappedGenres ->
                    if (mappedGenres.isNotEmpty()) {
                        _uiState.update { state ->
                            val combined = (state.moodsAndGenres + mappedGenres).distinctBy { it.id }
                            state.copy(moodsAndGenres = combined)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateFilter(filter: OnlineSearchFilter) {
        _uiState.update { it.copy(currentFilter = filter) }
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

            val filter = _uiState.value.currentFilter
            val result = when (filter) {
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
                musicRepository.addSearchHistoryItem(query)
            }
        }
    }

    fun onSearchItemClicked(item: SearchHistoryItem) {
        viewModelScope.launch {
            musicRepository.addSearchHistoryItemObj(item)
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
            musicRepository.deleteSearchHistoryItemByQuery(query)
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            musicRepository.clearSearchHistory()
        }
    }
}
