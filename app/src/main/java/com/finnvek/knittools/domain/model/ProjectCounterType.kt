package com.finnvek.knittools.domain.model

enum class ProjectCounterType(
    val persistedValue: String,
) {
    COUNT_UP("COUNT_UP"),
    REPEATING("REPEATING"),
    SHAPING("SHAPING"),
    REPEAT_SECTION("REPEAT_SECTION"),
    ;

    companion object {
        fun fromPersistedValue(value: String?): ProjectCounterType =
            entries.firstOrNull { it.persistedValue == value } ?: COUNT_UP
    }
}
