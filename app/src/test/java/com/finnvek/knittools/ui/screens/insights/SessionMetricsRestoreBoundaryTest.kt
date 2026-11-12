package com.finnvek.knittools.ui.screens.insights

import com.finnvek.knittools.domain.model.KnitSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneOffset

class SessionMetricsRestoreBoundaryTest {
    private val zone = ZoneOffset.UTC
    private val session =
        KnitSession(
            projectId = 1L,
            startedAt = 0L,
            endedAt = 1_000L,
            startRow = 0,
            endRow = 1,
            durationMinutes = 1,
            durationSeconds = Long.MAX_VALUE,
            rowsWorked = 1,
        )

    @Test
    fun `restored duration totals saturate instead of wrapping negative`() {
        val summary = SessionMetrics.summarize(listOf(session, session), null, zone)
        assertEquals(Long.MAX_VALUE, summary.totalSeconds)
        assertEquals(Int.MAX_VALUE, summary.totalMinutes)
        assertEquals(2, summary.sessionCount)
    }

    @Test
    fun `streamed pace duration saturates across repeated contributions`() {
        val accumulator = SessionPaceAccumulator(null, PaceGroupingInterval.DAY, zone, DayOfWeek.MONDAY)
        val last = session.copy(startedAt = Long.MAX_VALUE - 1_000L, endedAt = Long.MAX_VALUE)
        accumulator.add(last)
        accumulator.add(last)
        assertEquals(
            Long.MAX_VALUE,
            accumulator.values
                .getValue(1L)
                .values
                .single()
                .totalSeconds,
        )
    }

    @Test
    fun `duration derived end cannot wrap before the start`() {
        val summary = SessionMetrics.summarize(listOf(session.copy(endedAt = 0L)), null, zone)
        assertEquals(Long.MAX_VALUE, summary.totalSeconds)
        assertEquals(1, summary.sessionCount)
    }

    @Test
    fun `legacy row difference cannot wrap negative`() {
        val legacy =
            session.copy(durationSeconds = 1L, startRow = Int.MIN_VALUE, endRow = Int.MAX_VALUE, rowsWorked = 0)
        val summary = SessionMetrics.summarize(listOf(legacy), null, zone)
        assertEquals(Int.MAX_VALUE.toLong(), summary.totalRows)
    }

    @Test
    fun `last representable epoch day is split without overflowing the next midnight`() {
        val last = session.copy(startedAt = Long.MAX_VALUE - 1_000L, endedAt = Long.MAX_VALUE, durationSeconds = 1L)
        val daily = SessionMetrics.dailyActivitySeconds(listOf(last), LocalDate.MIN, zone)
        val buckets = SessionMetrics.paceBuckets(listOf(last), null, PaceGroupingInterval.DAY, zone)
        assertEquals(1L, daily.values.sum())
        assertEquals(1L, buckets.values.sumOf { it.totalSeconds })
        assertTrue(daily.size <= 366)
    }

    @Test
    fun `chart minutes cannot wrap when combining projects`() {
        val date = LocalDate.of(2026, 10, 1)
        val metric = PaceBucketMetric(Long.MAX_VALUE, 1L, 0f)
        val projects = mapOf(1L to mapOf(date to metric), 2L to mapOf(date to metric))
        val chart = InsightsViewModel.chartBucketsFromMetrics(projects, listOf(1L, 2L)).getValue(date)
        assertEquals(Int.MAX_VALUE, chart.totalMinutes)
    }

    @Test
    fun `previous period sums restored durations without wrapping`() {
        val start = LocalDate.of(2026, 9, 1)
        val values = mapOf(start to Long.MAX_VALUE, start.plusDays(1) to Long.MAX_VALUE)
        assertEquals(Long.MAX_VALUE, previousPeriodSeconds(values, start, start.plusMonths(1), start.plusMonths(2)))
    }
}
