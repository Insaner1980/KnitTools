package com.finnvek.knittools.ui.theme

import androidx.compose.ui.unit.dp

/** Ommellun hintalapun mitat: ulkoreunan ja katkoviivatikkauksen väli sekä tikin pituus. */
object LaunchPriceTagDimens {
    val OuterInset = 5.dp
    val InnerPadding = 14.dp
    val StitchWidth = 1.5.dp
    val Stitch = DashedOutlineStyle(width = StitchWidth, dash = 6.dp, gap = 4.dp, corner = 12.dp)
}
