package com.finnvek.knittools.ui.screens.yarn

import com.finnvek.knittools.R
import com.finnvek.knittools.ui.components.skeinCountStringRes
import org.junit.Assert.assertEquals
import org.junit.Test

class YarnEstimatorScreenTest {
    @Test
    fun `pending yarn card meters are converted to yards for imperial calculator input`() {
        val values =
            pendingCalculatorInputValues(
                weightGrams = "100",
                lengthMeters = "200",
                useImperial = true,
            )

        assertEquals(Pair("100", "218.7"), values)
    }

    @Test
    fun `pending yarn card meters stay metric for metric calculator input`() {
        val values =
            pendingCalculatorInputValues(
                weightGrams = "100",
                lengthMeters = "200",
                useImperial = false,
            )

        assertEquals(Pair("100", "200"), values)
    }

    @Test
    fun `displayed skein estimate rounds up so whole skein result is not contradicted`() {
        assertEquals("2.01", formatSkeinsEstimateForDisplay(2.0001))
        assertEquals("2.25", formatSkeinsEstimateForDisplay(2.25))
        assertEquals("2.00", formatSkeinsEstimateForDisplay(2.0))
    }

    @Test
    fun `skein count result uses singular resource for one skein`() {
        assertEquals(R.string.skein_count_one, skeinCountStringRes(1))
        assertEquals(R.string.skein_count_many, skeinCountStringRes(2))
    }

    @Suppress("UNCHECKED_CAST")
    private fun pendingCalculatorInputValues(
        weightGrams: String,
        lengthMeters: String,
        useImperial: Boolean,
    ): Pair<String, String> =
        screenMethod(
            "pendingCalculatorInputValues",
            String::class.java,
            String::class.java,
            Boolean::class.javaPrimitiveType!!,
        ).invoke(null, weightGrams, lengthMeters, useImperial) as Pair<String, String>

    private fun formatSkeinsEstimateForDisplay(exactSkeins: Double): String =
        screenMethod("formatSkeinsEstimateForDisplay", Double::class.javaPrimitiveType!!)
            .invoke(null, exactSkeins) as String

    private fun screenMethod(
        name: String,
        vararg parameterTypes: Class<*>,
    ) = Class
        .forName("com.finnvek.knittools.ui.screens.yarn.YarnEstimatorScreenKt")
        .getDeclaredMethod(name, *parameterTypes)
        .apply { isAccessible = true }
}
