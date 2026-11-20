package com.finnvek.knittools.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** PDF-lukijan palkit. Matalassa ikkunassa (vaakapuhelin) palkit tiivistyvät, jotta ohjetta näkyy enemmän. */
object PatternViewerDimens {
    const val HIGHLIGHTER_PALETTE_WINDOW_FRACTION = 0.4f
    val CompactTopBarHeight = 48.dp
    val BottomBarHorizontalPadding = 16.dp
    val BottomBarVerticalPadding = 10.dp
    val BottomBarButtonSize = 48.dp
    val BottomBarRowSpacing = 4.dp
    val BottomBarCountSpacing = 8.dp
    val LibraryBottomBarHorizontalPadding = 8.dp
    val LibraryBottomBarVerticalPadding = 6.dp
    val ReadingLineLabelGap = 4.dp
    val ReadingLineHandleSize = 48.dp
    val ReadingLineHandleRadius = 6.dp
    val ReadingLineMinimumBandHeight = 24.dp
    val DocumentStatusGap = 8.dp
    val DocumentInfoLineSpacing = 2.dp
    val DocumentWebsiteGap = 12.dp
    val CalibrationPanelElevation = 2.dp
    val CalibrationInstructionGap = 4.dp
    val CalibrationFieldGap = 12.dp
    val CalibrationActionGap = 8.dp

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
