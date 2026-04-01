package com.finnvek.knittools.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.finnvek.knittools.data.datastore.ThemeMode

private val KnitToolsDarkColorScheme =
    darkColorScheme(
        primary = DarkAccent,
        onPrimary = DarkPrimaryText,
        secondary = DarkAccentVariant,
        background = DarkBackground,
        surface = DarkSurface,
        onBackground = DarkPrimaryText,
        onSurface = DarkPrimaryText,
        onSurfaceVariant = DarkSecondaryText,
        error = ErrorColor,
    )

private val KnitToolsLightColorScheme =
    lightColorScheme(
        primary = LightAccent,
        onPrimary = LightSurface,
        secondary = LightAccentVariant,
        background = LightBackground,
        surface = LightSurface,
        onBackground = LightPrimaryText,
        onSurface = LightPrimaryText,
        onSurfaceVariant = LightSecondaryText,
        error = ErrorColor,
    )

@Composable
fun KnitToolsTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val isDark =
        when (themeMode) {
            ThemeMode.DARK -> true
            ThemeMode.LIGHT -> false
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
        }

    MaterialTheme(
        colorScheme = if (isDark) KnitToolsDarkColorScheme else KnitToolsLightColorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
