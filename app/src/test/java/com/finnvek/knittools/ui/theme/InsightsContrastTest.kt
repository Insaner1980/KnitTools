package com.finnvek.knittools.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/**
 * Kaavion apuviivat ovat ei-tekstigrafiikkaa, jota tarvitaan sisällön
 * ymmärtämiseen (asteikko), joten niitä koskee WCAG 2.2:n 3:1 vaatimus.
 */
class InsightsContrastTest {
    @Test
    fun `chart gridlines clear the non-text contrast minimum in both themes`() {
        val dark = contrastRatio(TextPrimary, Background, InsightsDimens.ChartGridlineAlpha)
        val light = contrastRatio(LightTextPrimary, LightBackground, InsightsDimens.ChartGridlineAlpha)

        assertTrue("Dark gridline contrast was $dark", dark >= 3.0)
        assertTrue("Light gridline contrast was $light", light >= 3.0)
    }

    @Test
    fun `muted body text clears the text contrast minimum in both themes`() {
        val dark = contrastRatio(TextMuted, Background, alpha = 1f)
        val light = contrastRatio(LightTextMuted, LightBackground, alpha = 1f)

        assertTrue("Dark muted text contrast was $dark", dark >= 4.5)
        assertTrue("Light muted text contrast was $light", light >= 4.5)
    }

    private fun contrastRatio(
        foreground: Color,
        background: Color,
        alpha: Float,
    ): Double {
        val blended =
            Triple(
                blend(foreground.red, background.red, alpha),
                blend(foreground.green, background.green, alpha),
                blend(foreground.blue, background.blue, alpha),
            )
        val foregroundLuminance = relativeLuminance(blended.first, blended.second, blended.third)
        val backgroundLuminance = relativeLuminance(background.red, background.green, background.blue)
        val lighter = maxOf(foregroundLuminance, backgroundLuminance)
        val darker = minOf(foregroundLuminance, backgroundLuminance)
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun blend(
        foreground: Float,
        background: Float,
        alpha: Float,
    ): Float = foreground * alpha + background * (1f - alpha)

    private fun relativeLuminance(
        red: Float,
        green: Float,
        blue: Float,
    ): Double = 0.2126 * linear(red) + 0.7152 * linear(green) + 0.0722 * linear(blue)

    private fun linear(channel: Float): Double {
        val value = channel.toDouble()
        return if (value <= 0.03928) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
    }
}
