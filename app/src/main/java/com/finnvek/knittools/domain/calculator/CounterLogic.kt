package com.finnvek.knittools.domain.calculator

data class CounterState(
    val count: Int = 0,
    val previousCount: Int? = null,
    val stepSize: Int = 1,
)

object CounterLogic {

    fun increment(state: CounterState): CounterState =
        state.copy(
            count = state.count + state.stepSize,
            previousCount = state.count,
        )

    fun decrement(state: CounterState): CounterState =
        state.copy(
            count = maxOf(0, state.count - state.stepSize),
            previousCount = state.count,
        )

    fun undo(state: CounterState): CounterState =
        if (state.previousCount != null) {
            state.copy(count = state.previousCount, previousCount = null)
        } else {
            state
        }

    fun reset(state: CounterState): CounterState =
        state.copy(count = 0, previousCount = state.count)

    fun setStepSize(state: CounterState, stepSize: Int): CounterState =
        state.copy(stepSize = maxOf(1, stepSize))
}
