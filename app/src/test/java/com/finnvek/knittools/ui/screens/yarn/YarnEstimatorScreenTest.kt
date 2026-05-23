package com.finnvek.knittools.ui.screens.yarn

import com.finnvek.knittools.R
import com.finnvek.knittools.ui.components.skeinCountStringRes
import org.junit.Assert.assertEquals
import org.junit.Test

class YarnEstimatorScreenTest {
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
