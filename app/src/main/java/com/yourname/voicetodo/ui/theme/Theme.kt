package com.yourname.voicetodo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Dark theme colors matching HTML
private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = White,
    primaryContainer = Gray800,
    onPrimaryContainer = White,

    secondary = Gray200,
    onSecondary = Black,
    secondaryContainer = Gray800,
    onSecondaryContainer = White,

    background = BackgroundDark,
    onBackground = White,

    surface = BackgroundDark,
    onSurface = White,
    surfaceVariant = Gray800,
    onSurfaceVariant = Gray200,

    error = Color(0xFFCF6679),
    onError = Black,
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    outline = Gray800,
    outlineVariant = Gray200
)

// Light theme colors matching HTML
private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = White,
    primaryContainer = Gray200,
    onPrimaryContainer = Black,

    secondary = Gray800,
    onSecondary = White,
    secondaryContainer = Gray200,
    onSecondaryContainer = Black,

    background = BackgroundLight,
    onBackground = Black,

    surface = BackgroundLight,
    onSurface = Black,
    surfaceVariant = Gray200,
    onSurfaceVariant = Gray800,

    error = Color(0xFFB00020),
    onError = White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),

    outline = Gray200,
    outlineVariant = Gray800
)

@Composable
fun VoiceTodoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
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