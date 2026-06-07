package com.finnvek.knittools.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.finnvek.knittools.R
import com.finnvek.knittools.ui.theme.CounterDimens

@Composable
fun StitchCounter(
    label: String,
    currentStitch: Int,
    totalStitches: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor =
        MaterialTheme.colorScheme.surfaceContainerHighest.copy(
            alpha = CounterDimens.StitchTrackerContainerAlpha,
        )

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(CounterDimens.StitchTrackerCornerRadius),
        color = containerColor,
    ) {
        Row(
            modifier =
                Modifier.padding(
                    horizontal = CounterDimens.StitchTrackerHorizontalPadding,
                    vertical = CounterDimens.StitchTrackerVerticalPadding,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CounterDimens.StitchTrackerContentSpacing),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            CounterStepperButton(
                icon = Icons.Filled.Remove,
                isIncrement = false,
                contentDescription = stringResource(R.string.counter_decrease),
                onClick = onDecrement,
            )
            Text(
                text = stringResource(R.string.stitch_counter_compact_format, currentStitch, totalStitches),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            CounterStepperButton(
                icon = Icons.Filled.Add,
                isIncrement = true,
                contentDescription = stringResource(R.string.counter_increase),
                onClick = onIncrement,
            )
        }
    }
}
