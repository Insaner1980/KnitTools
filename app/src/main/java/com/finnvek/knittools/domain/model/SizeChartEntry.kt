package com.finnvek.knittools.domain.model

import android.content.Context
import androidx.annotation.StringRes

data class SizeMeasurement(
    val cm: Double,
    val inches: Double,
)

sealed class SizeLabel {
    data class Literal(
        val text: String,
    ) : SizeLabel()

    data class Resource(
        @StringRes val resId: Int,
    ) : SizeLabel()

    fun resolve(context: Context): String =
        when (this) {
            is Literal -> text
            is Resource -> context.getString(resId)
        }
}

data class SizeChartEntry(
    val sizeLabel: SizeLabel,
    val measurements: List<SizeMeasurement>,
)
