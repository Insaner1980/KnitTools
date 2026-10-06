package com.finnvek.knittools.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

class ProjectListContrastTest {
    @Test
    fun `progress fill clears the graphical contrast minimum against its track`() {
        assertTrue(contrastRatio(Primary, Background) >= MINIMUM_GRAPHICAL_CONTRAST)
        assertTrue(contrastRatio(Primary, LightBackground) >= MINIMUM_GRAPHICAL_CONTRAST)
    }

    @Test
    fun `readable accent supports small text on background and buttons`() {
        listOf(Background, ActionContainer).forEach { assertTrue(contrastRatio(PrimaryReadable, it) >= 4.5) }
        listOf(LightBackground, LightActionContainer).forEach {
            assertTrue(
                contrastRatio(LightPrimaryReadable, it) >= 4.5,
            )
        }
    }

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
