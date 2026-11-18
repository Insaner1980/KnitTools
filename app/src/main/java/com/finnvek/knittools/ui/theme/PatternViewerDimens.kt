package com.finnvek.knittools.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** PDF-lukijan palkit. Matalassa ikkunassa (vaakapuhelin) palkit tiivistyvät, jotta ohjetta näkyy enemmän. */
object PatternViewerDimens {
    val CompactTopBarHeight = 48.dp
    val BottomBarHorizontalPadding = 16.dp
    val BottomBarVerticalPadding = 10.dp
    val LibraryBottomBarHorizontalPadding = 8.dp
    val LibraryBottomBarVerticalPadding = 6.dp

    /** Matalassa ikkunassa painikkeiden 48 dp:n kosketusalue riittää palkin korkeudeksi. */
    fun bottomBarVerticalPadding(
        compactHeight: Boolean,
        library: Boolean,
    ): Dp =
        when {
            compactHeight -> 0.dp
            library -> LibraryBottomBarVerticalPadding
            else -> BottomBarVerticalPadding
        }
}
