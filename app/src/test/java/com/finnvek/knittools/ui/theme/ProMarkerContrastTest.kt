package com.finnvek.knittools.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

class ProMarkerContrastTest {
    @Test
    fun `marker text clears small text contrast on filled and transparent light backgrounds`() {
        assertContrast(LightProMarkerContent, LightProMarkerContainer)
        assertContrast(LightProMarkerContent, LightCardContainer)
    }

    @Test
    fun `marker text clears small text contrast on filled and transparent dark backgrounds`() {
        assertContrast(ProMarkerContent, ProMarkerContainer)
        assertContrast(ProMarkerContent, SurfaceHigh)
    }

    private fun assertContrast(
        foreground: Color,
        background: Color,
    ) {
        val first = luminance(foreground)
        val second = luminance(background)
        val ratio = (maxOf(first, second) + LUMINANCE_OFFSET) / (minOf(first, second) + LUMINANCE_OFFSET)
        assertTrue("Pro-merkin tekstikontrasti on vain $ratio:1", ratio >= MINIMUM_TEXT_CONTRAST)
    }

    private fun luminance(color: Color): Double {
        fun channel(value: Float): Double {
            val channel = value.toDouble()
            return if (channel <=
                SRGB_THRESHOLD
            ) {
                channel / SRGB_DIVISOR
            } else {
                ((channel + SRGB_OFFSET) / SRGB_SCALE).pow(SRGB_POWER)
            }
        }
        return RED_WEIGHT * channel(color.red) + GREEN_WEIGHT * channel(color.green) + BLUE_WEIGHT * channel(color.blue)
    }

    private companion object {
        const val MINIMUM_TEXT_CONTRAST = 4.5
        const val LUMINANCE_OFFSET = 0.05
        const val SRGB_THRESHOLD = 0.04045
        const val SRGB_DIVISOR = 12.92
        const val SRGB_OFFSET = 0.055
        const val SRGB_SCALE = 1.055
        const val SRGB_POWER = 2.4
        const val RED_WEIGHT = 0.2126
        const val GREEN_WEIGHT = 0.7152
        const val BLUE_WEIGHT = 0.0722
    }
}
