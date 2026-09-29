package com.itantra.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * iTantra Material 3 Theme.
 *
 * Supports Light, Dark, and System Default modes via [ThemeMode].
 * All colors sourced from Color.kt — no raw hex values here.
 */

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

private val LightColorScheme = lightColorScheme(
    primary = iTantraPrimary,
    onPrimary = iTantraWhite,
    primaryContainer = iTantraLightBlue,
    onPrimaryContainer = iTantraWhite,
    secondary = iTantraLightBlue,
    onSecondary = iTantraWhite,
    secondaryContainer = iTantraLightBlue.copy(alpha = 0.12f),
    onSecondaryContainer = iTantraPrimary,
    tertiary = iTantraSuccess,
    onTertiary = iTantraWhite,
    error = iTantraError,
    onError = iTantraWhite,
    errorContainer = iTantraError.copy(alpha = 0.12f),
    onErrorContainer = iTantraError,
    background = iTantraLightBackground,
    onBackground = iTantraTextPrimary,
    surface = iTantraSurface,
    onSurface = iTantraTextPrimary,
    surfaceVariant = iTantraCardBackground,
    onSurfaceVariant = iTantraTextSecondary,
    outline = iTantraDivider,
    outlineVariant = iTantraDivider.copy(alpha = 0.5f)
)

private val DarkColorScheme = darkColorScheme(
    primary = iTantraLightBlue,
    onPrimary = iTantraWhite,
    primaryContainer = iTantraPrimary,
    onPrimaryContainer = iTantraWhite,
    secondary = iTantraLightBlue,
    onSecondary = iTantraWhite,
    secondaryContainer = iTantraLightBlue.copy(alpha = 0.20f),
    onSecondaryContainer = iTantraWhite,
    tertiary = iTantraSuccess,
    onTertiary = iTantraBlack,
    error = iTantraError,
    onError = iTantraWhite,
    errorContainer = iTantraError.copy(alpha = 0.20f),
    onErrorContainer = iTantraError,
    background = iTantraDarkBackground,
    onBackground = iTantraTextPrimaryDark,
    surface = iTantraSurfaceDark,
    onSurface = iTantraTextPrimaryDark,
    surfaceVariant = iTantraCardBackgroundDark,
    onSurfaceVariant = iTantraTextSecondaryDark,
    outline = iTantraDividerDark,
    outlineVariant = iTantraDividerDark.copy(alpha = 0.5f)
)

@Composable
fun ITantraTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    // Update system bar colors to match the theme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val barColor = if (darkTheme) {
                iTantraDarkBackground.toArgb()
            } else {
                iTantraLightBackground.toArgb()
            }
            window.statusBarColor = barColor
            window.navigationBarColor = barColor

            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ITantraTypography,
        content = content
    )
}
