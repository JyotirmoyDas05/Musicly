package com.jyotirmoy.musicly.presentation.components.scoped

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.jyotirmoy.musicly.presentation.viewmodel.PlayerSheetState

internal data class FullPlayerRuntimePolicy(
    val allowRealtimeUpdates: Boolean
)

@Composable
internal fun rememberFullPlayerRuntimePolicy(
    currentSheetState: PlayerSheetState,
    expansionFraction: Float,
    fullPlayerContentAlpha: Float,
    bottomSheetOpenFraction: Float
): FullPlayerRuntimePolicy {
    val isOccludedByOverlay by remember(bottomSheetOpenFraction) {
        derivedStateOf {
            // Queue/Cast overlays start occluding meaningful portions of the full player very early.
            bottomSheetOpenFraction >= 0.08f
        }
    }

    val allowRealtimeUpdates by remember(
        currentSheetState,
        expansionFraction,
        fullPlayerContentAlpha,
        isOccludedByOverlay
    ) {
        derivedStateOf {
            currentSheetState == PlayerSheetState.EXPANDED &&
                expansionFraction >= 0.985f &&
                fullPlayerContentAlpha >= 0.95f &&
                !isOccludedByOverlay
        }
    }

    return FullPlayerRuntimePolicy(
        allowRealtimeUpdates = allowRealtimeUpdates
    )
}
