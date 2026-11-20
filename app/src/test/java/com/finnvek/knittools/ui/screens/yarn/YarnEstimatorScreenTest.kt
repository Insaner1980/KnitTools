package com.finnvek.knittools.ui.screens.yarn

import com.finnvek.knittools.domain.model.YarnCard
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
        assertNull(calculate("0." + "0".repeat(307) + "1", largeFinite, "50"))
    }

    @Test
    fun `whole skeins omit the exact line and fractional skeins use one localized decimal`() {
        assertNull(exactSkeinsText(2.0, Locale.US))
        assertEquals("2.3", exactSkeinsText(2.31, Locale.US))
        assertEquals("2,3", exactSkeinsText(2.31, Locale.forLanguageTag("fi-FI")))
    }

    @Test
    fun `saved yarn prefill rounds lengths to whole units and preserves grams`() {
        val card = YarnCard(lengthMeters = "200", weightGrams = "100")
        assertEquals(YarnEstimatorSkeinFields("219", "100"), savedYarnSkeinFields(card, true, Locale.US))
        assertEquals(
            YarnEstimatorSkeinFields("241", "100"),
            savedYarnSkeinFields(card.copy(lengthMeters = "220"), true, Locale.US),
        )
        assertEquals(
            YarnEstimatorSkeinFields("221", "100"),
            savedYarnSkeinFields(card.copy(lengthMeters = "220.5"), false, Locale.US),
        )
        assertEquals(
            YarnEstimatorSkeinFields("", "100"),
            savedYarnSkeinFields(card.copy(lengthMeters = ""), false, Locale.US),
        )
        assertEquals(
            YarnEstimatorSkeinFields("1201", "100,5"),
            savedYarnSkeinFields(
                YarnCard(lengthMeters = "1200.5", weightGrams = "100.5"),
                false,
                Locale.forLanguageTag("fi"),
            ),
        )
        assertNull(savedYarnSkeinFields(YarnCard(), false, Locale.US))
    }

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
