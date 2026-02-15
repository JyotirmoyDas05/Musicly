package com.jyotirmoy.musicly.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.jyotirmoy.musicly.presentation.viewmodel.ColorSchemePair
import androidx.core.graphics.ColorUtils
import androidx.compose.ui.unit.dp

val LocalMusiclyDarkTheme = staticCompositionLocalOf { false }

val DarkColorScheme = darkColorScheme(
    primary = MusiclyPurplePrimary,
    secondary = MusiclyPink,
    tertiary = MusiclyOrange,
    background = MusiclyPurpleDark,
    surface = MusiclySurface,
    onPrimary = MusiclyWhite,
    onSecondary = MusiclyWhite,
    onTertiary = MusiclyWhite,
    onBackground = MusiclyWhite,
    onSurface = MusiclyLightPurple, // Texto sobre superficies
    error = Color(0xFFFF5252),
    onError = MusiclyWhite
)

val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = MusiclyWhite,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = MusiclyPink,
    onSecondary = MusiclyWhite,
    secondaryContainer = MusiclyPink.copy(alpha = 0.15f),
    onSecondaryContainer = MusiclyPink.copy(alpha = 0.85f),
    tertiary = MusiclyOrange,
    onTertiary = MusiclyBlack,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutline.copy(alpha = 0.6f),
    surfaceTint = LightPrimary,
    error = Color(0xFFD32F2F),
    onError = MusiclyWhite
)

@Composable
fun MusiclyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    colorSchemePairOverride: ColorSchemePair? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val finalColorScheme = when {
        colorSchemePairOverride == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            // System dynamic theme as priority if there is no override
            try {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } catch (e: Exception) {
                // Fallback to defaults if dynamic colors fail (rare, but possible on some devices)
                if (darkTheme) DarkColorScheme else LightColorScheme
            }
        }
        colorSchemePairOverride != null -> {
            // Use album scheme if provided
            if (darkTheme) colorSchemePairOverride.dark else colorSchemePairOverride.light
        }
        // Final fallback to defaults if no override or dynamic colors applicable
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val statusBarElevation = if (darkTheme) 4.dp else 12.dp
            val elevatedSurface = finalColorScheme.surfaceColorAtElevation(statusBarElevation)
            val statusBarColor = Color(ColorUtils.blendARGB(finalColorScheme.background.toArgb(), elevatedSurface.toArgb(), 0.35f))
            window.statusBarColor = statusBarColor.toArgb()
            val isLightStatusBar = ColorUtils.calculateLuminance(statusBarColor.toArgb()) > 0.55
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = isLightStatusBar
        }
    }

    CompositionLocalProvider(LocalMusiclyDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = finalColorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}