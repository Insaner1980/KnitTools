package com.finnvek.knittools.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

internal fun PaddingValues.withExtraBottom(
    extraBottom: Dp,
    layoutDirection: LayoutDirection,
): PaddingValues =
    PaddingValues.Absolute(
        left = calculateLeftPadding(layoutDirection),
        top = calculateTopPadding(),
        right = calculateRightPadding(layoutDirection),
        bottom = calculateBottomPadding() + extraBottom,
    )
