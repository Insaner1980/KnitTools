package com.finnvek.knittools.domain.calculator

import com.finnvek.knittools.data.local.RowReminderEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderLogicTest {
    private fun oneTime(
        targetRow: Int,
        isCompleted: Boolean = false,
    ) = RowReminderEntity(
        id = 1,
        projectId = 1,
        targetRow = targetRow,
        repeatInterval = null,
        message = "Test",
        isCompleted = isCompleted,
    )

    private fun repeating(
        targetRow: Int,
        interval: Int,
    ) = RowReminderEntity(
        id = 2,
        projectId = 1,
        targetRow = targetRow,
        repeatInterval = interval,
        message = "Test",
    )

    // Kertaluonteiset
    @Test
    fun `one-time triggers at exact target row`() {
        assertTrue(ReminderLogic.shouldTrigger(oneTime(12), 12))
    }

    @Test
    fun `one-time does not trigger before target row`() {
        assertFalse(ReminderLogic.shouldTrigger(oneTime(12), 11))
    }

    @Test
    fun `one-time does not trigger after target row`() {
        assertFalse(ReminderLogic.shouldTrigger(oneTime(12), 13))
    }

    @Test
    fun `one-time does not trigger when completed`() {
        assertFalse(ReminderLogic.shouldTrigger(oneTime(12, isCompleted = true), 12))
    }

    @Test
    fun `one-time does not trigger at row 0`() {
        assertFalse(ReminderLogic.shouldTrigger(oneTime(0), 0))
    }

    // Toistuvat
    @Test
    fun `repeating triggers at target row`() {
        assertTrue(ReminderLogic.shouldTrigger(repeating(8, 8), 8))
    }

    @Test
    fun `repeating triggers at first repeat`() {
        assertTrue(ReminderLogic.shouldTrigger(repeating(8, 8), 16))
    }

    @Test
    fun `repeating triggers at second repeat`() {
        assertTrue(ReminderLogic.shouldTrigger(repeating(8, 8), 24))
    }

    @Test
    fun `repeating does not trigger between repeats`() {
        assertFalse(ReminderLogic.shouldTrigger(repeating(8, 8), 10))
    }

    @Test
    fun `repeating does not trigger before target row`() {
        assertFalse(ReminderLogic.shouldTrigger(repeating(8, 8), 4))
    }

    @Test
    fun `repeating with interval 1 triggers every row from target`() {
        assertTrue(ReminderLogic.shouldTrigger(repeating(5, 1), 5))
        assertTrue(ReminderLogic.shouldTrigger(repeating(5, 1), 6))
        assertTrue(ReminderLogic.shouldTrigger(repeating(5, 1), 100))
    }

    @Test
    fun `repeating does not trigger at row 0`() {
        assertFalse(ReminderLogic.shouldTrigger(repeating(0, 8), 0))
    }

    // repeatCount
    @Test
    fun `repeatCount is 1 at target row`() {
        assertEquals(1, ReminderLogic.repeatCount(repeating(8, 8), 8))
    }

    @Test
    fun `repeatCount is 2 at first repeat`() {
        assertEquals(2, ReminderLogic.repeatCount(repeating(8, 8), 16))
    }

    @Test
    fun `repeatCount is 3 at second repeat`() {
        assertEquals(3, ReminderLogic.repeatCount(repeating(8, 8), 24))
    }

    @Test
    fun `repeatCount is 0 for one-time reminder`() {
        assertEquals(0, ReminderLogic.repeatCount(oneTime(12), 12))
    }

    @Test
    fun `repeatCount is 0 before target row`() {
        assertEquals(0, ReminderLogic.repeatCount(repeating(8, 8), 4))
    }

    // activeReminders
    @Test
    fun `activeReminders filters matching reminders`() {
        val reminders =
            listOf(
                oneTime(10),
                repeating(8, 8),
                oneTime(20),
            )
        val active = ReminderLogic.activeReminders(reminders, 10)
        assertEquals(1, active.size)
        assertEquals(10, active[0].targetRow)
    }

    @Test
    fun `activeReminders returns multiple matches`() {
        val reminders =
            listOf(
                oneTime(16),
                repeating(8, 8),
            )
        val active = ReminderLogic.activeReminders(reminders, 16)
        assertEquals(2, active.size)
    }

    @Test
    fun `activeReminders returns empty for no matches`() {
        val reminders = listOf(oneTime(10), oneTime(20))
        assertTrue(ReminderLogic.activeReminders(reminders, 15).isEmpty())
    }
}
