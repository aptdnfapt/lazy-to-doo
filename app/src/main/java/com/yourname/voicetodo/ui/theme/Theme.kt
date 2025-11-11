package com.yourname.voicetodo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Professional black-based dark theme
private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = White,
    primaryContainer = PrimaryVariant,
    onPrimaryContainer = White,

    secondary = MutedGray,
    onSecondary = White,
    secondaryContainer = SurfaceVariant,
    onSecondaryContainer = OnSurface,

    background = Black,
    onBackground = OnSurface,

    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceSecondary,

    inverseSurface = NearBlack,
    inverseOnSurface = OnSurface,
    inversePrimary = PrimaryVariant,

    error = Error,
    onError = White,
    errorContainer = Color(0xFF2A1515),
    onErrorContainer = Color(0xFFFFB4B4),

    outline = LightGray,
    outlineVariant = DarkGray,

    scrim = Color(0x80000000),  // Semi-transparent black overlay
    surfaceTint = Primary
)

// Refined light theme with better contrast
private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = White,
    primaryContainer = PrimaryVariant,
    onPrimaryContainer = White,

    secondary = Gray800,
    onSecondary = White,
    secondaryContainer = Gray200,
    onSecondaryContainer = Black,

    background = BackgroundLight,
    onBackground = Black,

    surface = White,
    onSurface = Black,
    surfaceVariant = Gray200,
    onSurfaceVariant = Gray800,

    inverseSurface = Gray800,
    inverseOnSurface = White,
    inversePrimary = PrimaryVariant,

    error = Error,
    onError = White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),

    outline = Gray200,
    outlineVariant = Gray800,

    scrim = Color(0x40000000),
    surfaceTint = Primary
)

@Composable
fun VoiceTodoTheme(
    darkTheme: Boolean = true,  // Force dark mode as default
    useDynamicColors: Boolean = false,  // Set to false for custom themes
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,  // Flat design shapes
        content = content
    )
}