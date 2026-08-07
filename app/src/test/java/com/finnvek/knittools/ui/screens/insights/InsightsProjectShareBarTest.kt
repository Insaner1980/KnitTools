package com.finnvek.knittools.ui.screens.insights

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class InsightsProjectShareBarTest {
    @Test
    fun `project share bar hides without a range total`() {
        assertEquals(
            0f,
            projectShareBarFraction(
                projectTotalMinutes = 45,
                rangeTotalMinutes = 0,
                containerWidth = 100.dp,
            ),
            0f,
        )
    }

    @Test
    fun `project share bar uses its actual share when it exceeds the visible minimum`() {
        assertEquals(
            0.5f,
            projectShareBarFraction(
                projectTotalMinutes = 50,
                rangeTotalMinutes = 100,
                containerWidth = 100.dp,
            ),
            0.0001f,
        )
    }

    @Test
    fun `project share bar keeps a positive small share at three dp`() {
        assertEquals(
            0.03f,
            projectShareBarFraction(
                projectTotalMinutes = 1,
                rangeTotalMinutes = 1_000,
                containerWidth = 100.dp,
            ),
            0.0001f,
        )
    }

    @Test
    fun `project share bar never exceeds its container`() {
        assertEquals(
            1f,
            projectShareBarFraction(
                projectTotalMinutes = 150,
                rangeTotalMinutes = 100,
                containerWidth = 2.dp,
            ),
            0f,
        )
    }
}
