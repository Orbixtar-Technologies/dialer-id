package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = RoyalBlue600,
    onPrimary = PureWhite,
    primaryContainer = RoyalBlue50,
    onPrimaryContainer = RoyalBlue800,
    secondary = Slate800,
    onSecondary = PureWhite,
    secondaryContainer = Slate100,
    onSecondaryContainer = Slate900,
    tertiary = Emerald500,
    onTertiary = PureWhite,
    tertiaryContainer = Emerald50,
    onTertiaryContainer = Emerald600,
    background = Slate50,
    onBackground = Slate900,
    surface = PureWhite,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate700,
    outline = Slate200,
    outlineVariant = Slate300,
    error = Rose500,
    onError = PureWhite,
    errorContainer = Rose50,
    onErrorContainer = Rose600
)

private val DarkColorScheme = darkColorScheme(
    primary = RoyalBlue600,
    onPrimary = PureWhite,
    primaryContainer = Slate800,
    onPrimaryContainer = RoyalBlue100,
    secondary = Slate200,
    onSecondary = Slate900,
    secondaryContainer = Slate800,
    onSecondaryContainer = Slate100,
    tertiary = Emerald500,
    onTertiary = PureWhite,
    tertiaryContainer = Slate800,
    onTertiaryContainer = Emerald500,
    background = Slate900,
    onBackground = Slate50,
    surface = Slate800,
    onSurface = Slate50,
    surfaceVariant = Slate700,
    onSurfaceVariant = Slate200,
    outline = Slate600,
    outlineVariant = Slate700,
    error = Rose500,
    onError = PureWhite,
    errorContainer = Slate800,
    onErrorContainer = Rose500
)

@Composable
fun DialerIDTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // DialerID uses clean Light Mode by default as per spec, but supports dark gracefully
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
