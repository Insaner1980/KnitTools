package com.finnvek.knittools.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

class ProjectListContrastTest {
    @Test
    fun `progress fill clears the graphical contrast minimum against its track`() {
        val alpha = ProjectListDimens.ProgressTrackAlpha
        val dark = progressContrast(TextPrimary, Background, alpha)
        val light = progressContrast(LightTextPrimary, LightBackground, alpha)

        assertTrue("Dark progress contrast was $dark", dark >= MINIMUM_GRAPHICAL_CONTRAST)
        assertTrue("Light progress contrast was $light", light >= MINIMUM_GRAPHICAL_CONTRAST)
    }

    private fun progressContrast(
        trackForeground: Color,
        background: Color,
        trackAlpha: Float,
    ): Double {
        val track =
            Color(
                red = blend(trackForeground.red, background.red, trackAlpha),
                green = blend(trackForeground.green, background.green, trackAlpha),
                blue = blend(trackForeground.blue, background.blue, trackAlpha),
            )
        return contrastRatio(Primary, track)
    }

    private fun blend(
        foreground: Float,
        background: Float,
        alpha: Float,
    ): Float = foreground * alpha + background * (1f - alpha)

    private fun contrastRatio(
        first: Color,
        second: Color,
    ): Double {
        val firstLuminance = relativeLuminance(first)
        val secondLuminance = relativeLuminance(second)
        val lighter = maxOf(firstLuminance, secondLuminance)
        val darker = minOf(firstLuminance, secondLuminance)
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun relativeLuminance(color: Color): Double =
        0.2126 * linear(color.red) + 0.7152 * linear(color.green) + 0.0722 * linear(color.blue)

    private fun linear(channel: Float): Double {
        val value = channel.toDouble()
        return if (value <= 0.03928) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
    }

    private companion object {
        const val MINIMUM_GRAPHICAL_CONTRAST = 3.0
    }
}
