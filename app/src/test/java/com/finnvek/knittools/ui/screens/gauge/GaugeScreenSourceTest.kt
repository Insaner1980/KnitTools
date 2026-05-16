package com.finnvek.knittools.ui.screens.gauge

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertTrue
import org.junit.Test

class GaugeScreenSourceTest {
    @Test
    fun `result card displays row gauge percentage as well as stitch gauge`() {
        val source = ProjectSourceFiles.read(GAUGE_SCREEN)

        assertTrue(source.contains("R.string.stitch_gauge_diff"))
        assertTrue(source.contains("R.string.row_gauge_diff"))
        assertTrue(source.contains("result.rowPercentDifference"))
    }

    private companion object {
        private const val GAUGE_SCREEN = "app/src/main/java/com/finnvek/knittools/ui/screens/gauge/GaugeScreen.kt"
    }
}
