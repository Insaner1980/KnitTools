package com.finnvek.knittools.domain.model

import android.content.Context
import androidx.annotation.StringRes

data class SizeMeasurement(
    val cm: Double,
    val inches: Double,
    val maxCm: Double = cm,
    val maxInches: Double = maxCm / 2.54,
)

sealed class SizeLabel {
    data class Resource(
        @param:StringRes val resId: Int,
        @param:StringRes val groupResId: Int? = null,
    ) : SizeLabel()

    fun resolve(context: Context): String =
        when (this) {
            is Resource -> {
                groupResId?.let { context.getString(it) + ": " + context.getString(resId) }
                    ?: context.getString(resId)
            }
        }
}

data class SizeChartEntry(
    val sizeLabel: SizeLabel,
    val measurements: List<SizeMeasurement>,
)
