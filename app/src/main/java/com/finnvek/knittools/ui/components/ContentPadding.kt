package com.finnvek.knittools.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

internal fun PaddingValues.withExtraBottom(
    extraBottom: Dp,
    layoutDirection: LayoutDirection,
): PaddingValues =
    PaddingValues(
        start = calculateLeftPadding(layoutDirection),
        top = calculateTopPadding(),
        end = calculateRightPadding(layoutDirection),
        bottom = calculateBottomPadding() + extraBottom,
    )
