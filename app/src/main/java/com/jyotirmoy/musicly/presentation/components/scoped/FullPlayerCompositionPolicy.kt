package com.jyotirmoy.musicly.presentation.components.scoped

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jyotirmoy.musicly.presentation.viewmodel.PlayerSheetState
import kotlinx.coroutines.delay

internal data class FullPlayerCompositionPolicy(
    val shouldRenderFullPlayer: Boolean
)

@Composable
internal fun rememberFullPlayerCompositionPolicy(
    currentSongId: String?,
    currentSheetState: PlayerSheetState,
    expansionFraction: Float,
    releaseDelayMs: Long = 450L
): FullPlayerCompositionPolicy {
    var keepFullPlayerComposed by remember(currentSongId) { mutableStateOf(false) }

    LaunchedEffect(currentSongId, currentSheetState) {
        if (currentSongId == null) {
            keepFullPlayerComposed = false
            return@LaunchedEffect
        }

        if (currentSheetState == PlayerSheetState.EXPANDED) {
            keepFullPlayerComposed = true
        } else {
            delay(releaseDelayMs)
            if (currentSheetState == PlayerSheetState.COLLAPSED) {
                keepFullPlayerComposed = false
            }
        }
    }

    LaunchedEffect(currentSongId, expansionFraction) {
        if (currentSongId != null && expansionFraction > 0.12f) {
            keepFullPlayerComposed = true
        }
    }

    val shouldRenderFullPlayer by remember(
        currentSongId,
        currentSheetState,
        expansionFraction,
        keepFullPlayerComposed
    ) {
        derivedStateOf {
            currentSongId != null && (
                currentSheetState == PlayerSheetState.EXPANDED ||
                    expansionFraction > 0.015f ||
                    keepFullPlayerComposed
                )
        }
    }

    return FullPlayerCompositionPolicy(
        shouldRenderFullPlayer = shouldRenderFullPlayer
    )
}
