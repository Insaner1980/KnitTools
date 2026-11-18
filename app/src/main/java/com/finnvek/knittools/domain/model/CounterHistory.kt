package com.finnvek.knittools.domain.model

data class CounterHistory(
    val id: Long,
    val action: CounterHistoryAction,
    val previousValue: Int,
    val newValue: Int,
    val timestamp: Long,
)

enum class CounterHistoryAction {
    INCREASE,
    DECREASE,
    RESET,
    CHANGED,
    ;

    companion object {
        fun fromPersistedValue(value: String): CounterHistoryAction =
            when (value) {
                "increment" -> INCREASE
                "decrement" -> DECREASE
                "reset" -> RESET
                else -> CHANGED
            }
    }
}
