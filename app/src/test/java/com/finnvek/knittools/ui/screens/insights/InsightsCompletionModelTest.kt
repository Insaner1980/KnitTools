package com.finnvek.knittools.ui.screens.insights

import com.finnvek.knittools.domain.model.ProjectCompletion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class InsightsCompletionModelTest {
    private val today = LocalDate.of(2026, 9, 13)
    private val zone = ZoneId.of("Europe/Helsinki")

    @Test
    fun weekAndMonthFilterHistoricalDatesAndPreserveRepeatedEvents() {
        val events =
            listOf(event(1, "2026-09-01"), event(2, "2026-09-07"), event(3, "2026-09-07"), event(4, "2026-08-31"))
        assertEquals(2, aggregate(events, TimeRange.THIS_WEEK).events.size)
        assertEquals(3, aggregate(events, TimeRange.THIS_MONTH).events.size)
        assertEquals(4, aggregate(events, TimeRange.ALL_TIME).events.size)
        val week = aggregate(events, TimeRange.THIS_WEEK)
        assertEquals(listOf(2, 0, 0, 0, 0, 0, 0), week.buckets.map { it.count })
    }

    @Test
    fun emptyRangeHasZeroBucketsAndNoEvents() {
        val result = aggregate(listOf(event(1, "2026-08-01")), TimeRange.THIS_WEEK)
        assertTrue(result.events.isEmpty())
        assertEquals(7, result.buckets.size)
        assertTrue(result.buckets.all { it.count == 0 })
    }

    @Test
    fun allTimeKeepsOlderHistoryAndGapsBeyondSessionChartWindow() {
        val result = aggregate(listOf(event(1, "2024-01-01"), event(2, "2026-09-13")), TimeRange.ALL_TIME)
        assertEquals(33, result.buckets.size)
        assertEquals(2, result.buckets.sumOf { it.count })
        assertEquals(31, result.buckets.count { it.count == 0 })
    }

    @Test
    fun selectedProjectRetainsEveryCompletionCycle() {
        val result =
            aggregate(
                listOf(event(1, "2026-09-01"), event(2, "2026-09-02"), event(3, "2026-09-03").copy(projectId = 2)),
                TimeRange.ALL_TIME,
                1,
            )
        assertEquals(listOf(2L, 1L), result.events.map { it.id })
    }

    @Test
    fun storedZonePreservesDateAndLegacyUsesCapturedFallback() {
        val timestamp = Instant.parse("2026-09-06T22:30:00Z").toEpochMilli()
        val events =
            listOf(ProjectCompletion(1, 1, timestamp, "Europe/Helsinki"), ProjectCompletion(2, 1, timestamp, null))
        val result =
            buildInsightsCompletions(
                events,
                InsightsQueryParams(
                    timeRange = TimeRange.THIS_WEEK,
                    currentDate = today,
                    zone = ZoneId.of("America/New_York"),
                ),
                DayOfWeek.MONDAY,
            )
        assertEquals(listOf(1L), result.events.map { it.id })
        assertEquals(LocalDate.of(2026, 9, 7), result.events.single().date)
    }

    private fun event(
        id: Long,
        date: String,
    ) = ProjectCompletion(
        id,
        1,
        LocalDate
            .parse(date)
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli(),
        zone.id,
    )

    private fun aggregate(
        events: List<ProjectCompletion>,
        range: TimeRange,
        projectId: Long? = null,
    ) = buildInsightsCompletions(
        events,
        InsightsQueryParams(projectId = projectId, timeRange = range, currentDate = today, zone = zone),
        DayOfWeek.MONDAY,
    )
}
