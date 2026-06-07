package com.finnvek.knittools.ui.screens.counter

import com.finnvek.knittools.ui.components.extraCounterStepperColors
import com.finnvek.knittools.ui.theme.LightSurfaceHigh
import com.finnvek.knittools.ui.theme.LightSurfaceHighest
import com.finnvek.knittools.ui.theme.LightTextPrimary
import com.finnvek.knittools.ui.theme.Primary
import com.finnvek.knittools.ui.theme.SurfaceHigh
import com.finnvek.knittools.ui.theme.TextPrimary
import org.junit.Assert.assertEquals
import org.junit.Test

class CounterStepperButtonColorsTest {
    @Test
    fun `light theme steppers use highest surface circles and action-specific symbols`() {
        val plus =
            extraCounterStepperColors(
                isLightTheme = true,
                isIncrement = true,
                primary = Primary,
                onSurface = LightTextPrimary,
                surfaceVariant = LightSurfaceHigh,
                surfaceContainerHighest = LightSurfaceHighest,
            )
        val minus =
            extraCounterStepperColors(
                isLightTheme = true,
                isIncrement = false,
                primary = Primary,
                onSurface = LightTextPrimary,
                surfaceVariant = LightSurfaceHigh,
                surfaceContainerHighest = LightSurfaceHighest,
            )

        assertEquals(LightSurfaceHighest, plus.container)
        assertEquals(Primary, plus.content)
        assertEquals(LightSurfaceHighest, minus.container)
        assertEquals(LightTextPrimary, minus.content)
    }

    @Test
    fun `dark theme steppers keep the existing colors`() {
        val plus =
            extraCounterStepperColors(
                isLightTheme = false,
                isIncrement = true,
                primary = Primary,
                onSurface = TextPrimary,
                surfaceVariant = SurfaceHigh,
                surfaceContainerHighest = LightSurfaceHighest,
            )
        val minus =
            extraCounterStepperColors(
                isLightTheme = false,
                isIncrement = false,
                primary = Primary,
                onSurface = TextPrimary,
                surfaceVariant = SurfaceHigh,
                surfaceContainerHighest = LightSurfaceHighest,
            )

        assertEquals(SurfaceHigh, plus.container)
        assertEquals(TextPrimary, plus.content)
        assertEquals(SurfaceHigh, minus.container)
        assertEquals(TextPrimary, minus.content)
    }
}
