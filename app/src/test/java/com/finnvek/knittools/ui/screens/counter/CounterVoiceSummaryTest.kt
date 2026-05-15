package com.finnvek.knittools.ui.screens.counter

import com.finnvek.knittools.domain.model.ProjectCounter
import org.junit.Assert.assertEquals
import org.junit.Test

class CounterVoiceSummaryTest {
    @Test
    fun `pro secondary counter is included in voice counter summary`() {
        val state =
            CounterUiState(
                isPro = true,
                secondaryCount = 4,
                projectCounters =
                    listOf(
                        ProjectCounter(
                            projectId = 1L,
                            name = "Sleeve",
                            count = 2,
                        ),
                    ),
            )

        val items = counterVoiceSummaryItems(state, secondaryCounterName = "Secondary counter")

        assertEquals(listOf("Secondary counter:4", "Sleeve:2"), items)
    }

    @Test
    fun `free secondary counter is omitted from voice counter summary`() {
        val state = CounterUiState(isPro = false, secondaryCount = 4)

        val items = counterVoiceSummaryItems(state, secondaryCounterName = "Secondary counter")

        assertEquals(emptyList<String>(), items)
    }

    @Suppress("UNCHECKED_CAST")
    private fun counterVoiceSummaryItems(
        state: CounterUiState,
        secondaryCounterName: String,
    ): List<String> {
        val method =
            Class
                .forName("com.finnvek.knittools.ui.screens.counter.CounterVoiceSummaryItemKt")
                .getDeclaredMethod(
                    "counterVoiceSummaryItems",
                    CounterUiState::class.java,
                    String::class.java,
                ).apply { isAccessible = true }
        val rawItems = method.invoke(null, state, secondaryCounterName) as List<Any>
        return rawItems.map { item ->
            val name = item.javaClass.getDeclaredMethod("getName").invoke(item)
            val count = item.javaClass.getDeclaredMethod("getCount").invoke(item)
            "$name:$count"
        }
    }
}
