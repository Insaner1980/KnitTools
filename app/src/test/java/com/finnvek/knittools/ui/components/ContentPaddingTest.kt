package com.finnvek.knittools.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class ContentPaddingTest {
    @Test
    fun `extra bottom preserves existing physical padding in both layout directions`() {
        val original = PaddingValues(start = 3.dp, top = 5.dp, end = 7.dp, bottom = 11.dp)
        LayoutDirection.entries.forEach { direction ->
            val result = original.withExtraBottom(13.dp, direction)
            assertEquals(original.calculateLeftPadding(direction), result.calculateLeftPadding(LayoutDirection.Ltr))
            assertEquals(original.calculateRightPadding(direction), result.calculateRightPadding(LayoutDirection.Ltr))
            assertEquals(5.dp, result.calculateTopPadding())
            assertEquals(24.dp, result.calculateBottomPadding())
        }
    }
}
