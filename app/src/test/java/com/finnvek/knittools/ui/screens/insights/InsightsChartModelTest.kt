package com.finnvek.knittools.ui.screens.insights

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class InsightsChartModelTest {
    private val monday = DayOfWeek.MONDAY

    @Test
    fun `week axis stops at today instead of drawing days that have not happened`() {
        val axis =
            insightsChartAxis(
                timeRange = TimeRange.THIS_WEEK,
                today = LocalDate.of(2026, 7, 28),
                firstSessionDate = LocalDate.of(2026, 1, 1),
                firstDayOfWeek = monday,
            )

        assertEquals(PaceGroupingInterval.DAY, axis.interval)
        assertEquals(
            listOf(LocalDate.of(2026, 7, 27), LocalDate.of(2026, 7, 28)),
            axis.bucketStarts,
        )
    }

    @Test
    fun `week axis covers the whole week on its last day`() {
        val axis =
            insightsChartAxis(
                timeRange = TimeRange.THIS_WEEK,
                today = LocalDate.of(2026, 8, 2),
                firstSessionDate = LocalDate.of(2026, 1, 1),
                firstDayOfWeek = monday,
            )

        assertEquals(7, axis.bucketStarts.size)
        assertEquals(LocalDate.of(2026, 7, 27), axis.bucketStarts.first())
        assertEquals(LocalDate.of(2026, 8, 2), axis.bucketStarts.last())
    }

    @Test
    fun `month axis covers the calendar month up to today`() {
        val axis =
            insightsChartAxis(
                timeRange = TimeRange.THIS_MONTH,
                today = LocalDate.of(2026, 7, 28),
                firstSessionDate = LocalDate.of(2026, 1, 1),
                firstDayOfWeek = monday,
            )

        assertEquals(PaceGroupingInterval.DAY, axis.interval)
        assertEquals(28, axis.bucketStarts.size)
        assertEquals(LocalDate.of(2026, 7, 1), axis.bucketStarts.first())
        assertEquals(LocalDate.of(2026, 7, 28), axis.bucketStarts.last())
    }

    @Test
    fun `all time caps at twelve monthly buckets`() {
        val axis =
            insightsChartAxis(
                timeRange = TimeRange.ALL_TIME,
                today = LocalDate.of(2026, 7, 28),
                firstSessionDate = LocalDate.of(2021, 3, 4),
                firstDayOfWeek = monday,
            )

        assertEquals(PaceGroupingInterval.MONTH, axis.interval)
        assertEquals(ALL_TIME_MONTH_BUCKET_LIMIT, axis.bucketStarts.size)
        assertEquals(LocalDate.of(2025, 8, 1), axis.bucketStarts.first())
        assertEquals(LocalDate.of(2026, 7, 1), axis.bucketStarts.last())
    }

    @Test
    fun `all time starts at the first session month when history is short`() {
        val axis =
            insightsChartAxis(
                timeRange = TimeRange.ALL_TIME,
                today = LocalDate.of(2026, 7, 28),
                firstSessionDate = LocalDate.of(2026, 5, 20),
                firstDayOfWeek = monday,
            )

        assertEquals(PaceGroupingInterval.MONTH, axis.interval)
        assertEquals(
            listOf(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 6, 1), LocalDate.of(2026, 7, 1)),
            axis.bucketStarts,
        )
    }

    @Test
    fun `all time day buckets start at the first session not at the month start`() {
        val axis =
            insightsChartAxis(
                timeRange = TimeRange.ALL_TIME,
                today = LocalDate.of(2026, 7, 28),
                firstSessionDate = LocalDate.of(2026, 7, 2),
                firstDayOfWeek = monday,
            )

        assertEquals(PaceGroupingInterval.DAY, axis.interval)
        assertEquals(27, axis.bucketStarts.size)
        assertEquals(LocalDate.of(2026, 7, 2), axis.bucketStarts.first())
        assertEquals(LocalDate.of(2026, 7, 28), axis.bucketStarts.last())
    }

    @Test
    fun `all time without sessions falls back to day buckets`() {
        val axis =
            insightsChartAxis(
                timeRange = TimeRange.ALL_TIME,
                today = LocalDate.of(2026, 7, 28),
                firstSessionDate = null,
                firstDayOfWeek = monday,
            )

        assertEquals(PaceGroupingInterval.DAY, axis.interval)
        assertEquals(28, axis.bucketStarts.size)
    }

    @Test
    fun `missing buckets are filled with zeros instead of gaps`() {
        val axis =
            insightsChartAxis(
                timeRange = TimeRange.THIS_WEEK,
                today = LocalDate.of(2026, 7, 28),
                firstSessionDate = LocalDate.of(2026, 7, 27),
                firstDayOfWeek = monday,
            )
        val measured =
            mapOf(
                LocalDate.of(2026, 7, 28) to
                    InsightsChartBucket(LocalDate.of(2026, 7, 28), totalMinutes = 45, totalRows = 5),
            )

        val buckets = fillChartBuckets(axis, measured)

        assertEquals(2, buckets.size)
        assertEquals(axis.bucketStarts, buckets.map { it.bucketStart })
        assertEquals(45, buckets[1].totalMinutes)
        assertTrue(buckets.filterIndexed { index, _ -> index != 1 }.all { it.totalMinutes == 0 })
    }

    @Test
    fun `default selection is the most recent bucket with data not the largest`() {
        val buckets =
            listOf(
                InsightsChartBucket(LocalDate.of(2026, 7, 25), totalMinutes = 200, totalRows = 40),
                InsightsChartBucket(LocalDate.of(2026, 7, 26), totalMinutes = 45, totalRows = 5),
                InsightsChartBucket(LocalDate.of(2026, 7, 27), totalMinutes = 0, totalRows = 0),
            )

        assertEquals(1, defaultSelectedBucketIndex(buckets))
    }

    @Test
    fun `default selection is absent when no bucket has data`() {
        val buckets =
            listOf(
                InsightsChartBucket(LocalDate.of(2026, 7, 26), totalMinutes = 0, totalRows = 0),
                InsightsChartBucket(LocalDate.of(2026, 7, 27), totalMinutes = 0, totalRows = 0),
            )

        assertNull(defaultSelectedBucketIndex(buckets))
    }

    @Test
    fun `trend reports growth against the previous period`() {
        val trend = insightsTrend(currentMinutes = 120, previousMinutes = 100)

        assertEquals(InsightsTrend(percentChange = 20, direction = InsightsTrendDirection.UP), trend)
    }

    @Test
    fun `trend reports decline as a positive magnitude`() {
        val trend = insightsTrend(currentMinutes = 80, previousMinutes = 100)

        assertEquals(InsightsTrend(percentChange = 20, direction = InsightsTrendDirection.DOWN), trend)
    }

    @Test
    fun `equal periods are flat`() {
        val trend = insightsTrend(currentMinutes = 100, previousMinutes = 100)

        assertEquals(InsightsTrend(percentChange = 0, direction = InsightsTrendDirection.FLAT), trend)
    }

    @Test
    fun `trend is unavailable without a previous period to divide by`() {
        assertNull(insightsTrend(currentMinutes = 90, previousMinutes = 0))
        assertNull(insightsTrend(currentMinutes = 0, previousMinutes = 0))
    }

    @Test
    fun `dropping to zero is a full decline`() {
        val trend = insightsTrend(currentMinutes = 0, previousMinutes = 100)

        assertEquals(InsightsTrend(percentChange = 100, direction = InsightsTrendDirection.DOWN), trend)
    }
}
