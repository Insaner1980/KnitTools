package com.finnvek.knittools.ui.screens.pattern

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun RowHighlightOverlay(
    yPosition: Float?,
    modifier: Modifier = Modifier,
    highlightColor: Color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
    accessibilityDescription: String? = null,
) {
    val semanticsModifier =
        if (accessibilityDescription != null) {
            modifier.semantics { contentDescription = accessibilityDescription }
        } else {
            modifier
        }
    Canvas(modifier = semanticsModifier) {
        val markerY = yPosition ?: return@Canvas
        val centerY = size.height * markerY.coerceIn(0f, 1f)
        val bandHeight = (size.height * 0.04f).coerceAtLeast(18.dp.toPx())
        drawRect(
            color = highlightColor,
            topLeft = Offset(0f, centerY - (bandHeight / 2f)),
            size =
                androidx.compose.ui.geometry
                    .Size(size.width, bandHeight),
        )
    }
}
