package com.finnvek.knittools.ui.screens.counter

import com.finnvek.knittools.domain.model.ProjectCounter
import com.finnvek.knittools.domain.model.ProjectCounterType

private const val LEGACY_SECONDARY_BACKFILL_NAME = "Pattern repeat"

fun withoutLegacySecondaryBackfillCopies(counters: List<ProjectCounter>): List<ProjectCounter> =
    counters.filterNot { counter -> counter.isLegacySecondaryBackfillCopy() }

private fun ProjectCounter.isLegacySecondaryBackfillCopy(): Boolean =
    name == LEGACY_SECONDARY_BACKFILL_NAME &&
        counterType == ProjectCounterType.REPEATING &&
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
