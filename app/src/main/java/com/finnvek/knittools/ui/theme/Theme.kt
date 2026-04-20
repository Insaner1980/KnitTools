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

@Immutable
data class KnitToolsExtendedColors(
    val surfaceTint: Color,
    val secondaryOutline: Color,
    val onSurfaceMuted: Color,
    val brandWine: Color,
    val inactiveContent: Color,
    val navBarContainer: Color,
    val navBarIndicator: Color,
)

val LocalKnitToolsColors =
    staticCompositionLocalOf {
        KnitToolsExtendedColors(
            surfaceTint = Color.Unspecified,
            secondaryOutline = Color.Unspecified,
            onSurfaceMuted = Color.Unspecified,
            brandWine = Color.Unspecified,
            inactiveContent = Color.Unspecified,
            navBarContainer = Color.Unspecified,
            navBarIndicator = Color.Unspecified,
        )
    }

val MaterialTheme.knitToolsColors: KnitToolsExtendedColors
    @Composable
    get() = LocalKnitToolsColors.current

// === Dark color scheme ===

private val KnitToolsDarkColorScheme =
    darkColorScheme(
        primary = Primary,
        onPrimary = OnPrimary,
        primaryContainer = PrimaryContainer,
        onPrimaryContainer = TextPrimary,
        secondary = Secondary,
        secondaryContainer = SecondaryContainer,
        tertiary = Tertiary,
        tertiaryContainer = TertiaryContainer,
        surface = Surface,
        surfaceVariant = SurfaceHigh,
        surfaceContainerLowest = Background,
        surfaceContainerLow = Surface,
        surfaceContainer = SurfaceHigh,
        surfaceContainerHigh = SurfaceHighest,
        surfaceContainerHighest = SurfaceHighest,
        onSurface = TextPrimary,
        onSurfaceVariant = TextSecondary,
        background = Background,
        onBackground = TextPrimary,
        error = Error,
        errorContainer = ErrorContainer,
        outline = TextMuted,
        outlineVariant = Divider,
    )

private val DarkExtendedColors =
    KnitToolsExtendedColors(
        surfaceTint = SurfaceHighest,
        secondaryOutline = Divider,
        onSurfaceMuted = TextMuted,
        brandWine = DustyRose,
        inactiveContent = NavText,
        navBarContainer = NavBackground,
        navBarIndicator = NavActiveBg,
    )

// === Light color scheme ===

private val KnitToolsLightColorScheme =
    lightColorScheme(
        primary = Primary,
        onPrimary = OnPrimary,
        primaryContainer = PrimaryContainer,
        onPrimaryContainer = LightTextPrimary,
        secondary = LightSecondary,
        secondaryContainer = LightSecondaryContainer,
        tertiary = LightTertiary,
        tertiaryContainer = LightTertiaryContainer,
        surface = LightSurface,
        surfaceVariant = LightSurfaceHigh,
        surfaceContainerLowest = LightBackground,
        surfaceContainerLow = LightSurface,
        surfaceContainer = LightSurfaceHigh,
        surfaceContainerHigh = LightSurfaceMediumHigh,
        surfaceContainerHighest = LightSurfaceHighest,
        onSurface = LightTextPrimary,
        onSurfaceVariant = LightTextSecondary,
        background = LightBackground,
        onBackground = LightTextPrimary,
        error = Error,
        errorContainer = LightErrorContainer,
        outline = LightTextMuted,
        outlineVariant = LightDivider,
    )

private val LightExtendedColors =
    KnitToolsExtendedColors(
        surfaceTint = LightSurfaceHighest,
        secondaryOutline = LightDivider,
        onSurfaceMuted = LightTextMuted,
        brandWine = LightDustyRose,
        inactiveContent = LightNavText,
        navBarContainer = LightNavBackground,
        navBarIndicator = LightNavActiveBg,
    )

@Composable
fun KnitToolsTheme(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (isDarkTheme) KnitToolsDarkColorScheme else KnitToolsLightColorScheme
    val extendedColors = if (isDarkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalKnitToolsColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
