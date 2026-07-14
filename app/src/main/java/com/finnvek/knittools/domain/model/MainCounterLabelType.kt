package com.finnvek.knittools.domain.model

const val MAIN_COUNTER_CUSTOM_LABEL_MAX_LENGTH = 32

enum class MainCounterLabelType(
    val persistedValue: String,
) {
    ROWS("ROWS"),
    ROUNDS("ROUNDS"),
    REPEATS("REPEATS"),
    CUSTOM("CUSTOM"),
    ;

    companion object {
        fun fromPersistedValue(value: String?): MainCounterLabelType =
            entries.firstOrNull { it.persistedValue == value } ?: ROWS
    }
}

fun sanitizeMainCounterCustomLabel(label: String?): String? =
    label
        ?.trim()
        ?.take(MAIN_COUNTER_CUSTOM_LABEL_MAX_LENGTH)
        ?.takeIf { it.isNotEmpty() }

fun resolvedMainCounterLabelType(
    craftType: CraftType,
    labelType: MainCounterLabelType,
    customLabel: String?,
): MainCounterLabelType =
    if (labelType == MainCounterLabelType.CUSTOM && sanitizeMainCounterCustomLabel(customLabel) == null) {
        craftType.defaultMainCounterLabelType()
    } else {
        labelType
    }
