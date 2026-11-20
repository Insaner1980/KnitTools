package com.finnvek.knittools.ui.screens.pattern

import com.finnvek.knittools.domain.model.READING_LINE_ROW_STEP_FRACTION
import com.finnvek.knittools.domain.model.sanitizeReadingGuideFraction
import com.finnvek.knittools.domain.model.sanitizeReadingLineYFraction

internal fun horizontalReadingLineAccessibilityStep(
    currentFraction: Float,
    forward: Boolean,
): Float =
    sanitizeReadingLineYFraction(
        currentFraction + if (forward) READING_LINE_ROW_STEP_FRACTION else -READING_LINE_ROW_STEP_FRACTION,
    )

internal fun verticalReadingGuideAccessibilityStep(
    currentFraction: Float,
    forward: Boolean,
): Float =
    sanitizeReadingGuideFraction(
        currentFraction + if (forward) READING_LINE_ROW_STEP_FRACTION else -READING_LINE_ROW_STEP_FRACTION,
    )
