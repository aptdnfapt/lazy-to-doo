package com.yourname.voicetodo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Dark theme colors (BLACK background, flat design)
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF90CAF9),           // Light blue
    onPrimary = Color(0xFF000000),         // Black text on primary
    primaryContainer = Color(0xFF1E1E1E),  // Dark gray container
    onPrimaryContainer = Color(0xFFE0E0E0),

    secondary = Color(0xFFB0BEC5),         // Gray blue
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF263238),
    onSecondaryContainer = Color(0xFFE0E0E0),

    background = Color(0xFF000000),        // Pure black
    onBackground = Color(0xFFFFFFFF),      // White text

    surface = Color(0xFF121212),           // Very dark gray
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF1E1E1E),    // Slightly lighter
    onSurfaceVariant = Color(0xFFB0B0B0),

    error = Color(0xFFCF6679),
    onError = Color(0xFF000000),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    outline = Color(0xFF3A3A3A),           // Subtle borders
    outlineVariant = Color(0xFF2A2A2A)
)

// Light theme colors (WHITE background, flat design)
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1976D2),           // Blue
    onPrimary = Color(0xFFFFFFFF),         // White text on primary
    primaryContainer = Color(0xFFE3F2FD),  // Light blue container
    onPrimaryContainer = Color(0xFF0D47A1),

    secondary = Color(0xFF546E7A),         // Blue gray
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFECEFF1),
    onSecondaryContainer = Color(0xFF263238),

    background = Color(0xFFFFFFFF),        // Pure white
    onBackground = Color(0xFF000000),      // Black text

    surface = Color(0xFFFAFAFA),           // Very light gray
    onSurface = Color(0xFF000000),
    surfaceVariant = Color(0xFFF5F5F5),    // Slightly darker
    onSurfaceVariant = Color(0xFF5F5F5F),

    error = Color(0xFFB00020),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),

    outline = Color(0xFFDDDDDD),           // Subtle borders
    outlineVariant = Color(0xFFEEEEEE)
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