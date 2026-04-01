package com.finnvek.knittools.domain.model

data class CastOnResult(
    val stitches: Int,
    val actualWidth: Double,
    val adjustedDown: Int? = null,
    val adjustedUp: Int? = null,
    val adjustedDownWidth: Double? = null,
    val adjustedUpWidth: Double? = null,
)
