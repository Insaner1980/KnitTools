package com.finnvek.knittools.domain.calculator

const val MAX_MAIN_COUNTER_STEP_SIZE = 99

fun parseMainCounterStepSize(text: String): Int? =
    text
        .trim()
        .takeIf { it.isNotEmpty() && it.all(Char::isDigit) }
        ?.toIntOrNull()
        ?.takeIf { it in 1..MAX_MAIN_COUNTER_STEP_SIZE }

data class CounterState(
    val count: Int = 0,
    val previousCount: Int? = null,
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
        return applyCountChange(state, currentCount, newCount, stepSize)
    }

    fun decrement(state: CounterState): CounterState {
        val currentCount = state.count.coerceAtLeast(0)
        val stepSize = state.stepSize.coerceAtLeast(1)
        val newCount = (currentCount.toLong() - stepSize.toLong()).coerceAtLeast(0L).toInt()
        return applyCountChange(state, currentCount, newCount, stepSize)
    }

    /** Rajaan osunut muutos ei muuta arvoa, joten se ei myöskään korvaa kumottavaa edellistä arvoa. */
    private fun applyCountChange(
        state: CounterState,
        currentCount: Int,
        newCount: Int,
        stepSize: Int,
    ): CounterState =
        if (newCount == currentCount) {
            state.copy(count = currentCount, stepSize = stepSize)
        } else {
            state.copy(
                count = newCount,
                previousCount = currentCount,
                stepSize = stepSize,
            )
        }

    fun undo(state: CounterState): CounterState =
        if (state.previousCount != null) {
            state.copy(count = state.previousCount, previousCount = null)
        } else {
            state
        }

    fun reset(state: CounterState): CounterState = state.copy(count = 0, previousCount = state.count)

    fun setStepSize(
        state: CounterState,
        stepSize: Int,
    ): CounterState = state.copy(stepSize = maxOf(1, stepSize))
}
