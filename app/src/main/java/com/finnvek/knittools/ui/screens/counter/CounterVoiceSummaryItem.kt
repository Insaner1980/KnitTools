package com.finnvek.knittools.ui.screens.counter

data class CounterVoiceSummaryItem(
    val name: String,
    val count: Int,
)

fun counterVoiceSummaryItems(
    state: CounterUiState,
    secondaryCounterName: String,
): List<CounterVoiceSummaryItem> =
    buildList {
        if (state.canUseSecondaryCounter) {
            add(CounterVoiceSummaryItem(secondaryCounterName, state.secondaryCount))
        }
        state.projectCounters.forEach { counter ->
            add(CounterVoiceSummaryItem(counter.name, counter.count))
        }
    }
