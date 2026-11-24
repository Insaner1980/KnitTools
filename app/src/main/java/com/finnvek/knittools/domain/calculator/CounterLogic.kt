package com.finnvek.knittools.domain.calculator

const val MAX_MAIN_COUNTER_STEP_SIZE = 99

fun parseMainCounterStepSize(text: String): Int? =
    text
        .trim()
        .takeIf { it.isNotEmpty() && it.all(Char::isDigit) }
        ?.toIntOrNull()
        ?.takeIf { it in 1..MAX_MAIN_COUNTER_STEP_SIZE }

// Kumoaminen kulkee tallennetun laskurihistorian kautta (CounterRepository), ei muistinvaraisen tilan.
data class CounterState(
    val count: Int = 0,
    val stepSize: Int = 1,
)

object CounterLogic {
    fun increment(state: CounterState): CounterState {
        val currentCount = state.count.coerceAtLeast(0)
        val stepSize = state.stepSize.coerceAtLeast(1)
        val newCount =
            (currentCount.toLong() + stepSize.toLong())
                .coerceAtMost(Int.MAX_VALUE.toLong())
                .toInt()
        return state.copy(count = newCount, stepSize = stepSize)
    }

    fun decrement(state: CounterState): CounterState {
        val currentCount = state.count.coerceAtLeast(0)
        val stepSize = state.stepSize.coerceAtLeast(1)
        val newCount = (currentCount.toLong() - stepSize.toLong()).coerceAtLeast(0L).toInt()
        return state.copy(count = newCount, stepSize = stepSize)
    }

    fun reset(state: CounterState): CounterState = state.copy(count = 0)

    fun setStepSize(
        state: CounterState,
        stepSize: Int,
    ): CounterState = state.copy(stepSize = maxOf(1, stepSize))
}
