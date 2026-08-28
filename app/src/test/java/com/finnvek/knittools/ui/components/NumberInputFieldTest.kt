package com.finnvek.knittools.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class NumberInputFieldTest {
    @Test
    fun `signed integer input preserves leading minus when allowed`() {
        assertEquals("-2", filterNumericInput("-2", allowNegative = true))
    }

    @Test
    fun `unsigned malformed input is preserved rather than changed into a different number`() {
        assertEquals("-2", filterNumericInput("-2", allowNegative = false))
        assertEquals("1e3", filterNumericInput("1e3", allowNegative = false))
        assertEquals("12.5", filterNumericInput("12.5", allowNegative = false))
    }

    private fun filterNumericInput(
        value: String,
        allowNegative: Boolean,
    ): String {
        val method =
            Class
                .forName("com.finnvek.knittools.ui.components.NumberInputFieldKt")
                .getDeclaredMethod(
                    "filterNumericInput",
                    String::class.java,
                    Boolean::class.javaPrimitiveType,
                    Boolean::class.javaPrimitiveType,
                )
        method.isAccessible = true
        return method.invoke(null, value, false, allowNegative) as String
    }
}
