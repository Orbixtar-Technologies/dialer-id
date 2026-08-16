package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Light scheme.
 *
 * Every `on*` role is picked against the container it is drawn on, targeting
 * WCAG AA (4.5:1 for body text, 3:1 for large text, icons and input borders).
 * `outline` is reserved for boundaries that carry meaning - most importantly
 * text field borders - and therefore clears 3:1 on its own; purely decorative
 * hairlines use `outlineVariant`.
 */
private val LightColorScheme = lightColorScheme(
    primary = RoyalBlue600,
    onPrimary = PureWhite,
    primaryContainer = RoyalBlue50,
    onPrimaryContainer = RoyalBlue800,
    inversePrimary = RoyalBlue400,
    secondary = Slate800,
    onSecondary = PureWhite,
    secondaryContainer = Slate100,
    onSecondaryContainer = Slate900,
    tertiary = Emerald600,
    onTertiary = PureWhite,
    tertiaryContainer = Emerald50,
    onTertiaryContainer = Emerald700,
    background = Slate50,
    onBackground = Slate900,
    surface = PureWhite,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate700,
    surfaceTint = RoyalBlue600,
    inverseSurface = Slate900,
    inverseOnSurface = Slate50,
    surfaceBright = PureWhite,
    surfaceDim = Slate200,
    surfaceContainerLowest = PureWhite,
    surfaceContainerLow = Slate50,
    surfaceContainer = Slate100,
    surfaceContainerHigh = Slate100,
    surfaceContainerHighest = Slate200,
    outline = Slate500,
    outlineVariant = Slate300,
    error = Rose600,
    onError = PureWhite,
    errorContainer = Rose50,
    onErrorContainer = Rose700,
    scrim = Color.Black
)

/**
 * Dark scheme.
 *
 * Brand blue, emerald and rose all move to their 400 tones here: the 500/600
 * tones used on light surfaces sit at roughly 2.8:1 against [Slate800] and are
 * unreadable as text or icons.
 */
private val DarkColorScheme = darkColorScheme(
    primary = RoyalBlue400,
    onPrimary = Slate900,
    primaryContainer = RoyalBlue800,
    onPrimaryContainer = RoyalBlue100,
    inversePrimary = RoyalBlue600,
    secondary = Slate200,
    onSecondary = Slate900,
    secondaryContainer = Slate700,
    onSecondaryContainer = Slate50,
    tertiary = Emerald400,
    onTertiary = Slate900,
    tertiaryContainer = Emerald700,
    onTertiaryContainer = Emerald50,
    background = Slate900,
    onBackground = Slate50,
    surface = Slate800,
    onSurface = Slate50,
    surfaceVariant = Slate700,
    onSurfaceVariant = Slate300,
    surfaceTint = RoyalBlue400,
    inverseSurface = Slate50,
    inverseOnSurface = Slate900,
    surfaceBright = Slate700,
    surfaceDim = Slate900,
    surfaceContainerLowest = Slate900,
    surfaceContainerLow = Slate900,
    surfaceContainer = Slate800,
    surfaceContainerHigh = Slate700,
    surfaceContainerHighest = Slate600,
    outline = Slate500,
    outlineVariant = Slate600,
    error = Rose400,
    onError = Slate900,
    errorContainer = Rose700,
    onErrorContainer = Rose50,
    scrim = Color.Black
)

/** True while the active scheme paints on light surfaces. */
val ColorScheme.isLightScheme: Boolean
    get() = surface.luminance() > 0.5f

/** Fill for "this succeeded / this line is up" affordances such as the call button. */
val ColorScheme.success: Color
    get() = if (isLightScheme) Emerald600 else Emerald400

/** Content colour for [success] fills. */
val ColorScheme.onSuccess: Color
    get() = if (isLightScheme) PureWhite else Slate900

/** Tinted background for success text and badges. */
val ColorScheme.successContainer: Color
    get() = if (isLightScheme) Emerald50 else Emerald700

/** Text/icon colour legible on [successContainer]. */
val ColorScheme.onSuccessContainer: Color
    get() = if (isLightScheme) Emerald700 else Emerald50

/** Fill for cautionary affordances such as the zero balance top-up action. */
val ColorScheme.warning: Color
    get() = if (isLightScheme) Amber700 else Amber500

/** Content colour for [warning] fills. */
val ColorScheme.onWarning: Color
    get() = if (isLightScheme) PureWhite else Slate900

/** Tinted background for warning banners. */
val ColorScheme.warningContainer: Color
    get() = if (isLightScheme) Amber50 else Amber700

/** Text/icon colour legible on [warningContainer]. */
val ColorScheme.onWarningContainer: Color
    get() = if (isLightScheme) Amber700 else Amber50

@Composable
fun DialerIDTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
