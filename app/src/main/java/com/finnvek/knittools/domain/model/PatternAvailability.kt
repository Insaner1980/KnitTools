package com.finnvek.knittools.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class PatternAvailability(
    val persistedValue: String,
) {
    @SerialName("free")
    Free("free"),

    @SerialName("paid")
    Paid("paid"),

    @SerialName("unknown")
    Unknown("unknown"),
    ;

    val isFree: Boolean
        get() = this == Free

    companion object {
        fun fromBackendValue(value: String?): PatternAvailability = fromStableValue(value)

        fun fromPersistedValue(value: String?): PatternAvailability = fromStableValue(value)

        private fun fromStableValue(value: String?): PatternAvailability =
            entries.firstOrNull { it.persistedValue == value } ?: Unknown
    }
}
