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
    val onPhotoScrim: Color = TextPrimary,
    val surfaceTint: Color,
    val secondaryOutline: Color,
    val onSurfaceMuted: Color,
    val brandWine: Color,
    val tealAccent: Color,
    val inactiveContent: Color,
    val navBarContainer: Color,
    val navBarIndicator: Color,
    /**
     * --- Käyttötarkoituksen mukaiset pinnat: näkymät käyttävät näitä, eivät colorScheme.surface*-rooleja suoraan ---
     * Kaikkien sisältökorttien, listarivien ja toimintopalkkien pohja (Libraryn kortin väri).
     */
    val cardContainer: Color,
    /** Monivalinnassa valitun kortin pohja. */
    val selectedCardContainer: Color,
    /** Päätoiminnon pohja: jatka-kortti, uusi projekti ja laskurin valikon projektilinkki. */
    val actionContainer: Color,
    /** Pienet versaaliosio-otsikot kaikissa näkymissä. */
    val sectionLabel: Color,
    /** Taulukoiden otsikkorivi, joka erottuu kortin pohjasta. */
    val tableHeaderContainer: Color,
    /** Syötekentän pohja kortissa, dialogissa ja sheetissä (`NumberInputField`, `cardTextFieldColors`). */
    val inputFieldContainer: Color,
    /** Tekstikenttä suoraan näkymän taustalla (hakukentät): vaaleassa teemassa kortin sävy, ei raskas khaki. */
    val screenFieldContainer: Color,
    /** Tyhjän tilan teksti näkymän taustalla, selvästi himmeämpi kuin sisältö. */
    val emptyStateText: Color,
    /** Esimerkkiarvo syötekentässä: luettava kentän pohjalla mutta selvästi oikeaa arvoa haaleampi. */
    val fieldPlaceholderText: Color,
    val primaryReadable: Color,
    val yarnSwatchNeutral: Color,
    val activityCellEmpty: Color,
    val transparentIndicator: Color,
    val activityRamp: List<Color>,
    val yarnPalette: List<Color>,
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
            cardContainer = Color.Unspecified,
            selectedCardContainer = Color.Unspecified,
            actionContainer = Color.Unspecified,
            sectionLabel = Color.Unspecified,
            tableHeaderContainer = Color.Unspecified,
            inputFieldContainer = Color.Unspecified,
            screenFieldContainer = Color.Unspecified,
            emptyStateText = Color.Unspecified,
            fieldPlaceholderText = Color.Unspecified,
            primaryReadable = Color.Unspecified,
            yarnSwatchNeutral = Color.Unspecified,
            activityCellEmpty = Color.Unspecified,
            transparentIndicator = Color.Transparent,
            activityRamp = emptyList(),
            yarnPalette = YarnColors,
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
        cardContainer = SurfaceHigh,
        selectedCardContainer = Primary.copy(alpha = SELECTED_CARD_ALPHA),
        actionContainer = ActionContainer,
        sectionLabel = DustyRose,
        tableHeaderContainer = SurfaceHighest,
        inputFieldContainer = SurfaceHighest,
        screenFieldContainer = SurfaceHighest,
        emptyStateText = TextMuted,
        // TextMuted jäi SurfaceHighest-kentässä 3,9:1:een, joten tummassa teemassa toissijainen teksti.
        fieldPlaceholderText = TextSecondary,
        primaryReadable = PrimaryReadable,
        yarnSwatchNeutral = YarnSwatchNeutral,
        activityCellEmpty = ActivityCellEmpty,
        transparentIndicator = Color.Transparent,
        activityRamp = listOf(SecondaryMuted, Secondary, Tertiary, PrimaryContainer),
        yarnPalette = YarnColors,
    )

// Valitun kortin oranssi sävy taustan päällä; sama kaikissa monivalintalistoissa.
private const val SELECTED_CARD_ALPHA = 0.07f

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
        cardContainer = LightCardContainer,
        selectedCardContainer = Primary.copy(alpha = SELECTED_CARD_ALPHA),
        actionContainer = LightActionContainer,
        sectionLabel = LightSectionLabel,
        // Kortin sävy: tumma khakikaista oli vanhan paletin jäänne taulukoiden yläpuolella.
        tableHeaderContainer = LightCardContainer,
        inputFieldContainer = LightInputField,
        screenFieldContainer = LightCardContainer,
        emptyStateText = LightEmptyStateText,
        fieldPlaceholderText = LightEmptyStateText,
        primaryReadable = LightPrimaryReadable,
        yarnSwatchNeutral = LightYarnSwatchNeutral,
        activityCellEmpty = LightActivityCellEmpty,
        transparentIndicator = Color.Transparent,
        activityRamp = listOf(LightActivityLow, Secondary, Tertiary, Primary),
        yarnPalette = LightYarnColors,
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
