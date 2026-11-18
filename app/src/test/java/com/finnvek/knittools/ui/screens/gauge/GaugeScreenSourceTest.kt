package com.finnvek.knittools.ui.screens.gauge

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertTrue
import org.junit.Test

class GaugeScreenSourceTest {
    @Test
    fun `shared presentation includes both independent axis percentages and rounded sizes`() {
        val source = ProjectSourceFiles.read(GAUGE_PRESENTATION)

        assertTrue(source.contains("GaugeAxis.entries.mapNotNull(::adjusted)"))
        assertTrue(source.contains("state.stitchAdjustment else state.rowAdjustment"))
        assertTrue(source.contains("result.differencePercent"))
        assertTrue(source.contains("result.roundedLengthMm"))
        assertTrue(source.contains("R.string.measurement_gauge_difference"))
    }

    private companion object {
        private const val GAUGE_PRESENTATION =
            "app/src/main/java/com/finnvek/knittools/ui/screens/gauge/GaugePresentation.kt"
    }
}
