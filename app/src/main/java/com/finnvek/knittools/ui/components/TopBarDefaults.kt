package com.finnvek.knittools.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawBehind

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Modifier.scrolledTopBarDivider(scrollBehavior: TopAppBarScrollBehavior): Modifier {
    val dividerColor = MaterialTheme.colorScheme.outlineVariant
    return drawBehind {
        if (scrollBehavior.state.contentOffset < 0f) {
            drawLine(
                color = dividerColor,
                start = Offset(0f, size.height),
                end = Offset(size.width, size.height),
            )
        }
    }
}
