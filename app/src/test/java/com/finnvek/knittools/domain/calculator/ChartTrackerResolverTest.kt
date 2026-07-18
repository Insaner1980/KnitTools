package com.finnvek.knittools.domain.calculator

import com.finnvek.knittools.domain.model.ChartColumnDirection
import com.finnvek.knittools.domain.model.ChartCorner
import com.finnvek.knittools.domain.model.ChartCounterType
import com.finnvek.knittools.domain.model.ChartRegionPayload
import com.finnvek.knittools.domain.model.ChartRowDirection
import com.finnvek.knittools.domain.model.ChartTrackerPayload
import com.finnvek.knittools.domain.model.ChartTrackingMode
import com.finnvek.knittools.domain.model.NormalizedPatternBounds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartTrackerResolverTest {
    @Test
    fun `row and column directions map counter value to visual cell`() {
        val tracker = tracker().copy(counterStartValue = 10, gridStartIndex = 0)

        val first = resolveChartTrackerHighlight(tracker, counterValue = 10)
        val seventh = resolveChartTrackerHighlight(tracker, counterValue = 16)

        assertEquals(ChartCell(row = 2, column = 0), first.activeCell)
        assertEquals(ChartCell(row = 1, column = 2), seventh.activeCell)
    }

    @Test
    fun `alternating rows reverse every other logical row`() {
        val tracker =
            tracker().copy(
                region = tracker().region.copy(columnDirection = ChartColumnDirection.ALTERNATING),
            )

        assertEquals(ChartCell(2, 3), resolveChartTrackerHighlight(tracker, 3).activeCell)
        assertEquals(ChartCell(1, 3), resolveChartTrackerHighlight(tracker, 4).activeCell)
        assertEquals(ChartCell(1, 1), resolveChartTrackerHighlight(tracker, 6).activeCell)
    }

    @Test
    fun `counter decreases clamp and wrap are calculated from current value`() {
        val tracker = tracker().copy(counterStartValue = 5)

        assertEquals(ChartCell(2, 0), resolveChartTrackerHighlight(tracker, 2).activeCell)
        assertEquals(ChartCell(0, 3), resolveChartTrackerHighlight(tracker, 99).activeCell)
        assertEquals(
            ChartCell(0, 2),
            resolveChartTrackerHighlight(tracker.copy(wrapAtEnd = true), 3).activeCell,
        )
    }

    @Test
    fun `C2C traverses diagonals from configured corner`() {
        val tracker =
            tracker().copy(
                trackingMode = ChartTrackingMode.C2C_DIAGONAL,
                c2cOrigin = ChartCorner.TOP_RIGHT,
            )

        assertEquals(ChartCell(0, 3), resolveChartTrackerHighlight(tracker, 0).activeCell)
        assertEquals(ChartCell(0, 2), resolveChartTrackerHighlight(tracker, 1).activeCell)
        assertEquals(ChartCell(1, 3), resolveChartTrackerHighlight(tracker, 2).activeCell)
    }

    @Test
    fun `missing extra counter yields explicit unavailable state`() {
        val result = resolveChartTrackerHighlight(tracker(), counterValue = null)

        assertFalse(result.counterAvailable)
        assertTrue(result.cells.isEmpty())
        assertEquals(null, result.activeCell)
    }

    private fun tracker() =
        ChartTrackerPayload(
            region =
                ChartRegionPayload(
                    bounds = NormalizedPatternBounds(0.1f, 0.1f, 0.9f, 0.9f),
                    name = "Chart",
                    rows = 3,
                    columns = 4,
                    rowDirection = ChartRowDirection.BOTTOM_TO_TOP,
                    columnDirection = ChartColumnDirection.LEFT_TO_RIGHT,
                ),
            trackingMode = ChartTrackingMode.ACTIVE_ROW,
            counterType = ChartCounterType.MAIN,
            counterStartValue = 0,
            gridStartIndex = 0,
            wrapAtEnd = false,
            highlightArgb = 0x60FFD54F,
            highlightAlpha = 0.35f,
        )
}
