package com.finnvek.knittools.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class ThemeContrastTest {
    @Test
    fun `hiljennetty teksti täyttää normaalin tekstin kontrastirajan`() {
        assertMinimumContrast(TextMuted, listOf(Background, Surface, SurfaceHigh))
        assertMinimumContrast(LightTextMuted, listOf(LightBackground, LightSurface, LightSurfaceHigh))
    }

    @Test
    fun `secondary labelit täyttävät normaalin tekstin kontrastirajan`() {
        assertMinimumContrast(Secondary, listOf(Background, Surface, SurfaceHigh))
        assertMinimumContrast(LightSecondary, listOf(LightBackground, LightSurface, LightSurfaceHigh))
    }

    @Test
    fun `light secondary täytteellä on riittävä vastaväri`() {
        assertMinimumContrast(OnPrimary, listOf(LightSecondary))
    }

    @Test
    fun `pieni aksenttiteksti täyttää kontrastirajan todellisilla pinnoilla`() {
        assertMinimumContrast(AccentTextPrimary, listOf(Background, Surface))
        assertMinimumContrast(LightAccentTextPrimary, listOf(LightBackground, LightSurface))
    }

    @Test
    fun `hakukentän placeholder täyttää kontrastirajan`() {
        assertMinimumContrast(TextMuted, listOf(SurfaceHigh))
        assertMinimumContrast(LightTextPrimary, listOf(LightSurfaceHigh))
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

    private fun contrastRatio(
        foreground: Color,
        background: Color,
    ): Double {
        val foregroundLuminance = foreground.relativeLuminance()
        val backgroundLuminance = background.relativeLuminance()
        return (max(foregroundLuminance, backgroundLuminance) + LUMINANCE_OFFSET) /
            (min(foregroundLuminance, backgroundLuminance) + LUMINANCE_OFFSET)
    }

    private fun Color.relativeLuminance(): Double =
        RED_LUMINANCE * red.linearized() +
            GREEN_LUMINANCE * green.linearized() +
            BLUE_LUMINANCE * blue.linearized()

    private fun Float.linearized(): Double {
        val component = toDouble()
        return if (component <= LINEAR_COMPONENT_THRESHOLD) {
            component / LINEAR_COMPONENT_DIVISOR
        } else {
            ((component + COMPONENT_OFFSET) / COMPONENT_DIVISOR).pow(COMPONENT_EXPONENT)
        }
    }

    private companion object {
        const val MINIMUM_NORMAL_TEXT_CONTRAST = 4.5
        const val LUMINANCE_OFFSET = 0.05
        const val RED_LUMINANCE = 0.2126
        const val GREEN_LUMINANCE = 0.7152
        const val BLUE_LUMINANCE = 0.0722
        const val LINEAR_COMPONENT_THRESHOLD = 0.04045
        const val LINEAR_COMPONENT_DIVISOR = 12.92
        const val COMPONENT_OFFSET = 0.055
        const val COMPONENT_DIVISOR = 1.055
        const val COMPONENT_EXPONENT = 2.4
    }
}
