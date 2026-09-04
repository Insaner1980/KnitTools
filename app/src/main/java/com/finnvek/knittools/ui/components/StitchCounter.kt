package com.finnvek.knittools.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
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
    val canDecrement = currentStitch > 0
    val canIncrement = currentStitch < totalStitches
    val useLargeFontLayout = LocalDensity.current.fontScale >= 1.5f

    Surface(
        modifier = modifier.heightIn(min = CounterDimens.StitchTrackerMinHeight),
        shape = RoundedCornerShape(CounterDimens.StitchTrackerCornerRadius),
        color = containerColor,
    ) {
        val labelContent: @Composable () -> Unit = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val controlsContent: @Composable () -> Unit = {
            CounterStepperButton(
                symbol = CounterStepSymbol.Minus,
                isIncrement = false,
                contentDescription = stringResource(R.string.counter_decrease_named, label),
                onClick = onDecrement,
                enabled = canDecrement,
            )
            Text(
                text = stringResource(R.string.stitch_counter_compact_format, currentStitch, totalStitches),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            CounterStepperButton(
                symbol = CounterStepSymbol.Plus,
                isIncrement = true,
                contentDescription = stringResource(R.string.counter_increase_named, label),
                onClick = onIncrement,
                enabled = canIncrement,
            )
        }
        val contentModifier =
            Modifier.padding(
                horizontal = CounterDimens.StitchTrackerHorizontalPadding,
                vertical = CounterDimens.StitchTrackerVerticalPadding,
            )
        if (useLargeFontLayout) {
            Column(
                modifier = contentModifier,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(CounterDimens.StitchTrackerContentSpacing),
            ) {
                labelContent()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(CounterDimens.StitchTrackerContentSpacing),
                ) {
                    controlsContent()
                }
            }
        } else {
            Row(
                modifier = contentModifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(CounterDimens.StitchTrackerContentSpacing),
            ) {
                labelContent()
                controlsContent()
            }
        }
    }
}
