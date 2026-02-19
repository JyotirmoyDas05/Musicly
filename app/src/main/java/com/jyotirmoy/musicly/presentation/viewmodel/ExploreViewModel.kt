package com.jyotirmoy.musicly.presentation.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jyotirmoy.musicly.data.model.OnlineHomeSection
import com.jyotirmoy.musicly.data.repository.YouTubeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import javax.inject.Inject

@Immutable
data class ExploreUiState(
    val homeSections: List<OnlineHomeSection> = emptyList(),
    val exploreSections: List<OnlineHomeSection> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val continuation: String? = null,
)

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val youTubeRepository: YouTubeRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    init {
        loadInitial()
    }

    private fun loadInitial() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // Load home feed and explore page in parallel
            val homeDeferred = async { youTubeRepository.getHome() }
            val exploreDeferred = async { youTubeRepository.getExplorePage() }

            val homeResult = homeDeferred.await()
            val exploreResult = exploreDeferred.await()

            val homeError = homeResult.exceptionOrNull()
            val exploreError = exploreResult.exceptionOrNull()

            _uiState.update { state ->
                state.copy(
                    homeSections = homeResult.getOrNull()?.sections ?: emptyList(),
                    exploreSections = exploreResult.getOrNull()?.sections ?: emptyList(),
                    continuation = homeResult.getOrNull()?.continuation,
                    isLoading = false,
                    error = when {
                        homeError != null && exploreError != null ->
                            "Failed to load content: ${homeError.localizedMessage ?: "Unknown error"}"
                        homeError != null ->
                            "Home feed unavailable: ${homeError.localizedMessage ?: "Unknown error"}"
                        exploreError != null ->
                            null // Explore failing is non-critical if home loaded
                        else -> null
                    }
                )
            }
        }
    }

    fun loadMore() {
        val currentContinuation = _uiState.value.continuation ?: return
        if (_uiState.value.isLoadingMore) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }

            youTubeRepository.getHome(currentContinuation).fold(
                onSuccess = { page ->
                    _uiState.update { state ->
                        state.copy(
                            homeSections = state.homeSections + page.sections,
                            continuation = page.continuation,
                            isLoadingMore = false,
                        )
                    }
                },
                onFailure = {
                    _uiState.update { it.copy(isLoadingMore = false) }
                }
            )
        }
    }

    fun retry() {
        loadInitial()
    }
}
