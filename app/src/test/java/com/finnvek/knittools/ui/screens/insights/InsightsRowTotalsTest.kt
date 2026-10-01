package com.finnvek.knittools.ui.screens.insights

import com.finnvek.knittools.data.local.SessionProjectActivity
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.KnitSession
import com.finnvek.knittools.repository.SessionInsightsFacts
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneOffset

@RunWith(Parameterized::class)
class InsightsRowTotalsTest(
    private val firstRows: Int,
    private val secondRows: Int,
    private val expectedRows: Long,
) {
    private val date = LocalDate.of(2026, 10, 1)
    private val zone = ZoneOffset.UTC
    private val start = date.atTime(12, 0).toInstant(zone).toEpochMilli()
    private val sessions = listOf(session(1, firstRows), session(2, secondRows))

    @Test
    fun summaryPreservesRowSum() {
        val summary = SessionMetrics.summarize(sessions, null, zone)

        assertEquals(expectedRows, summary.totalRows)
        assertEquals(3_600L, summary.totalSeconds)
        assertEquals(expectedRows.toFloat(), summary.rowsPerHour, 0f)
    }

    @Test
    fun paceBucketsPreserveRowSum() {
        for (interval in PaceGroupingInterval.entries) {
            val bucket = SessionMetrics.paceBuckets(sessions, null, interval, zone).values.single()

            assertEquals(expectedRows, bucket.totalRows)
            assertEquals(expectedRows.toFloat(), bucket.rowsPerHour, 0f)
        }
    }

    @Test
    fun streamingPreservesOverallProjectAndChartRows() {
        val accumulator =
            InsightsSessionAccumulator(
                InsightsQueryParams(timeRange = TimeRange.ALL_TIME, zone = zone, currentDate = date),
                SessionInsightsFacts(true, listOf(SessionProjectActivity(1, start)), date),
            )
        sessions.forEach(accumulator::add)

        assertEquals(expectedRows, accumulator.summary.totalRows)
        assertEquals(expectedRows, accumulator.timePerProject(listOf(CounterProject(1, "A"))).single().totalRows)
        val pace = accumulator.chart(PaceGroupingInterval.DAY).values
        assertEquals(expectedRows, pace.getValue(1).values.single().totalRows)
        val chart = InsightsViewModel.chartBucketsFromMetrics(pace, listOf(1))
        assertEquals(expectedRows, chart.getValue(date).totalRows)
    }

    @Test
    fun chartPreservesSumAcrossProjects() {
        val accumulator = SessionPaceAccumulator(null, PaceGroupingInterval.DAY, zone, DayOfWeek.MONDAY)
        accumulator.add(sessions[0])
        accumulator.add(sessions[1].copy(projectId = 2))

        val chart = InsightsViewModel.chartBucketsFromMetrics(accumulator.values, listOf(1, 2))

        assertEquals(expectedRows, chart.getValue(date).totalRows)
    }

    private fun session(id: Long, rows: Int) =
        KnitSession(
            id = id,
            projectId = 1,
            startedAt = start + (id - 1) * 1_800_000L,
            endedAt = start + id * 1_800_000L,
            startRow = 0,
            endRow = rows,
            durationMinutes = 30,
            durationSeconds = 1_800,
            rowsWorked = rows,
            zoneId = zone.id,
        )

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0} + {1} = {2}")
        fun cases(): List<Array<Any>> =
            listOf(
                arrayOf(12, 28, 40L),
                arrayOf(Int.MAX_VALUE - 2, 1, 2_147_483_646L),
                arrayOf(Int.MAX_VALUE - 1, 1, 2_147_483_647L),
                arrayOf(Int.MAX_VALUE, 1, 2_147_483_648L),
                arrayOf(1_500_000_000, 1_500_000_000, 3_000_000_000L),
            )
    }
}
