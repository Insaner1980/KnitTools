package com.finnvek.knittools.domain.model

const val DEFAULT_READING_LINE_Y_FRACTION = 0.5f
const val READING_LINE_MIN_Y_FRACTION = 0.05f
const val READING_LINE_MAX_Y_FRACTION = 0.95f

fun sanitizeReadingLineYFraction(yFraction: Float): Float =
    yFraction.coerceIn(READING_LINE_MIN_Y_FRACTION, READING_LINE_MAX_Y_FRACTION)
