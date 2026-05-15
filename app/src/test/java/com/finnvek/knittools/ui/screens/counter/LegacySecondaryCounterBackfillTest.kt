package com.finnvek.knittools.ui.screens.counter

import com.finnvek.knittools.domain.model.ProjectCounter
import org.junit.Assert.assertEquals
import org.junit.Test

class LegacySecondaryCounterBackfillTest {
    @Test
    fun `legacy secondary backfill row is omitted from visible project counters`() {
        val legacyBackfill =
            ProjectCounter(
                id = 1L,
                projectId = 7L,
                name = "Pattern repeat",
                count = 4,
                stepSize = 8,
                repeatAt = 8,
                sortOrder = 0,
                counterType = "REPEATING",
            )
        val userCounter =
            ProjectCounter(
                id = 2L,
                projectId = 7L,
                name = "Sleeve",
                count = 2,
            )

        val visibleCounters = withoutLegacySecondaryBackfillCopies(listOf(legacyBackfill, userCounter))

        assertEquals(listOf(userCounter), visibleCounters)
    }

    @Suppress("UNCHECKED_CAST")
    private fun withoutLegacySecondaryBackfillCopies(counters: List<ProjectCounter>): List<ProjectCounter> {
        val method =
            Class
                .forName("com.finnvek.knittools.ui.screens.counter.LegacySecondaryCounterBackfillKt")
                .getDeclaredMethod("withoutLegacySecondaryBackfillCopies", List::class.java)
                .apply { isAccessible = true }
        return method.invoke(null, counters) as List<ProjectCounter>
    }
}
