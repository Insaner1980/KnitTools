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

    @Test
    fun `all five modes match independent two by three boundary fixtures`() {
        val linear =
            listOf(ChartCell(0, 0), ChartCell(0, 1), ChartCell(0, 2), ChartCell(1, 0), ChartCell(1, 1), ChartCell(1, 2))
        val diagonal = listOf(linear[0], linear[1], linear[3], linear[2], linear[4], linear[5])
        for (mode in ChartTrackingMode.entries) {
            val base =
                tracker().copy(
                    region =
                        tracker().region.copy(
                            rows = 2,
                            columns = 3,
                            rowDirection = ChartRowDirection.TOP_TO_BOTTOM,
                        ),
                    trackingMode = mode,
                    counterStartValue = 5,
                    gridStartIndex = 1,
                    c2cOrigin = ChartCorner.TOP_LEFT,
                )
            val order = if (mode == ChartTrackingMode.C2C_DIAGONAL) diagonal else linear
            for (wrap in listOf(false, true)) {
                val boundaries =
                    listOf(
                        3 to if (wrap) 5 else 0,
                        4 to 0,
                        5 to 1,
                        9 to 5,
                        10 to if (wrap) 0 else 5,
                    )
                for ((value, index) in boundaries) {
                    val result = resolveChartTrackerHighlight(base.copy(wrapAtEnd = wrap), value)
                    val active = order[index]
                    val expected =
                        when (mode) {
                            ChartTrackingMode.ACTIVE_ROW -> linear.filter { it.row == active.row }.toSet()
                            ChartTrackingMode.ACTIVE_COLUMN -> linear.filter { it.column == active.column }.toSet()
                            ChartTrackingMode.CROSSHAIR ->
                                linear
                                    .filter {
                                        it.row == active.row ||
                                            it.column == active.column
                                    }.toSet()
                            ChartTrackingMode.COMPLETED_CELLS -> order.take(index + 1).toSet()
                            ChartTrackingMode.C2C_DIAGONAL -> setOf(active)
                        }
                    assertEquals("$mode/$wrap/$value", active, result.activeCell)
                    assertEquals("$mode/$wrap/$value", expected, result.cells)
                }
            }
            assertFalse(resolveChartTrackerHighlight(base, null).counterAvailable)
            assertTrue(resolveChartTrackerHighlight(base, null).cells.isEmpty())
            assertEquals(
                order[3],
                resolveChartTrackerHighlight(
                    base.copy(counterStartValue = Int.MAX_VALUE, gridStartIndex = 0, wrapAtEnd = true),
                    Int.MIN_VALUE,
                ).activeCell,
            )
            assertThinRegions(base)
        }
    }

    private fun assertThinRegions(base: ChartTrackerPayload) {
        for ((rows, columns) in listOf(1 to 1, 1 to 3, 3 to 1)) {
            val thin =
                base.copy(
                    region = base.region.copy(rows = rows, columns = columns),
                    counterStartValue = 0,
                    gridStartIndex = 0,
                )
            assertEquals(ChartCell(0, 0), resolveChartTrackerHighlight(thin, 0).activeCell)
            assertEquals(ChartCell(rows - 1, columns - 1), resolveChartTrackerHighlight(thin, 99).activeCell)
        }
    }

    @Test
    fun `directions and all diagonal origins match explicitly ordered cells`() {
        val expectedDirections =
            listOf(
                listOf(0, 1, 2, 3, 4, 5),
                listOf(2, 1, 0, 5, 4, 3),
                listOf(0, 1, 2, 5, 4, 3),
                listOf(3, 4, 5, 0, 1, 2),
                listOf(5, 4, 3, 2, 1, 0),
                listOf(3, 4, 5, 2, 1, 0),
            )
        var fixture = 0
        val columns =
            listOf(
                ChartColumnDirection.LEFT_TO_RIGHT,
                ChartColumnDirection.RIGHT_TO_LEFT,
                ChartColumnDirection.ALTERNATING,
            )
        for (rowDirection in listOf(ChartRowDirection.TOP_TO_BOTTOM, ChartRowDirection.BOTTOM_TO_TOP)) {
            for (columnDirection in columns) {
                for (mode in ChartTrackingMode.entries.filter { it != ChartTrackingMode.C2C_DIAGONAL }) {
                    val value =
                        tracker().copy(
                            region =
                                tracker().region.copy(
                                    rows = 2,
                                    columns = 3,
                                    rowDirection = rowDirection,
                                    columnDirection = columnDirection,
                                ),
                            trackingMode = mode,
                        )
                    val expected = expectedDirections[fixture].map { ChartCell(it / 3, it % 3) }
                    assertEquals(expected, (0..5).map { resolveChartTrackerHighlight(value, it).activeCell })
                }
                fixture++
            }
        }
        val diagonals =
            mapOf(
                ChartCorner.TOP_LEFT to listOf(0, 1, 3, 2, 4, 5),
                ChartCorner.TOP_RIGHT to listOf(2, 1, 5, 0, 4, 3),
                ChartCorner.BOTTOM_LEFT to listOf(3, 4, 0, 5, 1, 2),
                ChartCorner.BOTTOM_RIGHT to listOf(5, 4, 2, 3, 1, 0),
            )
        for ((corner, expected) in diagonals) {
            val value =
                tracker().copy(
                    region = tracker().region.copy(rows = 2, columns = 3),
                    trackingMode = ChartTrackingMode.C2C_DIAGONAL,
                    c2cOrigin = corner,
                )
            assertEquals(
                expected.map {
                    ChartCell(it / 3, it % 3)
                },
                (0..5).map { resolveChartTrackerHighlight(value, it).activeCell },
            )
        }
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
