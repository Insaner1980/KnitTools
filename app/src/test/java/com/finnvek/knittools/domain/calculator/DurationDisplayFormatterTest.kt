package com.finnvek.knittools.domain.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DurationDisplayFormatterTest {
    @Test
    fun `zero minutes stays in the minutes only shape`() {
        val display = DurationDisplayFormatter.fromMinutes(0)

        assertEquals(DurationShape.MINUTES_ONLY, display.shape)
        assertNull(display.hours)
        assertEquals(0, display.minutes)
    }

    @Test
    fun `under an hour reports minutes only`() {
        listOf(1, 59).forEach { minutes ->
            val display = DurationDisplayFormatter.fromMinutes(minutes)

            assertEquals(DurationShape.MINUTES_ONLY, display.shape)
            assertNull(display.hours)
            assertEquals(minutes, display.minutes)
        }
    }

    @Test
    fun `whole hours drop the minutes slot`() {
        mapOf(60 to 1, 120 to 2, 600 to 10).forEach { (minutes, hours) ->
            val display = DurationDisplayFormatter.fromMinutes(minutes)

            assertEquals(DurationShape.WHOLE_HOURS, display.shape)
            assertEquals(hours, display.hours)
            assertNull(display.minutes)
        }
    }

    @Test
    fun `hours and minutes split into both slots`() {
        mapOf(
            61 to (1 to 1),
            119 to (1 to 59),
            125 to (2 to 5),
            599 to (9 to 59),
        ).forEach { (minutes, expected) ->
            val display = DurationDisplayFormatter.fromMinutes(minutes)

            assertEquals(DurationShape.HOURS_AND_MINUTES, display.shape)
            assertEquals(expected.first, display.hours)
            assertEquals(expected.second, display.minutes)
        }
    }

    @Test
    fun `there is no upper bound special case`() {
        val display = DurationDisplayFormatter.fromMinutes(128 * 60 + 5)

        assertEquals(DurationShape.HOURS_AND_MINUTES, display.shape)
        assertEquals(128, display.hours)
        assertEquals(5, display.minutes)
    }

    @Test
    fun `negative input is treated as zero`() {
        val display = DurationDisplayFormatter.fromMinutes(-5)

        assertEquals(DurationShape.MINUTES_ONLY, display.shape)
        assertEquals(0, display.minutes)
    }
}
