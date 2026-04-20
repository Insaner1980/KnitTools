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
    @StringRes val nameResId: Int,
    @StringRes val descriptionResId: Int,
    val category: ChartSymbolCategory,
)
