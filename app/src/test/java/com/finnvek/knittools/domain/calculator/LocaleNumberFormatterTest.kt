package com.finnvek.knittools.domain.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class LocaleNumberFormatterTest {
    @Test
    fun `percent uses no decimals in english`() {
        assertEquals("33%", formatPercentForDisplay(0.3312, Locale.US))
        assertEquals("34%", formatPercentForDisplay(0.336, Locale.US))
        assertEquals("0%", formatPercentForDisplay(0.0, Locale.US))
        assertEquals("100%", formatPercentForDisplay(1.0, Locale.US))
    }

    @Test
    fun `percent follows locale conventions`() {
        val finnish = formatPercentForDisplay(0.3312, Locale.forLanguageTag("fi"))

        assertTrue("Finnish percent should start with the number: $finnish", finnish.startsWith("33"))
        assertTrue("Finnish percent should end with the sign: $finnish", finnish.endsWith("%"))
    }
}
