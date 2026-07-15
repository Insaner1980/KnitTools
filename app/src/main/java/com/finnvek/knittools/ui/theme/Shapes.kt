package com.finnvek.knittools.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val SheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)

val AppShapes =
    Shapes(
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(12.dp),
        large = RoundedCornerShape(16.dp),
        extraLarge = SheetShape,
    )
