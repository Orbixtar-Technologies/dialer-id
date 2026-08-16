package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Light scheme — warm paper, ink type, a single teal signal accent.
 *
 * Every `on*` role is picked against the container it is drawn on, targeting
 * WCAG AA (4.5:1 for body text, 3:1 for large text, icons and input borders).
 * `outline` is reserved for boundaries that carry meaning — most importantly
 * text field borders — and therefore clears 3:1 on its own; purely decorative
 * hairlines use `outlineVariant`.
 */
private val LightColorScheme = lightColorScheme(
    primary = SignalTeal600,
    onPrimary = PureWhite,
    primaryContainer = SignalTeal50,
    onPrimaryContainer = SignalTeal800,
    inversePrimary = SignalTeal400,
    secondary = Ink600,
    onSecondary = Paper,
    secondaryContainer = Linen100,
    onSecondaryContainer = Ink900,
    tertiary = SignalGreen600,
    onTertiary = PureWhite,
    tertiaryContainer = SignalGreen50,
    onTertiaryContainer = SignalGreen700,
    background = Linen50,
    onBackground = Ink900,
    surface = Paper,
    onSurface = Ink900,
    surfaceVariant = Linen100,
    onSurfaceVariant = Ink600,
    surfaceTint = SignalTeal600,
    inverseSurface = Ink900,
    inverseOnSurface = Linen50,
    surfaceBright = Paper,
    surfaceDim = Linen200,
    surfaceContainerLowest = PureWhite,
    surfaceContainerLow = Paper,
    surfaceContainer = Linen100,
    surfaceContainerHigh = Linen200,
    surfaceContainerHighest = Linen300,
    outline = Ink500,
    outlineVariant = Linen300,
    error = SignalRose600,
    onError = PureWhite,
    errorContainer = SignalRose50,
    onErrorContainer = SignalRose700,
    scrim = Color.Black
)

/**
 * Dark scheme — cinematic ink with layered lift, luminous teal that still
 * clears AA on the deep base. Never a flat #000 field.
 */
private val DarkColorScheme = darkColorScheme(
    primary = SignalTeal400,
    onPrimary = SignalTeal900,
    primaryContainer = SignalTeal800,
    onPrimaryContainer = SignalTeal100,
    inversePrimary = SignalTeal600,
    secondary = Ink200,
    onSecondary = Ink900,
    secondaryContainer = Ink800,
    onSecondaryContainer = Ink50,
    tertiary = SignalGreen400,
    onTertiary = SignalGreen900,
    tertiaryContainer = SignalGreen700,
    onTertiaryContainer = SignalGreen50,
    background = Ink950,
    onBackground = Ink50,
    surface = Ink850,
    onSurface = Ink50,
    surfaceVariant = Ink800,
    onSurfaceVariant = Ink200,
    surfaceTint = SignalTeal400,
    inverseSurface = Linen50,
    inverseOnSurface = Ink900,
    surfaceBright = Ink700,
    surfaceDim = Ink950,
    surfaceContainerLowest = Ink950,
    surfaceContainerLow = Ink900,
    surfaceContainer = Ink850,
    surfaceContainerHigh = Ink800,
    surfaceContainerHighest = Ink700,
    outline = Ink400,
    outlineVariant = Ink700,
    error = SignalRose400,
    onError = SignalRose900,
    errorContainer = SignalRose700,
    onErrorContainer = SignalRose50,
    scrim = Color.Black
)

/** True while the active scheme paints on light surfaces. */
val ColorScheme.isLightScheme: Boolean
    get() = surface.luminance() > 0.5f

/** Fill for "this succeeded / this line is up" affordances such as the call button. */
val ColorScheme.success: Color
    get() = if (isLightScheme) SignalGreen600 else SignalGreen400

/** Content colour for [success] fills. */
val ColorScheme.onSuccess: Color
    get() = if (isLightScheme) PureWhite else SignalGreen900

/** Tinted background for success text and badges. */
val ColorScheme.successContainer: Color
    get() = if (isLightScheme) SignalGreen50 else SignalGreen700

/** Text/icon colour legible on [successContainer]. */
val ColorScheme.onSuccessContainer: Color
    get() = if (isLightScheme) SignalGreen700 else SignalGreen50

/** Fill for cautionary affordances such as the zero balance top-up action. */
val ColorScheme.warning: Color
    get() = if (isLightScheme) SignalAmber600 else SignalAmber400

/** Content colour for [warning] fills. */
val ColorScheme.onWarning: Color
    get() = if (isLightScheme) PureWhite else SignalAmber900

/** Tinted background for warning banners. */
val ColorScheme.warningContainer: Color
    get() = if (isLightScheme) SignalAmber50 else SignalAmber700

/** Text/icon colour legible on [warningContainer]. */
val ColorScheme.onWarningContainer: Color
    get() = if (isLightScheme) SignalAmber700 else SignalAmber50

/** Outgoing / placed-call accent. */
val ColorScheme.outgoing: Color
    get() = primary

/** Incoming / connected-call accent. */
val ColorScheme.incoming: Color
    get() = success

/** Missed / failed-call accent. */
val ColorScheme.missed: Color
    get() = error

/** Soft luminous halo used behind hero actions. */
val ColorScheme.glow: Color
    get() = if (isLightScheme) SignalTeal600.copy(alpha = 0.18f) else SignalTeal400.copy(alpha = 0.28f)

/** Hairline used on glass-like cards. */
val ColorScheme.glassBorder: Color
    get() = if (isLightScheme) {
        Ink400.copy(alpha = 0.28f)
    } else {
        SignalTeal200.copy(alpha = 0.12f)
    }

@Composable
fun DialerIDTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = DialerShapes,
        content = content
    )
}
