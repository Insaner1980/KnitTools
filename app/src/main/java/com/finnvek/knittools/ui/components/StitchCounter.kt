package com.finnvek.knittools.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.finnvek.knittools.R

@Composable
fun StitchCounter(
    currentStitch: Int,
    totalStitches: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isComplete = totalStitches > 0 && currentStitch >= totalStitches
    val containerColor =
        if (isComplete) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }
    val contentColor =
        if (isComplete) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(onClick = onDecrement) {
                Text(text = stringResource(R.string.stitch_previous), color = contentColor)
            }
            Text(
                text = stringResource(R.string.stitch_counter_compact_format, currentStitch, totalStitches),
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
            )
            TextButton(onClick = onIncrement) {
                Text(text = stringResource(R.string.stitch_next), color = contentColor)
            }
        }
    }
}
