package com.jyotirmoy.musicly.presentation.components.scoped

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.util.lerp

internal data class FullPlayerVisualState(
    val contentAlpha: Float,
    val translationY: Float
)

@Composable
internal fun rememberFullPlayerVisualState(
    expansionFraction: Float,
    initialOffsetY: Float
): FullPlayerVisualState {
    val fullPlayerContentAlpha by remember(expansionFraction) {
        derivedStateOf {
            (expansionFraction - 0.25f).coerceIn(0f, 0.75f) / 0.75f
        }
    }

    val fullPlayerTranslationY by remember(fullPlayerContentAlpha, initialOffsetY) {
        derivedStateOf {
            lerp(initialOffsetY, 0f, fullPlayerContentAlpha)
        }
    }

    return FullPlayerVisualState(
        contentAlpha = fullPlayerContentAlpha,
        translationY = fullPlayerTranslationY
    )
}
