package com.finnvek.knittools.ui.screens.yarn

import com.finnvek.knittools.domain.model.YarnEstimate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class YarnEstimatorScreenTest {
    @Test
    fun `localized input retains exact and rounded estimates`() {
        assertEquals(YarnEstimate(2, 200.0, 2.0), calculate("400", "200", "100"))
        assertEquals(YarnEstimate(3, 300.0, 2.25), calculate("450", "200", "100"))
        assertEquals(
            YarnEstimate(3, 301.5, 2.25),
            calculate("450,0", "200,0", "100,5", Locale.forLanguageTag("fi-FI")),
        )
    }

    @Test
    fun `invalid and incomplete text keeps result absent`() {
        for (invalid in listOf("", "-", "1.", "text", "0", "-1", "NaN", "Infinity", "9".repeat(310))) {
            assertNull(calculate(invalid, "200", "100"))
            assertNull(calculate("400", invalid, "100"))
            assertNull(calculate("400", "200", invalid))
        }
    }

    @Test
    fun `parsed calculation overflow is rejected with integer boundary preserved`() {
        assertEquals(
            YarnEstimate(Int.MAX_VALUE, Int.MAX_VALUE.toDouble(), Int.MAX_VALUE.toDouble()),
            calculate("2147483647", "1", "1"),
        )
        assertNull(calculate("2147483647.25", "1", "1"))
        val largeFinite = "1" + "0".repeat(308)
        assertNull(calculate(largeFinite, "0." + "0".repeat(307) + "1", "1"))
        assertEquals(YarnEstimate(1, 1e308, 1.0), calculate("1", "1", largeFinite))
        assertNull(calculate("2", "1", largeFinite))
    }

    @Test
    fun `displayed skein estimate rounds up so whole skein result is not contradicted`() {
        assertEquals("2.01", formatSkeinsEstimateForDisplay(2.0001, Locale.US))
        assertEquals("2.25", formatSkeinsEstimateForDisplay(2.25, Locale.US))
        assertEquals("2.00", formatSkeinsEstimateForDisplay(2.0, Locale.US))
        assertEquals("2,25", formatSkeinsEstimateForDisplay(2.25, Locale.forLanguageTag("fi-FI")))
    }

    private fun formatSkeinsEstimateForDisplay(
        exactSkeins: Double,
        locale: Locale,
    ): String =
        screenMethod("formatSkeinsEstimateForDisplay", Double::class.javaPrimitiveType!!, Locale::class.java)
            .invoke(null, exactSkeins, locale) as String

    private fun calculate(
        total: String,
        perSkein: String,
        weight: String,
        locale: Locale = Locale.US,
    ): YarnEstimate? =
        screenMethod(
            "calculateYarnEstimate",
            String::class.java,
            String::class.java,
            String::class.java,
            Locale::class.java,
        ).invoke(null, total, perSkein, weight, locale) as YarnEstimate?

    private fun screenMethod(
        name: String,
        vararg parameterTypes: Class<*>,
    ) = Class
        .forName("com.finnvek.knittools.ui.screens.yarn.YarnEstimatorScreenKt")
        .getDeclaredMethod(name, *parameterTypes)
        .apply { isAccessible = true }
}
