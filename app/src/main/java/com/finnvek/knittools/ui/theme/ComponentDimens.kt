package com.finnvek.knittools.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

object ComponentDimens {
    val CompactSpacing = 4.dp
    val StandardSpacing = 8.dp
    val ContentSpacing = 12.dp
    val ContentPadding = 16.dp
    val LargeContentPadding = 24.dp
    val StandardIconSize = 20.dp
    val CompactProgressStrokeWidth = 2.dp
    val AnimatedResultOffset = 6.dp

    @Suppress("MayBeConstant")
    val AnimatedResultEnterDurationMillis = 200

    @Suppress("MayBeConstant")
    val AnimatedResultExitDurationMillis = 150

    val SegmentedContainerPadding = 6.dp
    val SegmentedItemTextInset = 6.dp
    val SegmentedItemMinHeight = 40.dp

    val ResultInsetPadding = 20.dp
    val ResultCardBorderWidth = 1.5.dp

    val FlatElevation = 0.dp

    // Näytön otsikko valitsimena (ScreenTitleSelector). Napautusalue tulee pehmusteesta.
    val TitleSelectorHorizontalPadding = 8.dp
    val TitleSelectorVerticalPadding = 6.dp
    val TitleSelectorIndicatorSize = 28.dp
    val TitleSelectorShape = RoundedCornerShape(percent = 50)

    // Yhteinen lisäys- ja muokkaussheet (FormSheet).
    val FormSheetHorizontalPadding = 20.dp
    val FormSheetBottomPadding = 32.dp
    val FormSheetItemSpacing = 12.dp
    val FormSheetActionMinHeight = 48.dp

    // Info-vihjeen ikoni nimikkeen vieressä (LabelWithInfo) ja IconButtonissa.
    val InfoIconSize = 18.dp

    // Tuloksen paikkamerkki (ResultPlaceholder): sama katkoviiva ja kulma kuin kuvan paikkamerkeissä.
    val ResultPlaceholderStrokeWidth = 1.5.dp
    val ResultPlaceholderDash = 6.dp
    val ResultPlaceholderCornerRadius = 16.dp
}
