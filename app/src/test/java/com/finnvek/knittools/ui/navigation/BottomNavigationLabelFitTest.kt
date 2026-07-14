package com.finnvek.knittools.ui.navigation

import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BottomNavigationLabelFitTest {
    @Test
    fun `shared label size continues below preferred minimum until every label fits`() {
        val measuredSizes = mutableListOf<Float>()

        val result =
            sharedBottomNavigationLabelFontSize(
                maxFontSize = 11.sp,
                step = 0.5.sp,
                allLabelsFit = { candidate ->
                    measuredSizes += candidate.value
                    candidate <= 5.5.sp
                },
            )

        assertEquals(5.5.sp, result)
        assertTrue(8f in measuredSizes)
        assertTrue(5.5f in measuredSizes)
    }
}
