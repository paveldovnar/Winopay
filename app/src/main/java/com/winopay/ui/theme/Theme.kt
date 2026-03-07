package com.winopay.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val LocalWinoColors = staticCompositionLocalOf { WinoDarkColors }

private val WinoDarkColorScheme = darkColorScheme(
    primary = WinoDarkColors.brandPrimary,
    onPrimary = WinoDarkColors.textOnBrand,
    primaryContainer = WinoDarkColors.brandSoft,
    onPrimaryContainer = WinoDarkColors.textPrimary,
    secondary = WinoDarkColors.bgSurfaceAlt,
    onSecondary = WinoDarkColors.textPrimary,
    secondaryContainer = WinoDarkColors.bgSurface,
    onSecondaryContainer = WinoDarkColors.textSecondary,
    tertiary = WinoDarkColors.brandStrong,
    onTertiary = WinoDarkColors.textOnBrand,
    background = WinoDarkColors.bgCanvas,
    onBackground = WinoDarkColors.textPrimary,
    surface = WinoDarkColors.bgSurface,
    onSurface = WinoDarkColors.textPrimary,
    surfaceVariant = WinoDarkColors.bgSurfaceAlt,
    onSurfaceVariant = WinoDarkColors.textSecondary,
    error = WinoDarkColors.stateError,
    onError = WinoDarkColors.textOnCritical,
    errorContainer = WinoDarkColors.stateErrorSoft,
    onErrorContainer = WinoDarkColors.stateError,
    outline = WinoDarkColors.borderDefault,
    outlineVariant = WinoDarkColors.borderSubtle,
    scrim = WinoDarkColors.bgCanvas.copy(alpha = 0.7f)
)

private val WinoLightColorScheme = lightColorScheme(
    primary = WinoLightColors.brandPrimary,
    onPrimary = WinoLightColors.textOnBrand,
    primaryContainer = WinoLightColors.brandSoft,
    onPrimaryContainer = WinoLightColors.textPrimary,
    secondary = WinoLightColors.bgSurfaceAlt,
    onSecondary = WinoLightColors.textPrimary,
    secondaryContainer = WinoLightColors.bgSurface,
    onSecondaryContainer = WinoLightColors.textSecondary,
    tertiary = WinoLightColors.brandStrong,
    onTertiary = WinoLightColors.textOnBrand,
    background = WinoLightColors.bgCanvas,
    onBackground = WinoLightColors.textPrimary,
    surface = WinoLightColors.bgSurface,
    onSurface = WinoLightColors.textPrimary,
    surfaceVariant = WinoLightColors.bgSurfaceAlt,
    onSurfaceVariant = WinoLightColors.textSecondary,
    error = WinoLightColors.stateError,
    onError = WinoLightColors.textOnCritical,
    errorContainer = WinoLightColors.stateErrorSoft,
    onErrorContainer = WinoLightColors.stateError,
    outline = WinoLightColors.borderDefault,
    outlineVariant = WinoLightColors.borderSubtle,
    scrim = WinoLightColors.bgCanvas.copy(alpha = 0.7f)
)

@Composable
fun WinoPayTheme(
    themeMode: String = "dark", // "dark", "light", or "system"
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val useDarkTheme = when (themeMode) {
        "light" -> false
        "system" -> isSystemDark
        else -> true // default to dark
    }

    val colorScheme = if (useDarkTheme) WinoDarkColorScheme else WinoLightColorScheme
    val winoColors = if (useDarkTheme) WinoDarkColors else WinoLightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            (view.context as? Activity)?.window?.let { window ->
                window.statusBarColor = Color.Transparent.toArgb()
                window.navigationBarColor = Color.Transparent.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !useDarkTheme
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !useDarkTheme
            }
        }
    }

    CompositionLocalProvider(LocalWinoColors provides winoColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}

object WinoTheme {
    val colors: WinoSemanticColors
        @Composable
        get() = LocalWinoColors.current

    val typography = WinoTypography
    val spacing = WinoSpacing
    val radius = WinoRadius
    val components = WinoComponents
}

// Safe area modifiers for consistent edge-to-edge handling

/**
 * Apply to the root content Column/Box to handle status bar inset.
 * Use this for screens with scrollable content.
 */
fun Modifier.winoTopSafeArea(): Modifier = this.statusBarsPadding()

/**
 * Apply to the bottom button container to handle navigation bar inset.
 * This ensures buttons don't overlap with gesture navigation or nav bar.
 */
fun Modifier.winoBottomSafeArea(): Modifier = this.navigationBarsPadding()

/**
 * Apply to scrollable content that needs bottom padding for navigation bar.
 * Use contentWindowInsets in LazyColumn/LazyRow instead when possible.
 */
fun Modifier.winoContentSafeArea(): Modifier = composed {
    this
        .statusBarsPadding()
        .navigationBarsPadding()
}

/**
 * WindowInsets for use with LazyColumn/LazyRow contentPadding
 */
object WinoInsets {
    val statusBars: WindowInsets
        @Composable
        get() = WindowInsets.statusBars

    val navigationBars: WindowInsets
        @Composable
        get() = WindowInsets.navigationBars

    val systemBars: WindowInsets
        @Composable
        get() = WindowInsets.systemBars
}
