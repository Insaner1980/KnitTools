package com.finnvek.knittools.ui.screens.insights

import com.finnvek.knittools.domain.model.KnitSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class SessionMetricsTest {
    @Test
    fun `repeated midnight splits both activity and pace into the correct dates`() {
        val zone = ZoneId.of("America/St_Johns")
        val start = Instant.parse("2009-11-01T03:00:00Z").toEpochMilli()
        val firstDate = LocalDate.of(2009, 10, 31)
        val expected = mapOf(firstDate to 1_800L, firstDate.plusDays(1) to 600L)
        val session =
            KnitSession(
                id = 1L,
                projectId = 1L,
                startedAt = start,
                endedAt = start + 2_400_000L,
                startRow = 0,
                endRow = 4,
                durationMinutes = 40,
                durationSeconds = 2_400L,
                rowsWorked = 4,
                zoneId = zone.id,
            )

        assertEquals(expected, SessionMetrics.dailyActivitySeconds(listOf(session), firstDate, zone))
        val pace = SessionMetrics.paceBuckets(listOf(session), null, PaceGroupingInterval.DAY, zone)
        assertEquals(expected, pace.mapValues { it.value.totalSeconds })
    }

    @Test
    fun `weekly and monthly totals agree with activity and pace dates after travel`() {
        val deviceZone = ZoneId.of("UTC")
        for (date in listOf(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 5))) {
            val tokyo = ZoneId.of("Asia/Tokyo")
            val losAngeles = ZoneId.of("America/Los_Angeles")
            val includedStart =
                date
                    .atTime(0, 30)
                    .atZone(tokyo)
                    .toInstant()
                    .toEpochMilli()
            val excludedStart =
                date
                    .minusDays(1)
                    .atTime(23, 30)
                    .atZone(losAngeles)
                    .toInstant()
                    .toEpochMilli()
            val sessions =
                listOf(includedStart to tokyo, excludedStart to losAngeles).mapIndexed { index, (start, sessionZone) ->
                    KnitSession(
                        id = index + 1L,
                        projectId = 1L,
                        startedAt = start,
                        endedAt = start + 1_800_000L,
                        startRow = 0,
                        endRow = 10,
                        durationMinutes = 30,
                        durationSeconds = 1_800L,
                        rowsWorked = 10,
                        zoneId = sessionZone.id,
                    )
                }
            val rangeStart = date.atStartOfDay(deviceZone).toInstant().toEpochMilli()

            val summary = SessionMetrics.summarize(sessions, rangeStart, deviceZone)
            val activity = SessionMetrics.dailyActivitySeconds(sessions, date, deviceZone)
            val pace = SessionMetrics.paceBuckets(sessions, rangeStart, PaceGroupingInterval.DAY, deviceZone)

            assertEquals(1_800L, summary.totalSeconds)
            assertEquals(10L, summary.totalRows)
            assertEquals(1, summary.sessionCount)
            assertEquals(summary.totalSeconds, activity.values.sum())
            assertEquals(summary.totalSeconds, pace.values.sumOf { it.totalSeconds })
        }
    }

    @Test
    fun `rows per hour uses exact seconds instead of rounded minutes`() {
        val session =
            KnitSession(
                id = 1L,
                projectId = 1L,
                startedAt = 0L,
                endedAt = 119_000L,
                startRow = 0,
                endRow = 10,
                durationMinutes = 1,
                durationSeconds = 119L,
                rowsWorked = 10,
            )

        val summary = SessionMetrics.summarize(listOf(session), rangeStartMillis = null, zone = ZoneId.of("UTC"))

        assertEquals(119L, summary.totalSeconds)
        assertEquals(10, summary.totalRows)
        assertEquals(302.52f, summary.rowsPerHour, 0.01f)
    }

    @Test
    fun `range summary counts only local-day overlap for midnight crossing session`() {
        val zone = ZoneId.of("Europe/Helsinki")
        val startedAt = instantMillis(2026, 1, 1, 23, 50, zone)
        val endedAt = instantMillis(2026, 1, 2, 0, 20, zone)
        val rangeStart = instantMillis(2026, 1, 2, 0, 0, zone)
        val session =
            KnitSession(
                id = 1L,
                projectId = 1L,
                startedAt = startedAt,
                endedAt = endedAt,
                startRow = 10,
                endRow = 40,
                durationMinutes = 30,
                durationSeconds = 1_800L,
                rowsWorked = 30,
            )

        val summary = SessionMetrics.summarize(listOf(session), rangeStartMillis = rangeStart, zone = zone)

        assertEquals(1_200L, summary.totalSeconds)
        assertEquals(20, summary.totalRows)
        assertEquals(60.0f, summary.rowsPerHour, 0.01f)
    }

    @Test
    fun `daily activity splits seconds over local dates`() {
        val zone = ZoneId.of("Europe/Helsinki")
        val session =
            KnitSession(
                id = 1L,
                projectId = 1L,
                startedAt = instantMillis(2026, 1, 1, 23, 50, zone),
                endedAt = instantMillis(2026, 1, 2, 0, 20, zone),
                startRow = 10,
                endRow = 40,
                durationMinutes = 30,
                durationSeconds = 1_800L,
                rowsWorked = 30,
            )

        val activity =
            SessionMetrics.dailyActivitySeconds(
                sessions = listOf(session),
                earliestDate = LocalDate.of(2026, 1, 1),
                zone = zone,
            )

        assertEquals(600L, activity[LocalDate.of(2026, 1, 1)])
        assertEquals(1_200L, activity[LocalDate.of(2026, 1, 2)])
    }

    @Test
    fun `daily activity keeps the session start zone after the device zone changes`() {
        val sessionZone = ZoneId.of("Pacific/Kiritimati")
        val currentDeviceZone = ZoneId.of("Pacific/Honolulu")
        val session =
            KnitSession(
                id = 1L,
                projectId = 1L,
                startedAt = instantMillis(2026, 1, 2, 0, 30, sessionZone),
                endedAt = instantMillis(2026, 1, 2, 1, 0, sessionZone),
                startRow = 0,
                endRow = 10,
                durationMinutes = 30,
                durationSeconds = 1_800L,
                rowsWorked = 10,
                zoneId = sessionZone.id,
            )

        val activity =
            SessionMetrics.dailyActivitySeconds(
                sessions = listOf(session),
                earliestDate = LocalDate.of(2026, 1, 1),
                zone = currentDeviceZone,
            )

        assertEquals(mapOf(LocalDate.of(2026, 1, 2) to 1_800L), activity)
    }

    @Test
    fun `cross midnight splits preserve synthetic session totals`() {
        val zone = ZoneId.of("UTC")
        val midnight = instantMillis(2026, 1, 2, 0, 0, zone)
        val session =
            KnitSession(
                id = 1L,
                projectId = 1L,
                startedAt = midnight - 500L,
                endedAt = midnight + 500L,
                startRow = 0,
                endRow = 1,
                durationMinutes = 0,
                durationSeconds = 0L,
                rowsWorked = 1,
            )

        val activity =
            SessionMetrics.dailyActivitySeconds(
                sessions = listOf(session),
                earliestDate = LocalDate.of(2026, 1, 1),
                zone = zone,
            )
        val paceBuckets =
            SessionMetrics.paceBuckets(
                sessions = listOf(session),
                rangeStartMillis = null,
                interval = PaceGroupingInterval.DAY,
                zone = zone,
            )

        assertEquals(1L, activity.values.sum())
        assertEquals(1L, paceBuckets.values.sumOf { it.totalSeconds })
        assertEquals(1, paceBuckets.values.sumOf { it.totalRows })
    }

    @Test
    fun `zero row sessions still contribute duration to pace buckets`() {
        val zone = ZoneId.of("UTC")
        val productiveSession =
            KnitSession(
                id = 1L,
                projectId = 1L,
                startedAt = 0L,
                endedAt = 1_800_000L,
                startRow = 0,
                endRow = 10,
                durationMinutes = 30,
                durationSeconds = 1_800L,
                rowsWorked = 10,
            )
        val zeroRowSession =
            KnitSession(
                id = 2L,
                projectId = 1L,
                startedAt = 1_800_000L,
                endedAt = 3_600_000L,
                startRow = 10,
                endRow = 10,
                durationMinutes = 30,
                durationSeconds = 1_800L,
                rowsWorked = 0,
            )

        val bucket =
            SessionMetrics
                .paceBuckets(
                    sessions = listOf(productiveSession, zeroRowSession),
                    rangeStartMillis = null,
                    interval = PaceGroupingInterval.DAY,
                    zone = zone,
                ).values
                .single()

        assertEquals(3_600L, bucket.totalSeconds)
        assertEquals(10, bucket.totalRows)
        assertEquals(10f, bucket.rowsPerHour, 0.01f)
    }

    @Test
    fun `range summary ignores overlap too small to contribute rounded seconds or rows`() {
        val zone = ZoneId.of("UTC")
        val session =
            KnitSession(
                id = 1L,
                projectId = 1L,
                startedAt = -3_600_000L,
                endedAt = 1L,
                startRow = 0,
                endRow = 3_600,
                durationMinutes = 60,
                durationSeconds = 3_600L,
                rowsWorked = 3_600,
            )

        val summary = SessionMetrics.summarize(listOf(session), rangeStartMillis = 0L, zone = zone)

        assertEquals(0L, summary.totalSeconds)
        assertEquals(0, summary.totalRows)
        assertEquals(0, summary.sessionCount)
        assertEquals(0f, summary.rowsPerHour, 0.01f)
    }

    @Test
    fun `malformed multi year session expands into at most one bounded analysis year`() {
        val zone = ZoneId.of("UTC")
        val session =
            KnitSession(
                id = 1L,
                projectId = 1L,
                startedAt = instantMillis(1970, 1, 1, 0, 0, zone),
                endedAt = instantMillis(2070, 1, 1, 0, 0, zone),
                startRow = 0,
                endRow = 100,
                durationMinutes = 60,
                durationSeconds = 3_600L,
                rowsWorked = 100,
            )

        val activity = SessionMetrics.dailyActivitySeconds(listOf(session), LocalDate.MIN, zone)
        val buckets = SessionMetrics.paceBuckets(listOf(session), null, PaceGroupingInterval.DAY, zone)

        assertTrue(activity.size <= 366)
        assertTrue(buckets.size <= 366)
    }

    @Test
    fun `apportioned minutes sum to the total instead of each part rounding up`() {
        // Erikseen ylöspäin pyöristettynä osuudet tuottaisivat 565 minuuttia.
        val parts = listOf(11_430L, 9_150L, 5_670L, 4_830L, 2_670L)

        val minutes = apportionDisplayMinutes(parts)

        assertEquals(secondsToDisplayMinutes(parts.sum()), minutes.sum())
        assertEquals(listOf(191, 153, 95, 80, 44), minutes)
    }

    @Test
    fun `apportioned minutes give every measured part at least a minute`() {
        assertEquals(listOf(1, 0, 60), apportionDisplayMinutes(listOf(20L, 0L, 3_600L)))
        assertEquals(listOf(1, 1, 1), apportionDisplayMinutes(listOf(1L, 1L, 120L)))
    }

    @Test
    fun `apportioned minutes stay empty without measured seconds`() {
        assertEquals(listOf(0, 0), apportionDisplayMinutes(listOf(0L, 0L)))
        assertEquals(emptyList<Int>(), apportionDisplayMinutes(emptyList()))
    }

    @Test
    fun `display minutes do not wrap for large restored durations`() {
        assertEquals(Int.MAX_VALUE, secondsToDisplayMinutes((Int.MAX_VALUE.toLong() + 1L) * 60L))
        assertEquals(Int.MAX_VALUE, secondsToDisplayMinutes(Long.MAX_VALUE))
        assertEquals(1, secondsToDisplayMinutes(59L))
        assertEquals(1, secondsToDisplayMinutes(60L))
        assertEquals(2, secondsToDisplayMinutes(61L))
    }

    @Test
    fun `apportioned large durations preserve positive project minutes`() {
        val largestMinutes = Int.MAX_VALUE.toLong() * 60L
        assertEquals(
            listOf(Int.MAX_VALUE, Int.MAX_VALUE),
            apportionDisplayMinutes(listOf(largestMinutes, largestMinutes)),
        )
        assertEquals(
            listOf(Int.MAX_VALUE, Int.MAX_VALUE, 1),
            apportionDisplayMinutes(listOf(Long.MAX_VALUE, Long.MAX_VALUE, 1L)),
        )
    }

    private fun instantMillis(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
        zone: ZoneId,
    ): Long =
        ZonedDateTime
            .of(year, month, day, hour, minute, 0, 0, zone)
            .toInstant()
            .toEpochMilli()
}
