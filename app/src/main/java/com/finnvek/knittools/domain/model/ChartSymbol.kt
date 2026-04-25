package com.finnvek.knittools.domain.model

import androidx.annotation.StringRes

enum class ChartSymbolCategory {
    BASIC,
    DECREASES,
    INCREASES,
    CABLES,
    OTHER,
}

data class ChartSymbol(
    val id: String,
    @param:StringRes val nameResId: Int,
    @param:StringRes val descriptionResId: Int,
    val category: ChartSymbolCategory,
)
