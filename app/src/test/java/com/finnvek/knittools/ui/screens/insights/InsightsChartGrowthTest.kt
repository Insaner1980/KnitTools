package com.finnvek.knittools.ui.screens.insights

import org.junit.Assert.assertEquals
import org.junit.Test

class InsightsChartGrowthTest {
    @Test
    fun `bar growth scales the complete stack from its baseline to its final height`() {
        assertEquals(0f, chartBarGrowthHeight(finalHeight = 96f, progress = 0f), 0f)
        assertEquals(48f, chartBarGrowthHeight(finalHeight = 96f, progress = 0.5f), 0f)
        assertEquals(96f, chartBarGrowthHeight(finalHeight = 96f, progress = 1f), 0f)
    }
}
