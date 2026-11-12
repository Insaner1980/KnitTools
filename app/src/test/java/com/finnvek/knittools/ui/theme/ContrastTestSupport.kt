package com.finnvek.knittools.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.pow

internal fun contrastRatio(
    foreground: Color,
    background: Color,
    alpha: Float = 1f,
): Double {
    val foregroundLuminance =
        relativeLuminance(
            foreground.red * alpha + background.red * (1f - alpha),
            foreground.green * alpha + background.green * (1f - alpha),
            foreground.blue * alpha + background.blue * (1f - alpha),
        )
    val backgroundLuminance = background.relativeLuminance()
    return (maxOf(foregroundLuminance, backgroundLuminance) + 0.05) /
        (minOf(foregroundLuminance, backgroundLuminance) + 0.05)
}

internal fun Color.relativeLuminance(): Double = relativeLuminance(red, green, blue)

private fun relativeLuminance(
    red: Float,
    green: Float,
    blue: Float,
): Double = 0.2126 * red.linearized() + 0.7152 * green.linearized() + 0.0722 * blue.linearized()

private fun Float.linearized(): Double {
    val component = toDouble()
    return if (component <= 0.04045) component / 12.92 else ((component + 0.055) / 1.055).pow(2.4)
}
