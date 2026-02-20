package com.jyotirmoy.musicly.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jyotirmoy.musicly.data.preferences.UserPreferencesRepository
import com.jyotirmoy.musicly.data.repository.MusicRepository
import com.jyotirmoy.musicly.data.worker.SyncManager
import com.jyotirmoy.musicly.data.worker.SyncProgress
import com.jyotirmoy.musicly.utils.LogUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val syncManager: SyncManager,
    musicRepository: MusicRepository,
    userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val isSetupComplete: StateFlow<Boolean> = userPreferencesRepository.initialSetupDoneFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val hasCompletedInitialSync: StateFlow<Boolean> = userPreferencesRepository.lastSyncTimestampFlow
        .map { it > 0L }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    /**
     * A Flow that emits `true` if the SyncWorker is queued or running.
     * Ideal for showing a loading indicator.
     */
    val isSyncing: StateFlow<Boolean> = syncManager.isSyncing
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    /**
     * Flow that exposes detailed sync progress including file count and phase.
     */
    val syncProgress: StateFlow<SyncProgress> = syncManager.syncProgress
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SyncProgress()
        )

    /**
     * A Flow that emits `true` if the Room database has no songs.
     * Helps us know if the app is being opened for the first time.
     */
    val isLibraryEmpty: StateFlow<Boolean> = musicRepository
        .getAudioFiles()
        .map { it.isEmpty() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    /**
     * Function to start syncing the music library.
     * Should be called after permissions have been granted.
     */
    fun startSync() {
        LogUtils.i(this, "startSync called")
        viewModelScope.launch {
            // For fresh installs after setup, SetupViewModel.setSetupComplete() triggers sync
            // For returning users (setup already complete), we trigger sync here
            if (isSetupComplete.value) {
                syncManager.sync()
            }
        }
    }
}
