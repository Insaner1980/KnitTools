package com.finnvek.knittools.domain.model

enum class ChartSymbolCategory {
    BASIC,
    DECREASES,
    INCREASES,
    CABLES,
    OTHER,
}

data class ChartSymbol(
    val id: String,
    val name: String,
    val description: String,
    val category: ChartSymbolCategory,
)
