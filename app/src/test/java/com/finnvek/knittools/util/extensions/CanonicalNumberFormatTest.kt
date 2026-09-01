package com.finnvek.knittools.util.extensions

import org.junit.Assert.assertEquals
import org.junit.Test

class CanonicalNumberFormatTest {
    @Test
    fun `canonical decimal uses half-up rounding and fixed fraction digits`() {
        assertEquals("12.35", formatCanonicalDecimal(12.345, 2))
        assertEquals("12.00", formatCanonicalDecimal(12.0, 2))
    }
}
