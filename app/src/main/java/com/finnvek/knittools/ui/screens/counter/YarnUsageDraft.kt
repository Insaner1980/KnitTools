package com.finnvek.knittools.ui.screens.counter

import com.finnvek.knittools.domain.calculator.MeasurementNumberError
import com.finnvek.knittools.domain.calculator.MeasurementNumberFormatter
import com.finnvek.knittools.domain.calculator.MeasurementNumberParser
import com.finnvek.knittools.domain.calculator.YarnUsageCalculator
import com.finnvek.knittools.domain.model.YarnUsageAmounts
import com.finnvek.knittools.domain.model.YarnUsageSource
import com.finnvek.knittools.domain.model.YarnUsageUnit
import java.util.Locale

enum class YarnUsageField { PLANNED, ALLOCATED, USED, LENGTH, WEIGHT }

data class YarnUsageInput(
    val text: String = "",
    val value: Double? = null,
    val error: MeasurementNumberError? = null,
    val incomplete: Boolean = false,
) {
    val valid: Boolean get() = text.isBlank() || (value != null && error == null && !incomplete)
}

data class YarnUsageDraft(
    val projectId: Long,
    val source: YarnUsageSource,
    val name: String,
    val usageId: Long? = null,
    val revision: Long = 0,
    val unit: YarnUsageUnit = YarnUsageUnit.METERS,
    val pendingUnit: YarnUsageUnit? = null,
    val planned: YarnUsageInput = YarnUsageInput(),
    val allocated: YarnUsageInput = YarnUsageInput(),
    val used: YarnUsageInput = YarnUsageInput(),
    val conversionEnabled: Boolean = false,
    val length: YarnUsageInput = YarnUsageInput(),
    val weight: YarnUsageInput = YarnUsageInput(),
) {
    val conversionValid: Boolean get() =
        conversionEnabled &&
            length.valid &&
            weight.valid &&
            YarnUsageCalculator.validConversion(length.value, weight.value)
    val amounts: YarnUsageAmounts get() =
        YarnUsageAmounts(
            planned.value,
            allocated.value,
            used.value,
            length.value.takeIf { conversionEnabled },
            weight.value.takeIf { conversionEnabled },
        )
    val canSave: Boolean get() =
        pendingUnit == null &&
            listOf(planned, allocated, used).all { it.valid } &&
            YarnUsageCalculator.validAmounts(amounts) &&
            (!conversionEnabled || conversionValid)
    val canSwitch: Boolean get() = listOf(planned, allocated, used).all { it.valid }
    val remaining: Double? get() =
        if (allocated.valid && used.valid) {
            YarnUsageCalculator.remaining(allocated.value, used.value)
        } else {
            null
        }

    fun input(field: YarnUsageField): YarnUsageInput =
        when (field) {
            YarnUsageField.PLANNED -> planned
            YarnUsageField.ALLOCATED -> allocated
            YarnUsageField.USED -> used
            YarnUsageField.LENGTH -> length
            YarnUsageField.WEIGHT -> weight
        }

    fun edit(
        field: YarnUsageField,
        text: String,
        locale: Locale,
    ): YarnUsageDraft {
        val ratio = field == YarnUsageField.LENGTH || field == YarnUsageField.WEIGHT
        val parsed = MeasurementNumberParser.parse(text, locale, allowZero = !ratio)
        val canonical =
            parsed.value?.let { value ->
                if (ratio) value else YarnUsageCalculator.toMeters(value, unit, length.value, weight.value)
            }
        val input =
            YarnUsageInput(
                text,
                canonical,
                parsed.error ?: MeasurementNumberError.TOO_LARGE.takeIf { parsed.value != null && canonical == null },
                parsed.incomplete && text.isNotBlank(),
            )
        val next =
            when (field) {
                YarnUsageField.PLANNED -> copy(planned = input)
                YarnUsageField.ALLOCATED -> copy(allocated = input)
                YarnUsageField.USED -> copy(used = input)
                YarnUsageField.LENGTH -> copy(length = input)
                YarnUsageField.WEIGHT -> copy(weight = input)
            }
        return if (ratio && next.conversionValid) next.displayIn(next.pendingUnit ?: next.unit) else next
    }

    fun switchUnit(next: YarnUsageUnit): YarnUsageDraft {
        if (!canSwitch) return this
        if (next in listOf(YarnUsageUnit.GRAMS, YarnUsageUnit.SKEINS) && !conversionValid) {
            return copy(pendingUnit = next, conversionEnabled = true)
        }
        return displayIn(next)
    }

    fun removeConversion(): YarnUsageDraft =
        displayIn(
            if (unit == YarnUsageUnit.GRAMS || unit == YarnUsageUnit.SKEINS) YarnUsageUnit.METERS else unit,
        ).copy(conversionEnabled = false, length = YarnUsageInput(), weight = YarnUsageInput(), pendingUnit = null)

    fun displayIn(next: YarnUsageUnit): YarnUsageDraft {
        fun converted(input: YarnUsageInput): YarnUsageInput {
            if (!input.valid || input.value == null) return input
            val display =
                YarnUsageCalculator.fromMeters(input.value, next, length.value, weight.value)
                    ?: return input.copy(error = MeasurementNumberError.TOO_LARGE)
            return input.copy(text = MeasurementNumberFormatter.formatEditing(display), error = null)
        }
        return copy(
            unit = next,
            pendingUnit = null,
            planned = converted(planned),
            allocated = converted(allocated),
            used = converted(used),
        )
    }
}
