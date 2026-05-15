package com.finnvek.knittools.ui.screens.counter

import com.finnvek.knittools.domain.model.ProjectCounter

private const val LEGACY_SECONDARY_BACKFILL_NAME = "Pattern repeat"
private const val REPEATING_COUNTER_TYPE = "REPEATING"

fun withoutLegacySecondaryBackfillCopies(counters: List<ProjectCounter>): List<ProjectCounter> =
    counters.filterNot { counter -> counter.isLegacySecondaryBackfillCopy() }

private fun ProjectCounter.isLegacySecondaryBackfillCopy(): Boolean =
    name == LEGACY_SECONDARY_BACKFILL_NAME &&
        counterType == REPEATING_COUNTER_TYPE &&
        sortOrder == 0 &&
        repeatAt != null &&
        repeatAt == stepSize &&
        startingStitches == null &&
        stitchChange == null &&
        shapeEveryN == null &&
        repeatStartRow == null &&
        repeatEndRow == null &&
        totalRepeats == null &&
        currentRepeat == null
