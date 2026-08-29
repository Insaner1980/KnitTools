package com.finnvek.knittools.ui.screens.counter

import androidx.lifecycle.SavedStateHandle
import com.finnvek.knittools.domain.calculator.MeasurementNumberError
import com.finnvek.knittools.domain.model.YarnUsageSource
import com.finnvek.knittools.domain.model.YarnUsageUnit

internal fun SavedStateHandle.saveYarnUsage(draft: YarnUsageDraft?) {
    this["yarnUsageProject"] = draft?.projectId
    if (draft == null) return
    this["yarnUsageCard"] = draft.source.yarnCardId
    this["yarnUsageNote"] = draft.source.projectYarnNoteId
    this["yarnUsageName"] = draft.name
    this["yarnUsageId"] = draft.usageId
    this["yarnUsageRevision"] = draft.revision
    this["yarnUsageUnit"] = draft.unit.name
    this["yarnUsagePendingUnit"] = draft.pendingUnit?.name
    this["yarnUsageConversion"] = draft.conversionEnabled
    YarnUsageField.entries.forEach { field ->
        val input = draft.input(field)
        this["yarnUsage${field}Text"] = input.text
        this["yarnUsage${field}Value"] = input.value
        this["yarnUsage${field}Error"] = input.error?.name
        this["yarnUsage${field}Incomplete"] = input.incomplete
    }
}

internal fun SavedStateHandle.restoreYarnUsage(): YarnUsageDraft? {
    val project = get<Long>("yarnUsageProject") ?: return null

    fun input(field: YarnUsageField): YarnUsageInput =
        YarnUsageInput(
            get<String>("yarnUsage${field}Text").orEmpty(),
            get("yarnUsage${field}Value"),
            get<String>("yarnUsage${field}Error")?.let(MeasurementNumberError::valueOf),
            get<Boolean>("yarnUsage${field}Incomplete") ?: false,
        )
    return YarnUsageDraft(
        projectId = project,
        source = YarnUsageSource(get("yarnUsageCard"), get("yarnUsageNote")),
        name = get<String>("yarnUsageName").orEmpty(),
        usageId = get("yarnUsageId"),
        revision = get<Long>("yarnUsageRevision") ?: 0,
        unit = get<String>("yarnUsageUnit")?.let(YarnUsageUnit::valueOf) ?: YarnUsageUnit.METERS,
        pendingUnit = get<String>("yarnUsagePendingUnit")?.let(YarnUsageUnit::valueOf),
        conversionEnabled = get<Boolean>("yarnUsageConversion") ?: false,
        planned = input(YarnUsageField.PLANNED),
        allocated = input(YarnUsageField.ALLOCATED),
        used = input(YarnUsageField.USED),
        length = input(YarnUsageField.LENGTH),
        weight = input(YarnUsageField.WEIGHT),
    )
}
