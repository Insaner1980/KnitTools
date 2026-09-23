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

    @Test fun boundedRangesRequestThePreviousPeriodAndEveryValidStoredZone() {
        for ((range, expected) in listOf(
            TimeRange.THIS_WEEK to LocalDate.of(2026, 9, 14),
            TimeRange.THIS_MONTH to LocalDate.of(2026, 8, 1),
        )) {
            val params = params(range)
            val previous =
                if (range ==
                    TimeRange.THIS_WEEK
                ) {
                    requireNotNull(InsightsViewModel.rangeStartDate(range, today, firstDay)).minusWeeks(1)
                } else {
                    expected
                }
            assertEquals(previous.atStartOfDay(ZoneOffset.MAX).toInstant().toEpochMilli(), insightsQueryStart(params))
        }
        assertEquals(null, insightsQueryStart(params(TimeRange.ALL_TIME)))
    }

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
            assertEquals(InsightsViewModel.calculateStreak(rows, params.startMillis), result.activeDates.bestStreak())
            assertEquals(
                InsightsViewModel.calculateCurrentStreak(rows, params.startMillis, zone, today),
                currentStreak(result.activeDates, today),
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
                    buildInsightsProjectFabric(result.fabric.values, today, firstDay, order),
                )
                assertEquals(12, axis.bucketStarts.size)
            }
            assertEquals(summary.totalRows, result.timePerProject(emptyList()).sumOf { it.totalRows })
        }
    }

    @Test fun emptySelectionRetainsGlobalExistenceWithoutInventingData() {
        val emptyFilter =
            InsightsSessionAccumulator(params(TimeRange.ALL_TIME), SessionInsightsFacts(true, emptyList()))
        assertTrue(emptyFilter.facts.hasAnySessionData)
        assertEquals(SessionMetricSummary(0, 0, 0), emptyFilter.summary)
        assertEquals(null, emptyFilter.firstDate)
    }

    @Test fun allTimeLeadingBucketKeepsTheExistingDeviceZoneClip() {
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
        assertTrue(accumulator.chart(PaceGroupingInterval.DAY).values.isEmpty())
    }

    @Test fun futureBucketsKeepTheComparisonFlagWithoutRetainingInvisibleCharts() {
        val params = params(TimeRange.THIS_MONTH)
        val accumulator = InsightsSessionAccumulator(params, SessionInsightsFacts(true, emptyList()))
        repeat(5) { accumulator.add(session(today.plusYears(it + 1L), 1, 3600, 10)) }
        val chart = accumulator.chart(PaceGroupingInterval.DAY)
        assertTrue(chart.values.isEmpty())
        assertEquals(2, chart.futureMeasuredBucketCount)
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
