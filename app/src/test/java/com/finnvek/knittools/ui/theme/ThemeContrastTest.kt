package com.finnvek.knittools.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeContrastTest {
    @Test
    fun `hiljennetty teksti täyttää normaalin tekstin kontrastirajan`() {
        assertMinimumContrast(TextMuted, listOf(Background, Surface, SurfaceHigh))
        assertMinimumContrast(
            LightTextMuted,
            listOf(LightBackground, LightSurface, LightSurfaceHigh, LightCardContainer, LightInputField),
        )
    }

    @Test
    fun `secondary labelit täyttävät normaalin tekstin kontrastirajan`() {
        assertMinimumContrast(Secondary, listOf(Background, Surface, SurfaceHigh))
        assertMinimumContrast(
            LightSecondary,
            listOf(LightBackground, LightSurface, LightSurfaceHigh, LightCardContainer),
        )
    }

    @Test
    fun `vaalean teeman syötekenttä on korttia vaaleampi eikä kortti ole raskas khaki`() {
        // Korttia tummempi kenttä luki käytöstä poistettuna; raskas kortti kilpaili petroolin heron kanssa.
        assertTrue(LightInputField.relativeLuminance() > LightCardContainer.relativeLuminance())
        assertTrue(LightCardContainer.relativeLuminance() > LightSurfaceHigh.relativeLuminance())
        assertTrue(LightCardContainer.relativeLuminance() < LightBackground.relativeLuminance())
        assertMinimumContrast(LightTextSecondary, listOf(LightInputField))
    }

    @Test
    fun `tyhjän tilan teksti on luettava mutta erottuu sisällöstä`() {
        assertMinimumContrast(LightEmptyStateText, listOf(LightBackground))
        assertMinimumContrast(TextMuted, listOf(Background))
        assertTrue(LightEmptyStateText.relativeLuminance() > LightTextSecondary.relativeLuminance())
    }

    @Test
    fun `kentän esimerkkiarvo on luettava kentän pohjalla`() {
        assertMinimumContrast(LightEmptyStateText, listOf(LightInputField))
        assertMinimumContrast(TextSecondary, listOf(SurfaceHighest))
    }

    @Test
    fun `light secondary täytteellä on riittävä vastaväri`() {
        assertMinimumContrast(OnPrimary, listOf(LightSecondary))
    }

    @Test
    fun `selected navigation label has normal text contrast`() {
        assertMinimumContrast(PrimaryReadable, listOf(NavBackground))
        assertMinimumContrast(LightPrimaryReadable, listOf(LightNavBackground))
    }

    private fun assertMinimumContrast(
        foreground: Color,
        backgrounds: List<Color>,
    ) {
        backgrounds.forEach { background ->
            val ratio = contrastRatio(foreground, background)
            assertTrue(
                "Kontrastisuhde $ratio alittaa rajan $MINIMUM_NORMAL_TEXT_CONTRAST",
                ratio >= MINIMUM_NORMAL_TEXT_CONTRAST,
            )
        }
    }

    private companion object {
        const val MINIMUM_NORMAL_TEXT_CONTRAST = 4.5
    }
}
