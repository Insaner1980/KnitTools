package com.finnvek.knittools.ui.screens.counter

import com.finnvek.knittools.ui.components.extraCounterStepperColors
import com.finnvek.knittools.ui.theme.LightSurfaceHigh
import com.finnvek.knittools.ui.theme.LightSurfaceHighest
import com.finnvek.knittools.ui.theme.LightTextSecondary
import com.finnvek.knittools.ui.theme.Primary
import com.finnvek.knittools.ui.theme.SurfaceHigh
import com.finnvek.knittools.ui.theme.TextSecondary
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
                neutralContent = LightTextSecondary,
                surfaceVariant = LightSurfaceHigh,
                surfaceContainerHighest = LightSurfaceHighest,
            )
        val minus =
            extraCounterStepperColors(
                isLightTheme = true,
                isIncrement = false,
                primary = Primary,
                neutralContent = LightTextSecondary,
                surfaceVariant = LightSurfaceHigh,
                surfaceContainerHighest = LightSurfaceHighest,
            )

        assertEquals(LightSurfaceHighest, plus.container)
        assertEquals(Primary, plus.content)
        assertEquals(LightSurfaceHighest, minus.container)
        assertEquals(LightTextSecondary, minus.content)
    }

    @Test
    fun `dark theme steppers use primary plus and neutral minus symbols`() {
        val plus =
            extraCounterStepperColors(
                isLightTheme = false,
                isIncrement = true,
                primary = Primary,
                neutralContent = TextSecondary,
                surfaceVariant = SurfaceHigh,
                surfaceContainerHighest = LightSurfaceHighest,
            )
        val minus =
            extraCounterStepperColors(
                isLightTheme = false,
                isIncrement = false,
                primary = Primary,
                neutralContent = TextSecondary,
                surfaceVariant = SurfaceHigh,
                surfaceContainerHighest = LightSurfaceHighest,
            )

        assertEquals(SurfaceHigh, plus.container)
        assertEquals(Primary, plus.content)
        assertEquals(SurfaceHigh, minus.container)
        assertEquals(TextSecondary, minus.content)
    }
}
