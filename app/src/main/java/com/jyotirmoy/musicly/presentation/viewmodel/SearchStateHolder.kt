package com.jyotirmoy.musicly.presentation.viewmodel

import android.util.Log
import com.jyotirmoy.musicly.data.model.SearchFilterType
import com.jyotirmoy.musicly.data.model.SearchHistoryItem
import com.jyotirmoy.musicly.data.model.SearchResultItem
import com.jyotirmoy.musicly.data.repository.MusicRepository
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages search state and operations.
 * Extracted from PlayerViewModel to improve modularity.
 *
 * Responsibilities:
 * - Search query execution
 * - Search filter management
 * - Search history CRUD operations
 */
@Singleton
class SearchStateHolder @Inject constructor(
    private val musicRepository: MusicRepository
) {
    // Search State
    private val _searchResults = MutableStateFlow<ImmutableList<SearchResultItem>>(persistentListOf())
    val searchResults = _searchResults.asStateFlow()

    private val _selectedSearchFilter = MutableStateFlow(SearchFilterType.ALL)
    val selectedSearchFilter = _selectedSearchFilter.asStateFlow()

    private val _searchHistory = MutableStateFlow<ImmutableList<SearchHistoryItem>>(persistentListOf())
    val searchHistory = _searchHistory.asStateFlow()

    private var scope: CoroutineScope? = null

    /**
     * Initialize with ViewModel scope.
     */
    fun initialize(scope: CoroutineScope) {
        this.scope = scope
        observeSearchHistory()
    }

    private fun observeSearchHistory() {
        scope?.launch {
            musicRepository.observeRecentSearchHistory().collect { history ->
                _searchHistory.value = history.take(20).toImmutableList()
            }
        }
    }

    fun updateSearchFilter(filterType: SearchFilterType) {
        _selectedSearchFilter.value = filterType
    }

    fun loadSearchHistory(limit: Int = 15) {
        // No-op since we are observing
    }

    fun onSearchQuerySubmitted(query: String) {
        scope?.launch {
            if (query.isNotBlank()) {
                try {
                    withContext(Dispatchers.IO) {
                        musicRepository.addSearchHistoryItem(query)
                    }
                } catch (e: Exception) {
                    Log.e("SearchStateHolder", "Error adding search history item", e)
                }
            }
        }
    }

    fun onSearchItemClicked(item: SearchHistoryItem) {
        scope?.launch {
            try {
                withContext(Dispatchers.IO) {
                    musicRepository.addSearchHistoryItemObj(item)
                }
            } catch (e: Exception) {
                Log.e("SearchStateHolder", "Error adding search history item", e)
            }
        }
    }

    fun performSearch(query: String) {
        scope?.launch {
            try {
                if (query.isBlank()) {
                    _searchResults.value = persistentListOf()
                    return@launch
                }

                val currentFilter = _selectedSearchFilter.value
                val resultsList = withContext(Dispatchers.IO) {
                    musicRepository.searchAll(query, currentFilter).first()
                }

                _searchResults.value = resultsList.toImmutableList()

            } catch (e: Exception) {
                Log.e("SearchStateHolder", "Error performing search for query: $query", e)
                _searchResults.value = persistentListOf()
            }
        }
    }

    fun deleteSearchHistoryItem(query: String) {
        scope?.launch {
            try {
                withContext(Dispatchers.IO) {
                    musicRepository.deleteSearchHistoryItemByQuery(query)
                }
            } catch (e: Exception) {
                Log.e("SearchStateHolder", "Error deleting search history item", e)
            }
        }
    }

    fun clearSearchHistory() {
        scope?.launch {
            try {
                withContext(Dispatchers.IO) {
                    musicRepository.clearSearchHistory()
                }
                _searchHistory.value = persistentListOf()
            } catch (e: Exception) {
                Log.e("SearchStateHolder", "Error clearing search history", e)
            }
        }
    }

    fun onCleared() {
        scope = null
    }
}
