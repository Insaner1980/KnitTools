package com.finnvek.knittools.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScrolledTopBarDivider(
    scrollBehavior: TopAppBarScrollBehavior,
    modifier: Modifier = Modifier,
) {
    if (scrollBehavior.state.contentOffset < 0f) {
        val color = MaterialTheme.colorScheme.outlineVariant
        Canvas(modifier = modifier.fillMaxWidth().height(1.dp)) {
            drawLine(
                color = color,
                start = Offset.Zero,
                end = Offset(size.width, 0f),
                strokeWidth = Stroke.HairlineWidth,
            )
        }
    }
}
