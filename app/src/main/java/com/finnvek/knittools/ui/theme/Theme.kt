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
    val tealAccent: Color,
    val inactiveContent: Color,
    val navBarContainer: Color,
    val navBarIndicator: Color,
    val primaryTintContainer: Color,
    val activityCellEmpty: Color,
    val activityRamp: List<Color>,
)

val LocalKnitToolsColors =
    staticCompositionLocalOf {
        KnitToolsExtendedColors(
            surfaceTint = Color.Unspecified,
            secondaryOutline = Color.Unspecified,
            onSurfaceMuted = Color.Unspecified,
            brandWine = Color.Unspecified,
            tealAccent = Color.Unspecified,
            inactiveContent = Color.Unspecified,
            navBarContainer = Color.Unspecified,
            navBarIndicator = Color.Unspecified,
            primaryTintContainer = Color.Unspecified,
            activityCellEmpty = Color.Unspecified,
            activityRamp = emptyList(),
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
        onPrimaryContainer = OnAccent,
        secondary = Secondary,
        onSecondary = OnAccent,
        secondaryContainer = SecondaryContainer,
        onSecondaryContainer = TextPrimary,
        tertiary = Tertiary,
        onTertiary = OnAccent,
        tertiaryContainer = TertiaryContainer,
        onTertiaryContainer = TextPrimary,
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
        onError = OnPrimary,
        errorContainer = ErrorContainer,
        onErrorContainer = TextPrimary,
        outline = TextMuted,
        outlineVariant = Divider,
    )

private val DarkExtendedColors =
    KnitToolsExtendedColors(
        surfaceTint = SurfaceHighest,
        secondaryOutline = Divider,
        onSurfaceMuted = TextMuted,
        brandWine = DustyRose,
        tealAccent = RavelryTeal,
        inactiveContent = NavText,
        navBarContainer = NavBackground,
        navBarIndicator = NavActiveBg,
        primaryTintContainer = PrimaryTintContainer,
        activityCellEmpty = ActivityCellEmpty,
        activityRamp = listOf(SecondaryMuted, Secondary, Tertiary, PrimaryContainer),
    )

// === Light color scheme ===

private val KnitToolsLightColorScheme =
    lightColorScheme(
        primary = Primary,
        onPrimary = OnPrimary,
        primaryContainer = PrimaryContainer,
        onPrimaryContainer = OnAccent,
        secondary = LightSecondary,
        onSecondary = OnPrimary,
        secondaryContainer = LightSecondaryContainer,
        onSecondaryContainer = LightTextPrimary,
        tertiary = LightTertiary,
        onTertiary = OnAccent,
        tertiaryContainer = LightTertiaryContainer,
        onTertiaryContainer = LightTextPrimary,
        surface = LightSurface,
        surfaceVariant = LightSurfaceHigh,
        surfaceContainerLowest = LightBackground,
        surfaceContainerLow = LightSurface,
        surfaceContainer = LightSurfaceMediumHigh,
        surfaceContainerHigh = LightSurfaceHigh,
        surfaceContainerHighest = LightSurfaceHighest,
        onSurface = LightTextPrimary,
        onSurfaceVariant = LightTextSecondary,
        background = LightBackground,
        onBackground = LightTextPrimary,
        error = Error,
        onError = OnPrimary,
        errorContainer = LightErrorContainer,
        onErrorContainer = LightTextPrimary,
        outline = LightTextMuted,
        outlineVariant = LightDivider,
    )

private val LightExtendedColors =
    KnitToolsExtendedColors(
        surfaceTint = LightSurfaceHighest,
        secondaryOutline = LightDivider,
        onSurfaceMuted = LightTextMuted,
        brandWine = LightDustyRose,
        tealAccent = LightRavelryTeal,
        inactiveContent = LightNavText,
        navBarContainer = LightNavBackground,
        navBarIndicator = LightNavActiveBg,
        primaryTintContainer = LightPrimaryTintContainer,
        activityCellEmpty = LightActivityCellEmpty,
        activityRamp = listOf(LightActivityLow, Secondary, Tertiary, Primary),
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
