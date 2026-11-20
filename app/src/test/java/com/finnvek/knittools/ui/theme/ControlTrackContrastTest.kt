package com.finnvek.knittools.ui.theme

import org.junit.Assert.assertTrue
import org.junit.Test

class ControlTrackContrastTest {
    @Test
    fun `dialog segmented tracks and labels clear contrast minimums in both themes`() {
        assertTrue(contrastRatio(TextPrimary, DarkSurfaceDialogTrack) >= 4.5)
        assertTrue(contrastRatio(LightTextPrimary, LightSurfaceHighest) >= 4.5)
    }

    @Test
    fun `slider inactive tracks clear contrast minimum against reader palette in both themes`() {
        assertTrue(contrastRatio(DarkSliderInactiveTrack, Background) >= 1.8)
        assertTrue(contrastRatio(LightSliderInactiveTrack, LightBackground) >= 1.8)
        assertTrue(contrastRatio(Primary, Background) > contrastRatio(DarkSliderInactiveTrack, Background))
        assertTrue(contrastRatio(Primary, LightBackground) > contrastRatio(LightSliderInactiveTrack, LightBackground))
    }
}
