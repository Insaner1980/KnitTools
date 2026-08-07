package com.finnvek.knittools.ui.screens.insights

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class InsightsFabricTest {
    @Test
    fun `visible slice drops complete oldest weeks and preserves model indices`() {
        val model = fabricModel(start = LocalDate.of(2026, 7, 13), dayCount = 24, weekCount = 4)

        val slice = visibleFabricSlice(model, maxVisibleWeeks = 2)

        assertEquals(14, slice.originalStartIndex)
        assertEquals(2, slice.weekCount)
        assertEquals(LocalDate.of(2026, 7, 27), slice.days.first().date)
        assertEquals(LocalDate.of(2026, 8, 5), slice.days.last().date)
    }

    @Test
    fun `default selection uses the latest active visible day`() {
        val model =
            fabricModel(
                start = LocalDate.of(2026, 7, 13),
                dayCount = 21,
                weekCount = 3,
                minutesByIndex = mapOf(5 to 100, 9 to 20, 18 to 30),
            )
        val slice = visibleFabricSlice(model, maxVisibleWeeks = 2)

        assertEquals(18, defaultSelectedFabricDayIndex(slice))
    }

    @Test
    fun `hidden selection resolves to the visible default while visible selection is preserved`() {
        val model =
            fabricModel(
                start = LocalDate.of(2026, 7, 13),
                dayCount = 21,
                weekCount = 3,
                minutesByIndex = mapOf(5 to 100, 10 to 20, 18 to 30),
            )
        val slice = visibleFabricSlice(model, maxVisibleWeeks = 2)

        assertEquals(18, resolveFabricSelectedDayIndex(slice, selectedIndex = 5))
        assertEquals(10, resolveFabricSelectedDayIndex(slice, selectedIndex = 10))
    }

    @Test
    fun `day cells follow the locale week start`() {
        val mondaySlice =
            visibleFabricSlice(
                fabricModel(LocalDate.of(2026, 7, 27), dayCount = 8, weekCount = 2),
                maxVisibleWeeks = 2,
            )
        val sundaySlice =
            visibleFabricSlice(
                fabricModel(
                    start = LocalDate.of(2026, 7, 26),
                    dayCount = 9,
                    weekCount = 2,
                    firstDayOfWeek = DayOfWeek.SUNDAY,
                ),
                maxVisibleWeeks = 2,
            )

        assertEquals(
            InsightsFabricCell(weekColumn = 0, weekdayRow = 6),
            fabricCellForDate(LocalDate.of(2026, 8, 2), mondaySlice, DayOfWeek.MONDAY),
        )
        assertEquals(
            InsightsFabricCell(weekColumn = 1, weekdayRow = 1),
            fabricCellForDate(LocalDate.of(2026, 8, 3), sundaySlice, DayOfWeek.SUNDAY),
        )
    }

    @Test
    fun `cell lookup rejects future cells missing from the partial current week`() {
        val slice =
            visibleFabricSlice(
                fabricModel(
                    start = LocalDate.of(2026, 8, 2),
                    dayCount = 9,
                    weekCount = 2,
                    firstDayOfWeek = DayOfWeek.SUNDAY,
                ),
                maxVisibleWeeks = 2,
            )

        assertEquals(8, fabricOriginalIndexAtCell(slice, InsightsFabricCell(weekColumn = 1, weekdayRow = 1)))
        assertNull(fabricOriginalIndexAtCell(slice, InsightsFabricCell(weekColumn = 1, weekdayRow = 2)))
    }

    @Test
    fun `month labels stay on day one columns and thin to the maximum`() {
        val slice =
            visibleFabricSlice(
                fabricModel(LocalDate.of(2025, 12, 29), dayCount = 70, weekCount = 10),
                maxVisibleWeeks = 10,
            )

        assertEquals(
            listOf(3, 62),
            fabricMonthLabelIndices(slice, firstDayOfWeek = DayOfWeek.MONDAY, maxLabels = 2),
        )
    }

    @Test
    fun `accessibility summary counts only displayed days`() {
        val model =
            fabricModel(
                start = LocalDate.of(2026, 7, 13),
                dayCount = 21,
                weekCount = 3,
                minutesByIndex = mapOf(2 to 100, 8 to 20, 20 to 30),
            )
        val slice = visibleFabricSlice(model, maxVisibleWeeks = 2)

        assertEquals(
            InsightsFabricAccessibilitySummary(activeDays = 2, shownDays = 14, totalMinutes = 50),
            fabricAccessibilitySummary(slice),
        )
    }

    private fun fabricModel(
        start: LocalDate,
        dayCount: Int,
        weekCount: Int,
        minutesByIndex: Map<Int, Int> = emptyMap(),
        firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    ): InsightsFabricModel =
        InsightsFabricModel(
            days =
                (0 until dayCount).map { index ->
                    InsightsFabricDay(
                        date = start.plusDays(index.toLong()),
                        totalMinutes = minutesByIndex[index] ?: 0,
                        totalRows = index,
                        dominantProjectId = index.toLong(),
                        level = if (index in minutesByIndex) 3 else 0,
                    )
                },
            firstDayOfWeek = firstDayOfWeek,
            weekCount = weekCount,
        )
}
