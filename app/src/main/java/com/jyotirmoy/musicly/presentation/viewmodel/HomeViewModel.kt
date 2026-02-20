package com.jyotirmoy.musicly.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jyotirmoy.musicly.data.model.OnlineHomePage
import com.jyotirmoy.musicly.domain.usecase.GetHomePageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface HomePageUiState {
    object Loading : HomePageUiState
    data class Success(val homePage: OnlineHomePage) : HomePageUiState
    data class Error(val message: String) : HomePageUiState
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getHomePageUseCase: GetHomePageUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomePageUiState>(HomePageUiState.Loading)
    val uiState: StateFlow<HomePageUiState> = _uiState.asStateFlow()

    init {
        fetchHomePage()
    }

    fun fetchHomePage() {
        viewModelScope.launch {
            _uiState.update { HomePageUiState.Loading }
            getHomePageUseCase()
                .onSuccess { homePage ->
                    _uiState.update { HomePageUiState.Success(homePage) }
                }
                .onFailure { error ->
                    _uiState.update { HomePageUiState.Error(error.message ?: "Unknown error") }
                }
        }
    }
}
