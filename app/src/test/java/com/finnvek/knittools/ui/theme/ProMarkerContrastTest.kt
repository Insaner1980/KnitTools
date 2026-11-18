package com.finnvek.knittools.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test

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
        val ratio = contrastRatio(foreground, background)
        assertTrue("Pro-merkin tekstikontrasti on vain $ratio:1", ratio >= MINIMUM_TEXT_CONTRAST)
    }

    private companion object {
        const val MINIMUM_TEXT_CONTRAST = 4.5
    }
}
