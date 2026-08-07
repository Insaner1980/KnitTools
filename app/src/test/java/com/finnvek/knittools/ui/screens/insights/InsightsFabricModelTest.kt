package com.finnvek.knittools.ui.screens.insights

import com.finnvek.knittools.domain.model.KnitSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class InsightsFabricModelTest {
    @Test
    fun `empty input shows only the current partial week ending today`() {
        val today = LocalDate.of(2026, 8, 5)

        val model =
            buildInsightsFabricModel(
                sessions = emptyList(),
                today = today,
                firstDayOfWeek = DayOfWeek.MONDAY,
                zone = ZoneId.of("UTC"),
            )

        assertEquals(1, model.weekCount)
        assertEquals(
            listOf(
                LocalDate.of(2026, 8, 3),
                LocalDate.of(2026, 8, 4),
                LocalDate.of(2026, 8, 5),
            ),
            model.days.map { it.date },
        )
        assertTrue(model.days.all { it.totalMinutes == 0 && it.totalRows == 0 })
        assertTrue(model.days.all { it.dominantProjectId == null && it.level == 0 })
    }

    @Test
    fun `window starts from the first session week using the locale week boundary`() {
        val zone = ZoneId.of("UTC")
        val today = LocalDate.of(2026, 8, 5)
        val session = session(projectId = 1L, date = LocalDate.of(2026, 7, 30), minutes = 30, zone = zone)

        val mondayModel =
            buildInsightsFabricModel(
                sessions = listOf(session),
                today = today,
                firstDayOfWeek = DayOfWeek.MONDAY,
                zone = zone,
            )
        val sundayModel =
            buildInsightsFabricModel(
                sessions = listOf(session),
                today = today,
                firstDayOfWeek = DayOfWeek.SUNDAY,
                zone = zone,
            )

        assertEquals(LocalDate.of(2026, 7, 27), mondayModel.days.first().date)
        assertEquals(LocalDate.of(2026, 7, 26), sundayModel.days.first().date)
        assertEquals(today, mondayModel.days.last().date)
        assertEquals(today, sundayModel.days.last().date)
        assertEquals(2, mondayModel.weekCount)
        assertEquals(2, sundayModel.weekCount)
    }

    @Test
    fun `cross-midnight work uses the persisted session zone and exact row allocation`() {
        val sessionZone = ZoneId.of("Pacific/Kiritimati")
        val fallbackZone = ZoneId.of("Pacific/Honolulu")
        val firstDate = LocalDate.of(2026, 1, 2)
        val startedAt = instantMillis(firstDate, hour = 23, minute = 50, zone = sessionZone)
        val endedAt = instantMillis(firstDate.plusDays(1), hour = 0, minute = 20, zone = sessionZone)
        val session =
            KnitSession(
                projectId = 7L,
                startedAt = startedAt,
                endedAt = endedAt,
                startRow = 0,
                endRow = 30,
                durationMinutes = 30,
                durationSeconds = 1_800L,
                rowsWorked = 30,
                zoneId = sessionZone.id,
            )

        val model =
            buildInsightsFabricModel(
                sessions = listOf(session),
                today = firstDate.plusDays(1),
                firstDayOfWeek = DayOfWeek.MONDAY,
                zone = fallbackZone,
            )

        val activeDays = model.days.filter { it.totalMinutes > 0 }
        assertEquals(listOf(firstDate, firstDate.plusDays(1)), activeDays.map { it.date })
        assertEquals(listOf(10, 20), activeDays.map { it.totalMinutes })
        assertEquals(listOf(10, 20), activeDays.map { it.totalRows })
        assertTrue(activeDays.all { it.dominantProjectId == 7L })
    }

    @Test
    fun `daily minutes use the existing per-project display allocation`() {
        val zone = ZoneId.of("UTC")
        val day = LocalDate.of(2026, 4, 8)
        val sessions =
            listOf(
                sessionSeconds(projectId = 1L, date = day, seconds = 10L, rows = 1, zone = zone),
                sessionSeconds(projectId = 2L, date = day, seconds = 10L, rows = 1, zone = zone),
            )

        val activeDay =
            buildInsightsFabricModel(
                sessions = sessions,
                today = day,
                firstDayOfWeek = DayOfWeek.MONDAY,
                zone = zone,
            ).days.single { it.date == day }

        assertEquals(2, activeDay.totalMinutes)
        assertEquals(2, activeDay.totalRows)
    }

    @Test
    fun `dominant project compares exact seconds when display minutes are equal`() {
        val zone = ZoneId.of("UTC")
        val day = LocalDate.of(2026, 4, 8)
        val sessions =
            listOf(
                sessionSeconds(projectId = 1L, date = day, seconds = 61L, rows = 1, zone = zone),
                sessionSeconds(projectId = 9L, date = day, seconds = 119L, rows = 1, zone = zone),
            )

        val activeDay =
            buildInsightsFabricModel(
                sessions = sessions,
                today = day,
                firstDayOfWeek = DayOfWeek.MONDAY,
                zone = zone,
            ).days.single { it.date == day }

        assertEquals(9L, activeDay.dominantProjectId)
    }

    @Test
    fun `dominant project ties choose the smaller project id regardless of session order`() {
        val zone = ZoneId.of("UTC")
        val day = LocalDate.of(2026, 4, 8)
        val sessions =
            listOf(
                sessionSeconds(projectId = 9L, date = day, seconds = 600L, rows = 1, zone = zone),
                sessionSeconds(projectId = 3L, date = day, seconds = 600L, rows = 1, zone = zone),
            )

        val activeDay =
            buildInsightsFabricModel(
                sessions = sessions,
                today = day,
                firstDayOfWeek = DayOfWeek.MONDAY,
                zone = zone,
            ).days.single { it.date == day }

        assertEquals(3L, activeDay.dominantProjectId)
    }

    @Test
    fun `single active day reaches full relative intensity`() {
        val zone = ZoneId.of("UTC")
        val day = LocalDate.of(2026, 4, 8)

        val activeDay =
            buildInsightsFabricModel(
                sessions = listOf(session(projectId = 1L, date = day, minutes = 10, zone = zone)),
                today = day,
                firstDayOfWeek = DayOfWeek.MONDAY,
                zone = zone,
            ).days.single { it.date == day }

        assertEquals(3, activeDay.level)
    }

    @Test
    fun `equal non-zero days all reach full relative intensity`() {
        val zone = ZoneId.of("UTC")
        val firstDay = LocalDate.of(2026, 4, 7)
        val sessions =
            listOf(
                session(projectId = 1L, date = firstDay, minutes = 10, zone = zone),
                session(projectId = 1L, date = firstDay.plusDays(1), minutes = 10, zone = zone),
            )

        val activeDays =
            buildInsightsFabricModel(
                sessions = sessions,
                today = firstDay.plusDays(1),
                firstDayOfWeek = DayOfWeek.MONDAY,
                zone = zone,
            ).days.filter { it.totalMinutes > 0 }

        assertEquals(listOf(3, 3), activeDays.map { it.level })
    }

    @Test
    fun `relative levels include both one-third and two-thirds boundaries`() {
        val zone = ZoneId.of("UTC")
        val firstDay = LocalDate.of(2026, 4, 6)
        val sessions =
            listOf(
                session(projectId = 1L, date = firstDay, minutes = 10, zone = zone),
                session(projectId = 1L, date = firstDay.plusDays(1), minutes = 11, zone = zone),
                session(projectId = 1L, date = firstDay.plusDays(2), minutes = 20, zone = zone),
                session(projectId = 1L, date = firstDay.plusDays(3), minutes = 21, zone = zone),
                session(projectId = 1L, date = firstDay.plusDays(4), minutes = 30, zone = zone),
            )

        val activeDays =
            buildInsightsFabricModel(
                sessions = sessions,
                today = firstDay.plusDays(4),
                firstDayOfWeek = DayOfWeek.MONDAY,
                zone = zone,
            ).days.filter { it.totalMinutes > 0 }

        assertEquals(listOf(10, 11, 20, 21, 30), activeDays.map { it.totalMinutes })
        assertEquals(listOf(1, 2, 2, 3, 3), activeDays.map { it.level })
    }

    @Test
    fun `window clamps old history to twenty-six aligned weeks`() {
        val zone = ZoneId.of("UTC")
        val today = LocalDate.of(2026, 8, 5)
        val oldSession =
            session(
                projectId = 1L,
                date = LocalDate.of(2025, 10, 29),
                minutes = 30,
                zone = zone,
            )

        val model =
            buildInsightsFabricModel(
                sessions = listOf(oldSession),
                today = today,
                firstDayOfWeek = DayOfWeek.MONDAY,
                zone = zone,
            )

        assertEquals(26, model.weekCount)
        assertEquals(LocalDate.of(2026, 2, 9), model.days.first().date)
        assertEquals(today, model.days.last().date)
        assertEquals(178, model.days.size)
        assertTrue(model.days.all { it.totalMinutes == 0 })
    }

    @Test
    fun `activity outside the clamped window does not reduce visible intensity`() {
        val zone = ZoneId.of("UTC")
        val today = LocalDate.of(2026, 8, 5)
        val sessions =
            listOf(
                session(
                    projectId = 1L,
                    date = LocalDate.of(2025, 10, 29),
                    minutes = 300,
                    zone = zone,
                ),
                session(projectId = 1L, date = today, minutes = 10, zone = zone),
            )

        val todayFabric =
            buildInsightsFabricModel(
                sessions = sessions,
                today = today,
                firstDayOfWeek = DayOfWeek.MONDAY,
                zone = zone,
            ).days.single { it.date == today }

        assertEquals(10, todayFabric.totalMinutes)
        assertEquals(3, todayFabric.level)
    }

    @Test
    fun `future sessions do not extend the day window beyond today`() {
        val zone = ZoneId.of("UTC")
        val today = LocalDate.of(2026, 8, 5)
        val futureSession =
            session(
                projectId = 1L,
                date = LocalDate.of(2026, 8, 10),
                minutes = 30,
                zone = zone,
            )

        val model =
            buildInsightsFabricModel(
                sessions = listOf(futureSession),
                today = today,
                firstDayOfWeek = DayOfWeek.MONDAY,
                zone = zone,
            )

        assertEquals(1, model.weekCount)
        assertEquals(LocalDate.of(2026, 8, 3), model.days.first().date)
        assertEquals(today, model.days.last().date)
        assertTrue(model.days.all { it.totalMinutes == 0 })
    }

    private fun session(
        projectId: Long,
        date: LocalDate,
        minutes: Int,
        rows: Int = minutes,
        zone: ZoneId,
    ): KnitSession {
        val startedAt =
            ZonedDateTime
                .of(date.year, date.monthValue, date.dayOfMonth, 12, 0, 0, 0, zone)
                .toInstant()
                .toEpochMilli()
        return KnitSession(
            projectId = projectId,
            startedAt = startedAt,
            endedAt = startedAt + minutes * 60_000L,
            startRow = 0,
            endRow = rows,
            durationMinutes = minutes,
            durationSeconds = minutes * 60L,
            rowsWorked = rows,
            zoneId = zone.id,
        )
    }

    private fun sessionSeconds(
        projectId: Long,
        date: LocalDate,
        seconds: Long,
        rows: Int,
        zone: ZoneId,
    ): KnitSession {
        val startedAt = instantMillis(date, hour = 12, minute = 0, zone = zone)
        return KnitSession(
            projectId = projectId,
            startedAt = startedAt,
            endedAt = startedAt + seconds * 1_000L,
            startRow = 0,
            endRow = rows,
            durationMinutes = 1,
            durationSeconds = seconds,
            rowsWorked = rows,
            zoneId = zone.id,
        )
    }

    private fun instantMillis(
        date: LocalDate,
        hour: Int,
        minute: Int,
        zone: ZoneId,
    ): Long =
        ZonedDateTime
            .of(date.year, date.monthValue, date.dayOfMonth, hour, minute, 0, 0, zone)
            .toInstant()
            .toEpochMilli()
}
