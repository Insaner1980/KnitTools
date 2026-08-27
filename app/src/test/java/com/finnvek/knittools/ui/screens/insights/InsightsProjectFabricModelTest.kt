package com.finnvek.knittools.ui.screens.insights

import com.finnvek.knittools.domain.model.KnitSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

class InsightsProjectFabricModelTest {
    @Test
    fun `window starts at the current week minus twenty five weeks`() {
        val today = LocalDate.of(2026, 8, 19)

        val model = buildModel(today = today, firstDayOfWeek = DayOfWeek.MONDAY)

        assertEquals(LocalDate.of(2026, 2, 23), model.startDate)
        assertEquals(today, model.endDate)
    }

    @Test
    fun `window follows the locale first day of week`() {
        val today = LocalDate.of(2026, 8, 19)

        val mondayModel = buildModel(today = today, firstDayOfWeek = DayOfWeek.MONDAY)
        val sundayModel = buildModel(today = today, firstDayOfWeek = DayOfWeek.SUNDAY)

        assertEquals(DayOfWeek.MONDAY, mondayModel.startDate.dayOfWeek)
        assertEquals(DayOfWeek.SUNDAY, sundayModel.startDate.dayOfWeek)
        assertEquals(
            today
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
                .minusWeeks(25),
            sundayModel.startDate,
        )
    }

    @Test
    fun `window includes elapsed days through today but no future days`() {
        val today = LocalDate.of(2026, 8, 19)

        val model = buildModel(today = today)

        assertEquals(model.startDate, model.days.first().date)
        assertEquals(today, model.days.last().date)
        assertTrue(model.days.none { it.date.isAfter(today) })
    }

    @Test
    fun `one day keeps every project`() {
        val day = LocalDate.of(2026, 8, 19)
        val sessions = (1L..5L).map { projectId -> session(projectId, day, 9) }

        val model = buildModel(today = day, sessions = sessions, projectOrder = (1L..5L).toList())

        assertEquals((1L..5L).toList(), model.days.single { it.date == day }.projectIds)
    }

    @Test
    fun `project count is never truncated`() {
        val day = LocalDate.of(2026, 8, 19)

        listOf(2, 3, 5, 8).forEach { count ->
            val ids = (1L..count.toLong()).toList()
            val model = buildModel(today = day, sessions = ids.map { session(it, day, 9) }, projectOrder = ids)

            assertEquals(ids, model.days.single { it.date == day }.projectIds)
        }
    }

    @Test
    fun `multiple sessions for one project produce one stripe`() {
        val day = LocalDate.of(2026, 8, 19)

        val model =
            buildModel(
                today = day,
                sessions = listOf(session(4L, day, 9), session(4L, day, 13)),
            )

        assertEquals(listOf(4L), model.days.single { it.date == day }.projectIds)
    }

    @Test
    fun `project order is independent of session order`() {
        val day = LocalDate.of(2026, 8, 19)
        val sessions = listOf(session(7L, day, 9), session(3L, day, 10), session(5L, day, 11))

        val model = buildModel(today = day, sessions = sessions, projectOrder = listOf(5L, 3L))

        assertEquals(listOf(5L, 3L, 7L), model.days.single { it.date == day }.projectIds)
    }

    @Test
    fun `session crossing midnight activates both days`() {
        val zone = ZoneId.of("Europe/Helsinki")
        val firstDay = LocalDate.of(2026, 8, 18)
        val start = firstDay.atTime(23, 30).atZone(zone)
        val end = firstDay.plusDays(1).atTime(0, 30).atZone(zone)

        val model = buildModel(today = firstDay.plusDays(1), sessions = listOf(session(9L, start, end)), zone = zone)

        assertEquals(listOf(9L), model.days.single { it.date == firstDay }.projectIds)
        assertEquals(listOf(9L), model.days.single { it.date == firstDay.plusDays(1) }.projectIds)
    }

    @Test
    fun `stored session zone controls the activity day`() {
        val fallbackZone = ZoneId.of("UTC")
        val storedZone = ZoneId.of("America/Los_Angeles")
        val start = ZonedDateTime.of(2026, 3, 2, 0, 30, 0, 0, fallbackZone)
        val end = start.plusMinutes(20)
        val session = session(6L, start, end).copy(zoneId = storedZone.id)

        val model = buildModel(today = LocalDate.of(2026, 3, 2), sessions = listOf(session), zone = fallbackZone)

        assertEquals(listOf(6L), model.days.single { it.date == LocalDate.of(2026, 3, 1) }.projectIds)
        assertTrue(
            model.days
                .single { it.date == LocalDate.of(2026, 3, 2) }
                .projectIds
                .isEmpty(),
        )
    }

    @Test
    fun `days outside the window are excluded after splitting`() {
        val today = LocalDate.of(2026, 8, 19)
        val startDate = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(25)

        val model =
            buildModel(
                today = today,
                sessions = listOf(session(1L, startDate.minusDays(1), 9), session(2L, startDate, 9)),
            )

        assertEquals(startDate, model.startDate)
        assertEquals(listOf(2L), model.days.first().projectIds)
        assertTrue(model.days.none { it.date.isBefore(startDate) })
    }

    @Test
    fun `window without positive tracked time returns null`() {
        val today = LocalDate.of(2026, 8, 19)
        val startedAt = session(1L, today, 9).startedAt
        val zeroDuration =
            session(1L, today, 9).copy(
                endedAt = startedAt,
                startRow = 0,
                endRow = 0,
                durationMinutes = 0,
                durationSeconds = 0,
                rowsWorked = 0,
            )

        assertNull(
            buildInsightsProjectFabric(
                sessions = listOf(zeroDuration, session(2L, today.minusWeeks(30), 9)),
                today = today,
                zone = TEST_ZONE,
                firstDayOfWeek = DayOfWeek.MONDAY,
                projectOrder = emptyList(),
            ),
        )
    }

    @Test
    fun `selected project data creates single colour active days`() {
        val today = LocalDate.of(2026, 8, 19)
        val sessions = listOf(session(12L, today.minusDays(1), 9), session(12L, today, 9))

        val model = buildModel(today = today, sessions = sessions, projectOrder = listOf(12L))

        assertEquals(2, model.activeDayCount)
        assertTrue(model.days.filter { it.projectIds.isNotEmpty() }.all { it.projectIds == listOf(12L) })
    }

    private fun buildModel(
        today: LocalDate,
        sessions: List<KnitSession> = listOf(session(1L, today, 9)),
        zone: ZoneId = TEST_ZONE,
        firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
        projectOrder: List<Long> = emptyList(),
    ): InsightsProjectFabricModel =
        requireNotNull(
            buildInsightsProjectFabric(
                sessions = sessions,
                today = today,
                zone = zone,
                firstDayOfWeek = firstDayOfWeek,
                projectOrder = projectOrder,
            ),
        )

    private fun session(
        projectId: Long,
        date: LocalDate,
        hour: Int,
    ): KnitSession {
        val start = date.atTime(hour, 0).atZone(TEST_ZONE)
        return session(projectId, start, start.plusMinutes(30))
    }

    private fun session(
        projectId: Long,
        start: ZonedDateTime,
        end: ZonedDateTime,
    ): KnitSession {
        val seconds = Duration.between(start, end).seconds
        return KnitSession(
            projectId = projectId,
            startedAt = start.toInstant().toEpochMilli(),
            endedAt = end.toInstant().toEpochMilli(),
            startRow = 0,
            endRow = 10,
            durationMinutes = (seconds / 60).toInt(),
            durationSeconds = seconds,
            rowsWorked = 10,
            zoneId = start.zone.id,
        )
    }

    private companion object {
        val TEST_ZONE: ZoneId = ZoneId.of("UTC")
    }
}
