package com.finnvek.knittools.ui.theme

import org.junit.Assert.assertTrue
import org.junit.Test

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

    private companion object {
        const val MINIMUM_GRAPHICAL_CONTRAST = 3.0
    }
}
