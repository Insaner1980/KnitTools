package com.finnvek.knittools.ui.screens.insights

import com.finnvek.knittools.data.local.SessionProjectActivity
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.KnitSession
import com.finnvek.knittools.repository.SessionInsightsFacts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.WeekFields

class InsightsSessionAccumulatorTest {
    private val zone = ZoneId.systemDefault()
    private val today = LocalDate.of(2026, 9, 22)
    private val firstDay = WeekFields.of(currentInsightsLocale()).firstDayOfWeek

    @Test fun streamingPreservesExactMetricsZonesStreaksChartsFabricAndProjectTimes() {
        val rows =
            listOf(
                session(today.minusYears(2), 1, 61, 7),
                session(today.minusMonths(1), 2, 1799, 11),
                session(today.minusWeeks(1), 1, 1801, 13),
                session(today.minusDays(1), 2, 7201, 17, ZoneId.of("Pacific/Kiritimati")),
                session(today, 1, 91, 3, ZoneId.of("Pacific/Honolulu")),
            )
        val facts =
            SessionInsightsFacts(
                true,
                rows.groupBy { it.projectId }.map { (id, sessions) ->
                    SessionProjectActivity(id, sessions.maxOf { it.startedAt })
                },
                InsightsViewModel.firstSessionDate(rows, zone),
            )
        for (range in TimeRange.entries) {
            val params = params(range)
            val result = InsightsSessionAccumulator(params, facts)
            rows.chunked(2).forEach { batch -> batch.forEach(result::add) }
            val summary = SessionMetrics.summarize(rows, params.startMillis, zone)
            assertEquals(summary, result.summary)
            val earliest = InsightsViewModel.rangeStartDate(range, today, firstDay) ?: LocalDate.MIN
            assertEquals(SessionMetrics.activityDates(rows, earliest, zone), result.activeDates)
            // Putki lasketaan koko historiasta valitusta aikavälistä riippumatta.
            assertEquals(InsightsViewModel.calculateStreak(rows), result.streakDates.bestStreak())
            assertEquals(
                InsightsViewModel.calculateCurrentStreak(rows, null, zone, today),
                currentStreak(result.streakDates, today),
            )
            assertEquals(InsightsViewModel.firstSessionDate(rows, zone), result.firstDate)
            val axis = insightsChartAxis(range, today, result.firstDate, firstDay)
            val order =
                result
                    .timePerProject(
                        listOf(CounterProject(1, "A"), CounterProject(2, "B")),
                    ).map { it.projectId }
            assertEquals(
                fillChartBuckets(
                    axis,
                    InsightsViewModel.measuredChartBuckets(rows, params, axis, zone, firstDay, order),
                ),
                fillChartBuckets(
                    axis,
                    InsightsViewModel.chartBucketsFromMetrics(result.chart(axis.interval).values, order),
                ),
            )
            if (range == TimeRange.ALL_TIME) {
                assertEquals(
                    buildInsightsProjectFabric(rows, today, zone, firstDay, order),
                    buildInsightsProjectFabric(result.fabric.values, today, firstDay, order, result.firstDate),
                )
                assertEquals(12, axis.bucketStarts.size)
            }
            assertEquals(summary.totalRows, result.timePerProject(emptyList()).sumOf { it.totalRows })
        }
    }

    @Test fun streakDoesNotRestartAtTheSelectedRangeStart() {
        for (range in TimeRange.entries) {
            val accumulator = InsightsSessionAccumulator(params(range), SessionInsightsFacts(true, emptyList()))
            (0L until 23L).forEach { daysAgo -> accumulator.add(session(today.minusDays(daysAgo), 1, 60, 1)) }

            assertEquals(23, currentStreak(accumulator.streakDates, today))
            assertEquals(23, accumulator.streakDates.bestStreak())
        }
    }

    @Test fun emptySelectionRetainsGlobalExistenceWithoutInventingData() {
        val emptyFilter =
            InsightsSessionAccumulator(params(TimeRange.ALL_TIME), SessionInsightsFacts(true, emptyList()))
        assertTrue(emptyFilter.facts.hasAnySessionData)
        assertEquals(SessionMetricSummary(0, 0, 0), emptyFilter.summary)
        assertEquals(null, emptyFilter.firstDate)
    }

    @Test fun trendRequiresThirtyExactMinutesInThePreviousPeriod() {
        for (range in listOf(TimeRange.THIS_WEEK, TimeRange.THIS_MONTH)) {
            for (previousSeconds in listOf(1799L, 1800L, 1801L)) {
                val params = params(range)
                val accumulator = InsightsSessionAccumulator(params, SessionInsightsFacts(true, emptyList()))
                accumulator.add(session(requireNotNull(insightsPreviousStart(params)), 1, previousSeconds, 1))
                accumulator.add(session(today, 1, 3600L, 1))

                assertEquals(
                    "$range baseline=$previousSeconds",
                    if (previousSeconds < 1800L) null else InsightsTrend(100, InsightsTrendDirection.UP),
                    accumulator.trend(),
                )
            }
        }
    }

    @Test fun trendCalculatesPercentageFromExactSecondsInsteadOfDisplayMinutes() {
        for (range in listOf(TimeRange.THIS_WEEK, TimeRange.THIS_MONTH)) {
            val params = params(range)
            val accumulator = InsightsSessionAccumulator(params, SessionInsightsFacts(true, emptyList()))
            accumulator.add(session(requireNotNull(insightsPreviousStart(params)), 1, 1801L, 1))
            accumulator.add(session(today, 1, 3541L, 1))

            assertEquals(InsightsTrend(97, InsightsTrendDirection.UP), accumulator.trend())
        }
    }

    @Test fun allTimeLeadingBucketUsesTheStoredSessionZone() {
        val date = today.withDayOfMonth(1)
        val start =
            date
                .atTime(0, 30)
                .atOffset(ZoneOffset.MAX)
                .toInstant()
                .toEpochMilli()
        val row =
            KnitSession(
                projectId = 1,
                startedAt = start,
                endedAt = start + 60_000,
                startRow = 0,
                endRow = 1,
                durationMinutes = 1,
                durationSeconds = 60,
                rowsWorked = 1,
                zoneId = "+18:00",
            )
        val params = params(TimeRange.ALL_TIME).copy(zone = ZoneOffset.UTC)
        val accumulator =
            InsightsSessionAccumulator(
                params,
                SessionInsightsFacts(true, listOf(SessionProjectActivity(1, start)), date),
            )
        accumulator.add(row)
        assertEquals(60L, accumulator.summary.totalSeconds)
        val buckets = accumulator.chart(PaceGroupingInterval.DAY).values.getValue(1L)
        assertEquals(setOf(date), buckets.keys)
        assertEquals(60L, buckets.getValue(date).totalSeconds)
        assertEquals(1L, buckets.getValue(date).totalRows)
    }

    @Test fun futureBucketsContributeDurationWithoutRetainingInvisibleCharts() {
        val params = params(TimeRange.THIS_MONTH)
        val accumulator = InsightsSessionAccumulator(params, SessionInsightsFacts(true, emptyList()))
        repeat(5) { accumulator.add(session(today.plusYears(it + 1L), 1, 3600, 10)) }
        val chart = accumulator.chart(PaceGroupingInterval.DAY)
        assertTrue(chart.values.isEmpty())
        assertEquals(18_000L, accumulator.summary.totalSeconds)
    }

    private fun params(range: TimeRange): InsightsQueryParams =
        InsightsQueryParams(
            timeRange = range,
            startMillis =
                InsightsViewModel
                    .rangeStartDate(
                        range,
                        today,
                        firstDay,
                    )?.atStartOfDay(zone)
                    ?.toInstant()
                    ?.toEpochMilli(),
            zone = zone,
            currentDate = today,
        )

    private fun session(
        date: LocalDate,
        project: Long,
        seconds: Long,
        rows: Int,
        storedZone: ZoneId = zone,
    ): KnitSession {
        val started =
            date
                .atTime(23, 30)
                .atZone(storedZone)
                .toInstant()
                .toEpochMilli()
        return KnitSession(
            projectId = project,
            startedAt = started,
            endedAt = started + seconds * 1000,
            startRow = 0,
            endRow = rows,
            durationMinutes = 2,
            durationSeconds = seconds,
            rowsWorked = rows,
            zoneId = storedZone.id,
        )
    }
}
