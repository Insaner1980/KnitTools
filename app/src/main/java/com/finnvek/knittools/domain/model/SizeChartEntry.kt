package com.finnvek.knittools.domain.model

import android.content.Context
import androidx.annotation.StringRes

data class SizeMeasurement(
    val cm: Double,
    val inches: Double,
)

sealed class SizeLabel {
    data class Resource(
        @param:StringRes val resId: Int,
    ) : SizeLabel()

    fun resolve(context: Context): String =
        when (this) {
            is Resource -> context.getString(resId)
        }
}

data class SizeChartEntry(
    val sizeLabel: SizeLabel,
    val measurements: List<SizeMeasurement>,
)
