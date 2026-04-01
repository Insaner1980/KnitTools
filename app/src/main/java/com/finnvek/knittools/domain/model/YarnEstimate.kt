package com.finnvek.knittools.domain.model

data class YarnEstimate(
    val skeinsNeeded: Int,
    val totalWeight: Double,
    val exactSkeins: Double,
)
