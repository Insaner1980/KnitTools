package com.finnvek.knittools.domain.model

enum class CraftType(
    val persistedValue: String,
) {
    KNITTING("KNITTING"),
    CROCHET("CROCHET"),
    ;

    fun defaultMainCounterLabelType(): MainCounterLabelType =
        when (this) {
            KNITTING -> MainCounterLabelType.ROWS
            CROCHET -> MainCounterLabelType.ROUNDS
        }

    companion object {
        fun fromPersistedValue(value: String?): CraftType =
            entries.firstOrNull { it.persistedValue == value } ?: KNITTING
    }
}
