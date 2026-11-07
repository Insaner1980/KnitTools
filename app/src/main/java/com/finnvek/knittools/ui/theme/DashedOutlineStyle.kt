package com.finnvek.knittools.ui.theme

import androidx.compose.ui.unit.Dp

/** Katkoviivareunan mitat; hintalappu ja onboardingin And more -laatta käyttävät samaa piirtoa. */
data class DashedOutlineStyle(
    val width: Dp,
    val dash: Dp,
    val gap: Dp,
    val corner: Dp,
)
