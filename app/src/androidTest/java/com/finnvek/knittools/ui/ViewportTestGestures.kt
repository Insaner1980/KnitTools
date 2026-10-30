package com.finnvek.knittools.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.performTouchInput

internal fun SemanticsNodeInteraction.pinchZoomIn() =
    performTouchInput {
        down(0, center - Offset(40f, 0f))
        down(1, center + Offset(40f, 0f))
        moveTo(0, center - Offset(100f, 0f))
        moveTo(1, center + Offset(100f, 0f))
        up(0)
        up(1)
    }
