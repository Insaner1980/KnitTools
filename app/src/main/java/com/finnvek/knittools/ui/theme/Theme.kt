package com.finnvek.knittools.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.finnvek.knittools.data.datastore.ThemeMode

@Immutable
data class KnitToolsExtendedColors(
    val surfaceTint: Color,
    val secondaryOutline: Color,
    val onSurfaceMuted: Color,
    val brandWine: Color,
    val inactiveContent: Color,
)

val LocalKnitToolsColors =
    staticCompositionLocalOf {
        KnitToolsExtendedColors(
            surfaceTint = Color.Unspecified,
            secondaryOutline = Color.Unspecified,
            onSurfaceMuted = Color.Unspecified,
            brandWine = Color.Unspecified,
            inactiveContent = Color.Unspecified,
        )
    }

val MaterialTheme.knitToolsColors: KnitToolsExtendedColors
    @Composable
    get() = LocalKnitToolsColors.current

private val KnitToolsDarkColorScheme =
    darkColorScheme(
        primary = DarkPrimary,
        onPrimary = DarkOnPrimary,
        primaryContainer = DarkPrimaryContainer,
        onPrimaryContainer = DarkPrimary,
        secondary = DarkSecondary,
        secondaryContainer = DarkSecondaryContainer,
        surface = DarkSurface,
        surfaceVariant = DarkSurfaceVariant,
        surfaceContainerLowest = DarkSurfaceContainerLowest,
        surfaceContainerLow = DarkSurfaceContainerLow,
        surfaceContainer = DarkSurfaceContainer,
        surfaceContainerHigh = DarkSurfaceContainerHigh,
        surfaceContainerHighest = DarkSurfaceContainerHighest,
        onSurface = DarkOnSurface,
        onSurfaceVariant = DarkOnSurfaceVariant,
        background = DarkBackground,
        onBackground = DarkOnSurface,
        error = DarkError,
        errorContainer = DarkErrorContainer,
        outline = DarkOutline,
        outlineVariant = DarkOutlineVariant,
    )

private val KnitToolsLightColorScheme =
    lightColorScheme(
        primary = LightPrimary,
        onPrimary = LightOnPrimary,
        primaryContainer = LightPrimaryContainer,
        onPrimaryContainer = LightOnPrimaryContainer,
        secondary = LightSecondary,
        secondaryContainer = LightSecondaryContainer,
        surface = LightSurface,
        surfaceVariant = LightSurfaceVariant,
        surfaceContainerLowest = LightSurfaceContainerLowest,
        surfaceContainerLow = LightSurfaceContainerLow,
        surfaceContainer = LightSurfaceContainer,
        surfaceContainerHigh = LightSurfaceContainerHigh,
        surfaceContainerHighest = LightSurfaceContainerHighest,
        onSurface = LightOnSurface,
        onSurfaceVariant = LightOnSurfaceVariant,
        background = LightBackground,
        onBackground = LightOnSurface,
        error = LightError,
        errorContainer = LightErrorContainer,
        outline = LightOutline,
        outlineVariant = LightOutlineVariant,
    )

private val DarkExtendedColors =
    KnitToolsExtendedColors(
        surfaceTint = DarkSurfaceTint,
        secondaryOutline = DarkSecondaryContainer,
        onSurfaceMuted = DarkOnSurfaceMuted,
        brandWine = BrandWine,
        inactiveContent = InactiveContent,
    )

private val LightExtendedColors =
    KnitToolsExtendedColors(
        surfaceTint = LightSurfaceTint,
        secondaryOutline = LightSecondaryOutline,
        onSurfaceMuted = LightOnSurfaceMuted,
        brandWine = BrandWine,
        inactiveContent = InactiveContent,
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

    val extendedColors = if (isDark) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalKnitToolsColors provides extendedColors) {
        MaterialTheme(
            colorScheme = if (isDark) KnitToolsDarkColorScheme else KnitToolsLightColorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
