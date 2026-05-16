package com.finnvek.knittools.domain.model

data class IncreaseDecreaseResult(
    val totalStitches: Int,
    val easyPattern: String,
    val balancedPattern: String,
    val isValid: Boolean,
    val errorMessage: String? = null,
    val message: IncreaseDecreaseMessage? = null,
)

enum class IncreaseDecreaseMode { INCREASE, DECREASE }

enum class KnittingStyle { FLAT, CIRCULAR }

sealed interface IncreaseDecreaseMessage {
    data object CurrentStitchesMustBePositive : IncreaseDecreaseMessage

    data object ChangeMustBePositive : IncreaseDecreaseMessage

    data class CannotDecreaseBy(
        val changeBy: Int,
        val currentStitches: Int,
    ) : IncreaseDecreaseMessage

    data class NotEnoughStitchesForDecrease(
        val changeBy: Int,
        val requiredStitches: Long,
    ) : IncreaseDecreaseMessage

    data object TotalStitchesOutOfRange : IncreaseDecreaseMessage

    data object IncreaseMoreThanCurrent : IncreaseDecreaseMessage
}
