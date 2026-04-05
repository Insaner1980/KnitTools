package com.finnvek.knittools.domain.model

data class KnittingAbbreviation(
    val abbreviation: String,
    val meaning: String,
    val description: String = "",
)
