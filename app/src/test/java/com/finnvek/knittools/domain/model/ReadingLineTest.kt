package com.finnvek.knittools.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ReadingLineTest {
    @Test
    fun `reading line fraction is clamped away from page edges`() {
        assertEquals(READING_LINE_MIN_Y_FRACTION, sanitizeReadingLineYFraction(-1f), 0.0f)
        assertEquals(0.42f, sanitizeReadingLineYFraction(0.42f), 0.0f)
        assertEquals(READING_LINE_MAX_Y_FRACTION, sanitizeReadingLineYFraction(1.5f), 0.0f)
    }
}
