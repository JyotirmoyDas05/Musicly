package com.jyotirmoy.musicly.domain.usecase

import com.jyotirmoy.musicly.data.model.OnlineSearchResult
import com.jyotirmoy.musicly.data.repository.YouTubeRepository
import com.metrolist.innertube.models.SearchSuggestions
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for searching YouTube Music for online content.
 *
 * Encapsulates search logic including saving search history
 * and delegating to the repository for API calls.
 */
class SearchOnlineUseCase @Inject constructor(
    private val youTubeRepository: YouTubeRepository,
) {
    /** Get search suggestions (autocomplete) without saving to history. */
    suspend fun getSuggestions(query: String): Result<SearchSuggestions> {
        return youTubeRepository.searchSuggestions(query)
    }

    /** Search for songs and save the query to history. */
    suspend fun searchSongs(query: String, saveToHistory: Boolean = true): Result<OnlineSearchResult> {
        if (saveToHistory && query.isNotBlank()) {
            youTubeRepository.saveSearchQuery(query)
        }
        return youTubeRepository.searchSongs(query)
    }

    /** Search for albums and save the query to history. */
    suspend fun searchAlbums(query: String, saveToHistory: Boolean = true): Result<OnlineSearchResult> {
        if (saveToHistory && query.isNotBlank()) {
            youTubeRepository.saveSearchQuery(query)
        }
        return youTubeRepository.searchAlbums(query)
    }

    /** Search for artists and save the query to history. */
    suspend fun searchArtists(query: String, saveToHistory: Boolean = true): Result<OnlineSearchResult> {
        if (saveToHistory && query.isNotBlank()) {
            youTubeRepository.saveSearchQuery(query)
        }
        return youTubeRepository.searchArtists(query)
    }

    /** Search for playlists and save the query to history. */
    suspend fun searchPlaylists(query: String, saveToHistory: Boolean = true): Result<OnlineSearchResult> {
        if (saveToHistory && query.isNotBlank()) {
            youTubeRepository.saveSearchQuery(query)
        }
        return youTubeRepository.searchPlaylists(query)
    }

    /** Load more search results from a continuation token. */
    suspend fun loadMore(continuation: String): Result<OnlineSearchResult> {
        return youTubeRepository.searchContinuation(continuation)
    }

    /** Observe recent online search history. */
    fun observeSearchHistory(limit: Int = 20): Flow<List<String>> {
        return youTubeRepository.observeSearchHistory(limit)
    }

    /** Delete a specific query from search history. */
    suspend fun deleteFromHistory(query: String) {
        youTubeRepository.deleteSearchQuery(query)
    }

    /** Clear all online search history. */
    suspend fun clearHistory() {
        youTubeRepository.clearSearchHistory()
    }
}
