package com.finnvek.knittools.ui.components

import com.finnvek.knittools.domain.calculator.formatDecimalForDisplay
import com.finnvek.knittools.domain.calculator.formatIntegerForDisplay
import com.finnvek.knittools.domain.calculator.formatSignedDecimalForDisplay
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class LocaleFormattingTest {
    @Test
    fun `integer display uses locale grouping and digits`() {
        assertEquals("1.234", formatIntegerForDisplay(1_234, Locale.GERMANY))
    }

    @Test
    fun `decimal display uses locale separator and requested precision`() {
        assertEquals(
            "2,25",
            formatDecimalForDisplay(
                value = 2.25,
                locale = Locale.forLanguageTag("fi-FI"),
                minimumFractionDigits = 2,
                maximumFractionDigits = 2,
            ),
        )
    }

    @Test
    fun `canonical decimal input is shown with locale separator`() {
        assertEquals("12,5", localizeDecimalSeparatorForDisplay("12.5", Locale.forLanguageTag("fi-FI")))
    }

    @Test
    fun `signed decimal display keeps locale separator`() {
        assertEquals("+2,5", formatSignedDecimalForDisplay(2.5, Locale.forLanguageTag("fi-FI"), fractionDigits = 1))
    }

    @Test
    fun `uppercase display follows locale casing rules`() {
        assertEquals("İZMİR", "izmir".uppercaseForDisplay(Locale.forLanguageTag("tr-TR")))
    }
}
