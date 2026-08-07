package com.finnvek.knittools.ui.screens.insights

import org.junit.Assert.assertEquals
import org.junit.Test

class InsightsChartDimmingTest {
    @Test
    fun `selection keeps its bar opaque while other bars dim as the transition advances`() {
        assertEquals(
            1f,
            chartBarAlpha(barIndex = 1, selectedIndex = 1, transitionProgress = 1f),
            0f,
        )
        assertEquals(
            0.45f,
            chartBarAlpha(barIndex = 0, selectedIndex = 1, transitionProgress = 1f),
            0f,
        )
        assertEquals(
            0.725f,
            chartBarAlpha(barIndex = 0, selectedIndex = 1, transitionProgress = 0.5f),
            0f,
        )
        assertEquals(
            1f,
            chartBarAlpha(barIndex = 0, selectedIndex = null, transitionProgress = 1f),
            0f,
        )
    }
}
