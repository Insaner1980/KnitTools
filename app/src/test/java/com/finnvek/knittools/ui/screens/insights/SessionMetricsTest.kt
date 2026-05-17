package com.finnvek.knittools.ui.screens.insights

import com.finnvek.knittools.domain.model.KnitSession
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class SessionMetricsTest {
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
    fun `daily activity splits minutes over local dates`() {
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
            SessionMetrics.dailyActivityMinutes(
                sessions = listOf(session),
                earliestDate = LocalDate.of(2026, 1, 1),
                zone = zone,
            )

        assertEquals(10, activity[LocalDate.of(2026, 1, 1)])
        assertEquals(20, activity[LocalDate.of(2026, 1, 2)])
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
