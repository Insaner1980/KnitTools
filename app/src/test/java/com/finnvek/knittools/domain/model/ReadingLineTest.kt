package com.finnvek.knittools.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ReadingLineTest {
    @Test
    fun `non-finite guide positions fall back to the center`() {
        assertEquals(DEFAULT_READING_LINE_Y_FRACTION, sanitizeReadingLineYFraction(Float.NaN))
        assertEquals(DEFAULT_READING_LINE_Y_FRACTION, sanitizeReadingLineYFraction(Float.POSITIVE_INFINITY))
        assertEquals(DEFAULT_READING_GUIDE_FRACTION, sanitizeReadingGuideFraction(Float.NEGATIVE_INFINITY))
    }

    @Test
    fun `reading line fraction is clamped away from page edges`() {
        assertEquals(READING_LINE_MIN_Y_FRACTION, sanitizeReadingLineYFraction(-1f), 0.0f)
        assertEquals(0.42f, sanitizeReadingLineYFraction(0.42f), 0.0f)
        assertEquals(READING_LINE_MAX_Y_FRACTION, sanitizeReadingLineYFraction(1.5f), 0.0f)
    }

    @Test
    fun `reading line advances by one row step for row delta`() {
        assertEquals(
            0.52f,
            advanceReadingLineForRowDelta(
                yFraction = 0.5f,
                rowDelta = 1,
            ),
            0.0f,
        )
    }
}
