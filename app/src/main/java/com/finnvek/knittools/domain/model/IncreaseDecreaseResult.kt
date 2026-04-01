package com.finnvek.knittools.domain.model

data class IncreaseDecreaseResult(
    val totalStitches: Int,
    val easyPattern: String,
    val balancedPattern: String,
    val isValid: Boolean,
    val errorMessage: String? = null,
)

enum class IncreaseDecreaseMode { INCREASE, DECREASE }

enum class KnittingStyle { FLAT, CIRCULAR }
