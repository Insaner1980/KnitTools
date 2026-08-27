package com.finnvek.knittools.ui.screens.insights

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class InsightsProjectFabricGeometryTest {
    @Test
    fun `month labels use localized uppercase initials`() {
        val start = LocalDate.of(2026, 2, 22)
        val end = LocalDate.of(2026, 4, 5)
        val model =
            InsightsProjectFabricModel(
                startDate = start,
                endDate = end,
                firstDayOfWeek = DayOfWeek.SUNDAY,
                days =
                    generateSequence(start) { it.plusDays(1) }
                        .takeWhile { !it.isAfter(end) }
                        .map { InsightsProjectFabricDay(it, emptyList()) }
                        .toList(),
            )

        val labels =
            projectFabricMonthLabels(
                model = model,
                formatter = DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH),
                locale = Locale.ENGLISH,
            )

        assertEquals(listOf("F", "M", "A"), labels.map { it.text })
    }

    @Test
    fun `same year fabric range shows the year only once`() {
        val labels =
            projectFabricRangeDateLabels(
                startDate = LocalDate.of(2026, 2, 22),
                endDate = LocalDate.of(2026, 8, 21),
                shortFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH),
                fullFormatter = DateTimeFormatter.ofPattern("MMM d, uuuu", Locale.ENGLISH),
            )

        assertEquals("Feb 22", labels.first)
        assertEquals("Aug 21, 2026", labels.second)
    }

    @Test
    fun `cross year fabric range keeps both years`() {
        val labels =
            projectFabricRangeDateLabels(
                startDate = LocalDate.of(2025, 12, 28),
                endDate = LocalDate.of(2026, 1, 3),
                shortFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH),
                fullFormatter = DateTimeFormatter.ofPattern("MMM d, uuuu", Locale.ENGLISH),
            )

        assertEquals("Dec 28, 2025", labels.first)
        assertEquals("Jan 3, 2026", labels.second)
    }

    @Test
    fun `empty fabric cells use a quieter outline`() {
        val outline = projectFabricEmptyOutlineColor(Color(0xFF664422))

        assertTrue(outline.alpha < 1f)
        assertEquals(0.6f, outline.alpha, TOLERANCE)
    }

    @Test
    fun `project stripes cover the full cell without gaps or overlap`() {
        listOf(1, 2, 3, 5, 20).forEach { count ->
            val stripes = projectFabricStripeBounds(width = 260f, projectCount = count)

            assertEquals(count, stripes.size)
            assertEquals(0f, stripes.first().left, TOLERANCE)
            assertEquals(260f, stripes.last().right, TOLERANCE)
            stripes.zipWithNext().forEach { (first, second) ->
                assertEquals(first.right, second.left, TOLERANCE)
            }
            val widths = stripes.map { it.right - it.left }
            assertTrue(widths.max() - widths.min() <= TOLERANCE)
        }
    }

    @Test
    fun `touch selects the matching calendar day`() {
        val model = model(endDate = START.plusWeeks(25).plusDays(2))

        val date =
            projectFabricDateAt(
                x = 2 * PITCH + 5f,
                y = MONTH_HEIGHT + 3 * PITCH + 5f,
                availableWidth = WIDTH,
                monthLabelHeight = MONTH_HEIGHT,
                gap = GAP,
                model = model,
            )

        assertEquals(START.plusWeeks(2).plusDays(3), date)
    }

    @Test
    fun `month row and cell gaps do not select a day`() {
        val model = model(endDate = START.plusWeeks(25).plusDays(2))

        assertNull(hit(x = 5f, y = MONTH_HEIGHT / 2f, model = model))
        assertNull(hit(x = CELL_SIZE + GAP / 2f, y = MONTH_HEIGHT + 5f, model = model))
        assertNull(hit(x = 5f, y = MONTH_HEIGHT + CELL_SIZE + GAP / 2f, model = model))
    }

    @Test
    fun `future cell in the current week cannot be selected`() {
        val model = model(endDate = START.plusWeeks(25).plusDays(2))

        assertNull(hit(x = 25 * PITCH + 5f, y = MONTH_HEIGHT + 3 * PITCH + 5f, model = model))
    }

    @Test
    fun `locale first day is row zero`() {
        assertEquals(0, projectFabricRowIndex(LocalDate.of(2026, 8, 17), DayOfWeek.MONDAY))
        assertEquals(6, projectFabricRowIndex(LocalDate.of(2026, 8, 23), DayOfWeek.MONDAY))
        assertEquals(0, projectFabricRowIndex(LocalDate.of(2026, 8, 23), DayOfWeek.SUNDAY))
        assertEquals(1, projectFabricRowIndex(LocalDate.of(2026, 8, 17), DayOfWeek.SUNDAY))
    }

    @Test
    fun `accessibility actions cycle only through active days`() {
        val firstActive = START.plusDays(2)
        val secondActive = START.plusDays(9)
        val days =
            listOf(
                InsightsProjectFabricDay(START, emptyList()),
                InsightsProjectFabricDay(firstActive, listOf(1L)),
                InsightsProjectFabricDay(secondActive, listOf(2L)),
            )

        assertEquals(firstActive, moveProjectFabricSelection(days, selectedDate = null, step = STEP_NEXT))
        assertEquals(secondActive, moveProjectFabricSelection(days, selectedDate = null, step = STEP_PREVIOUS))
        assertEquals(secondActive, moveProjectFabricSelection(days, selectedDate = firstActive, step = STEP_NEXT))
        assertEquals(secondActive, moveProjectFabricSelection(days, selectedDate = firstActive, step = STEP_PREVIOUS))
        assertEquals(firstActive, moveProjectFabricSelection(days, selectedDate = secondActive, step = STEP_NEXT))
    }

    private fun hit(
        x: Float,
        y: Float,
        model: InsightsProjectFabricModel,
    ): LocalDate? =
        projectFabricDateAt(
            x = x,
            y = y,
            availableWidth = WIDTH,
            monthLabelHeight = MONTH_HEIGHT,
            gap = GAP,
            model = model,
        )

    private fun model(endDate: LocalDate): InsightsProjectFabricModel =
        InsightsProjectFabricModel(
            startDate = START,
            endDate = endDate,
            firstDayOfWeek = DayOfWeek.MONDAY,
            days =
                generateSequence(START) { it.plusDays(1) }
                    .takeWhile { !it.isAfter(endDate) }
                    .map { InsightsProjectFabricDay(it, emptyList()) }
                    .toList(),
        )

    private companion object {
        val START: LocalDate = LocalDate.of(2026, 2, 23)
        const val WIDTH = 285f
        const val GAP = 1f
        const val CELL_SIZE = 10f
        const val PITCH = CELL_SIZE + GAP
        const val MONTH_HEIGHT = 8f
        const val TOLERANCE = 0.001f
    }
}
