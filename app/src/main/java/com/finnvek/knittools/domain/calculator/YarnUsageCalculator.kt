package com.finnvek.knittools.domain.calculator

import com.finnvek.knittools.domain.model.MeasurementUnit
import com.finnvek.knittools.domain.model.YarnUsageAmounts
import com.finnvek.knittools.domain.model.YarnUsageUnit

object YarnUsageCalculator {
    fun validConversion(
        metersPerSkein: Double?,
        gramsPerSkein: Double?,
    ): Boolean =
        metersPerSkein != null &&
            gramsPerSkein != null &&
            metersPerSkein.isFinite() &&
            gramsPerSkein.isFinite() &&
            metersPerSkein > 0.0 &&
            gramsPerSkein > 0.0

    fun validAmounts(amounts: YarnUsageAmounts): Boolean {
        val values = listOf(amounts.plannedMeters, amounts.allocatedMeters, amounts.usedMeters)
        return values.any { it != null } && values.all { it == null || (it.isFinite() && it >= 0.0) }
    }

    // CPD-OFF: Vastakkaissuuntaiset yksikkomuunnokset pidetaan eksplisiittisina ja rinnakkaisina.
    fun toMeters(
        value: Double,
        unit: YarnUsageUnit,
        metersPerSkein: Double?,
        gramsPerSkein: Double?,
    ): Double? {
        if (!value.isFinite() || value < 0.0) return null
        val result =
            when (unit) {
                YarnUsageUnit.METERS -> value
                YarnUsageUnit.YARDS -> MeasurementCalculator.convert(value, MeasurementUnit.YARD, MeasurementUnit.METER)
                YarnUsageUnit.GRAMS ->
                    if (validConversion(metersPerSkein, gramsPerSkein)) {
                        value / requireNotNull(gramsPerSkein) * requireNotNull(metersPerSkein)
                    } else {
                        null
                    }
                YarnUsageUnit.SKEINS ->
                    if (validConversion(metersPerSkein, gramsPerSkein)) {
                        value * requireNotNull(metersPerSkein)
                    } else {
                        null
                    }
            }
        return result?.takeIf { it.isFinite() && (it > 0.0 || value == 0.0) }
    }

    fun fromMeters(
        value: Double,
        unit: YarnUsageUnit,
        metersPerSkein: Double?,
        gramsPerSkein: Double?,
    ): Double? {
        if (!value.isFinite() || value < 0.0) return null
        val result =
            when (unit) {
                YarnUsageUnit.METERS -> value
                YarnUsageUnit.YARDS -> MeasurementCalculator.convert(value, MeasurementUnit.METER, MeasurementUnit.YARD)
                YarnUsageUnit.GRAMS ->
                    if (validConversion(metersPerSkein, gramsPerSkein)) {
                        value / requireNotNull(metersPerSkein) * requireNotNull(gramsPerSkein)
                    } else {
                        null
                    }
                YarnUsageUnit.SKEINS ->
                    if (validConversion(metersPerSkein, gramsPerSkein)) {
                        value / requireNotNull(metersPerSkein)
                    } else {
                        null
                    }
            }
        return result?.takeIf { it.isFinite() && (it > 0.0 || value == 0.0) }
    }
    // CPD-ON

    fun remaining(
        allocatedMeters: Double?,
        usedMeters: Double?,
    ): Double? {
        if (allocatedMeters == null || usedMeters == null) return null
        if (!allocatedMeters.isFinite() || !usedMeters.isFinite()) return null
        if (allocatedMeters < 0.0 || usedMeters < 0.0) return null
        return allocatedMeters - usedMeters
    }
}
