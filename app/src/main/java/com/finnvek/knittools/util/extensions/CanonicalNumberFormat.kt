package com.finnvek.knittools.util.extensions

import java.math.BigDecimal
import java.math.RoundingMode

fun formatCanonicalDecimal(
    value: Double,
    fractionDigits: Int,
): String =
    BigDecimal
        .valueOf(value)
        .setScale(fractionDigits, RoundingMode.HALF_UP)
        .toPlainString()
