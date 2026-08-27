package com.finnvek.knittools.ui.screens.insights

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
    fun `all time groups a few months into weeks instead of a couple of bars`() {
        val axis =
            insightsChartAxis(
                timeRange = TimeRange.ALL_TIME,
                today = LocalDate.of(2026, 7, 28),
                firstSessionDate = LocalDate.of(2026, 5, 20),
                firstDayOfWeek = monday,
            )

        // Kuukausiryhmittely olisi antanut kolme pylvästä koko historialle.
        assertEquals(PaceGroupingInterval.WEEK, axis.interval)
        assertEquals(LocalDate.of(2026, 5, 18), axis.bucketStarts.first())
        assertEquals(LocalDate.of(2026, 7, 27), axis.bucketStarts.last())
        assertTrue(axis.bucketStarts.size > 3)
        assertTrue(axis.bucketStarts.zipWithNext().all { (a, b) -> a.plusWeeks(1) == b })
    }

    @Test
    fun `all time starts at the first session month when history is long`() {
        val axis =
            insightsChartAxis(
                timeRange = TimeRange.ALL_TIME,
                today = LocalDate.of(2026, 7, 28),
                firstSessionDate = LocalDate.of(2025, 11, 20),
                firstDayOfWeek = monday,
            )

        assertEquals(PaceGroupingInterval.MONTH, axis.interval)
        assertEquals(LocalDate.of(2025, 11, 1), axis.bucketStarts.first())
        assertEquals(LocalDate.of(2026, 7, 1), axis.bucketStarts.last())
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

    @Test
    fun `previous period is truncated to the same elapsed days and excludes the tail of last period`() {
        // Nykyinen jakso: 2026-07-27 (ma) .. 2026-07-30 (to) = 4 kulunutta päivää.
        // Edellinen jakso katkaistaan siis 2026-07-20 .. 2026-07-24 (pl.), jolloin
        // viime viikon loppupää (pe-su) jää tarkoituksella pois.
        val dailySeconds =
            mapOf(
                LocalDate.of(2026, 7, 20) to minutesAsSeconds(30), // sisältyy: jakson ensimmäinen päivä
                LocalDate.of(2026, 7, 23) to minutesAsSeconds(10), // sisältyy: viimeinen kulunut päivä
                // CPD-OFF: Testin skenaariokohtainen asetelma pidetaan paikallisena ja luettavana.
                LocalDate.of(2026, 7, 24) to minutesAsSeconds(600), // ei sisälly: tasan katkaisurajalla
                LocalDate.of(2026, 7, 26) to minutesAsSeconds(999), // ei sisälly: viime viikon loppupää
            )

        val minutes =
            previousPeriodMinutes(
                dailySeconds = dailySeconds,
                previousStart = LocalDate.of(2026, 7, 20),
                currentStart = LocalDate.of(2026, 7, 27),
                today = LocalDate.of(2026, 7, 30),
                // CPD-ON
            )

        assertEquals(40, minutes)
    }

    @Test
    fun `previous period covers the full period when the current period has fully elapsed`() {
        // Nykyinen jakso on kokonaan kulunut (7 päivää), joten edellinen jakso ei
        // enää katkea vaan kattaa koko edellisen viikon eikä mitään jää pois.
        val dailySeconds =
            mapOf(
                LocalDate.of(2026, 7, 20) to minutesAsSeconds(30),
                // CPD-OFF: Testin skenaariokohtainen asetelma pidetaan paikallisena ja luettavana.
                LocalDate.of(2026, 7, 26) to minutesAsSeconds(999),
            )

        val minutes =
            previousPeriodMinutes(
                dailySeconds = dailySeconds,
                previousStart = LocalDate.of(2026, 7, 20),
                currentStart = LocalDate.of(2026, 7, 27),
                today = LocalDate.of(2026, 8, 2),
            )

        assertEquals(1029, minutes)
        // CPD-ON
    }

    @Test
    fun `previous period without matching activity is zero`() {
        val minutes =
            previousPeriodMinutes(
                dailySeconds = mapOf(LocalDate.of(2026, 6, 1) to minutesAsSeconds(45)),
                previousStart = LocalDate.of(2026, 7, 20),
                currentStart = LocalDate.of(2026, 7, 27),
                today = LocalDate.of(2026, 7, 30),
            )

        assertEquals(0, minutes)
    }

    @Test
    fun `previous month window never reaches into the current month on a long month end`() {
        // 31.3.: kuluneita päiviä on 31, mutta helmikuussa niitä on vain 28. Ilman
        // katkaisua ikkuna jatkuisi 4.3. asti ja laskisi maaliskuun alun sekä
        // vertailukohtaan että nykyiseen jaksoon.
        val dailySeconds =
            mapOf(
                LocalDate.of(2026, 2, 1) to minutesAsSeconds(20), // sisältyy: edellisen jakson alku
                LocalDate.of(2026, 2, 28) to minutesAsSeconds(25), // sisältyy: helmikuun viimeinen päivä
                LocalDate.of(2026, 3, 1) to minutesAsSeconds(600), // ei sisälly: kuuluu nykyiseen jaksoon
                // CPD-OFF: Testin skenaariokohtainen asetelma pidetaan paikallisena ja luettavana.
                LocalDate.of(2026, 3, 3) to minutesAsSeconds(900), // ei sisälly: kuuluu nykyiseen jaksoon
            )

        val minutes =
            previousPeriodMinutes(
                dailySeconds = dailySeconds,
                previousStart = LocalDate.of(2026, 2, 1),
                currentStart = LocalDate.of(2026, 3, 1),
                today = LocalDate.of(2026, 3, 31),
                // CPD-ON
            )

        assertEquals(45, minutes)
    }

    @Test
    fun `an ordinary mid month window still stops at the elapsed days`() {
        // 10.3.: kuluneita päiviä on 10, ikkuna 1.2.-11.2. (pl.) mahtuu helmikuuhun
        // eikä katkaisu saa lyhentää sitä.
        val dailySeconds =
            mapOf(
                LocalDate.of(2026, 2, 1) to minutesAsSeconds(20),
                LocalDate.of(2026, 2, 10) to minutesAsSeconds(25),
                LocalDate.of(2026, 2, 11) to minutesAsSeconds(600), // ei sisälly: tasan katkaisurajalla
                LocalDate.of(2026, 2, 28) to minutesAsSeconds(900), // ei sisälly: kuluneiden päivien jälkeen
            )

        val minutes =
            previousPeriodMinutes(
                dailySeconds = dailySeconds,
                previousStart = LocalDate.of(2026, 2, 1),
                currentStart = LocalDate.of(2026, 3, 1),
                today = LocalDate.of(2026, 3, 10),
            )

        assertEquals(45, minutes)
    }

    @Test
    fun `previous period rounds once over the window instead of once per active day`() {
        // Kolme päivää, joista jokainen jäisi omalla pyöristyksellään minuutin yli:
        // 3 x 90 s = 270 s = 5 min. Päiväkohtaisesti pyöristettynä summa olisi 6 min,
        // eli vertailukohta kasvaisi ja trendi näyttäisi laskua jota ei ole.
        val dailySeconds =
            mapOf(
                LocalDate.of(2026, 7, 20) to 90L,
                LocalDate.of(2026, 7, 21) to 90L,
                LocalDate.of(2026, 7, 22) to 90L,
            )

        val minutes =
            previousPeriodMinutes(
                dailySeconds = dailySeconds,
                previousStart = LocalDate.of(2026, 7, 20),
                currentStart = LocalDate.of(2026, 7, 27),
                today = LocalDate.of(2026, 8, 2),
            )

        assertEquals(5, minutes)
    }

    private fun minutesAsSeconds(minutes: Int): Long = minutes * 60L

    @Test
    fun `short axes label every bucket`() {
        assertEquals(listOf(0, 1, 2, 3, 4, 5, 6), axisLabelIndices(bucketCount = 7, maxLabels = 7))
    }

    @Test
    fun `long axes thin labels out evenly from the first bucket`() {
        assertEquals(listOf(0, 7, 14, 21, 28), axisLabelIndices(bucketCount = 31, maxLabels = 5))
    }

    @Test
    fun `labels stay within range and never exceed the maximum`() {
        (6..60).forEach { count ->
            val indices = axisLabelIndices(bucketCount = count, maxLabels = 5)

            assertTrue("count=$count produced ${indices.size} labels", indices.size <= 5)
            assertTrue("count=$count produced out of range indices", indices.all { it in 0 until count })
            assertEquals("count=$count produced duplicates", indices.distinct(), indices)
            assertEquals("count=$count did not anchor the left edge", 0, indices.first())
        }
    }

    @Test
    fun `empty axis has no labels`() {
        assertEquals(emptyList<Int>(), axisLabelIndices(bucketCount = 0, maxLabels = 5))
    }

    @Test
    fun `a tiny previous period never produces a percentage`() {
        // 3 min → 18 min oli "500 % more than last week": kohinaa, ei tietoa.
        assertNull(insightsTrend(currentMinutes = 18, previousMinutes = 3))
        assertNull(insightsTrend(currentMinutes = 18, previousMinutes = 0))
        assertNotNull(insightsTrend(currentMinutes = 90, previousMinutes = 60))
    }
}
