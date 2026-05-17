package com.finnvek.knittools.ui.screens.counter

import org.junit.Assert.assertEquals
import org.junit.Test

class SessionProgressTest {
    @Test
    fun `decrement removes counted row progress`() {
        val rowsWorked =
            SessionProgress.adjustRowsWorked(
                currentRowsWorked = 3,
                action = "decrement",
                previousValue = 12,
                newValue = 10,
            )

        assertEquals(1, rowsWorked)
    }

    @Test
    fun `reset cannot leave stale row progress`() {
        val rowsWorked =
            SessionProgress.adjustRowsWorked(
                currentRowsWorked = 4,
                action = "reset",
                previousValue = 20,
                newValue = 0,
            )

        assertEquals(0, rowsWorked)
    }

    @Test
    fun `undo only removes progress when it moves the row count backwards`() {
        val undoIncrementRows =
            SessionProgress.adjustRowsWorked(
                currentRowsWorked = 2,
                action = "undo",
                previousValue = 12,
                newValue = 11,
            )
        val undoDecrementRows =
            SessionProgress.adjustRowsWorked(
                currentRowsWorked = 0,
                action = "undo",
                previousValue = 9,
                newValue = 10,
            )

        assertEquals(1, undoIncrementRows)
        assertEquals(0, undoDecrementRows)
    }

    @Test
    fun `duration uses wall clock when timer has not ticked`() {
        val durationSeconds =
            SessionProgress.resolveDurationSeconds(
                recordedSeconds = 0L,
                startedAt = 1_000L,
                nowMillis = 3_500L,
            )

        assertEquals(2L, durationSeconds)
    }

    @Test
    fun `subsecond row progress has no synthetic duration`() {
        val durationSeconds =
            SessionProgress.resolveDurationSeconds(
                recordedSeconds = 0L,
                startedAt = 1_000L,
                nowMillis = 1_999L,
            )

        assertEquals(0L, durationSeconds)
    }
}
