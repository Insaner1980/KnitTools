package com.finnvek.knittools.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import com.finnvek.knittools.ui.theme.CounterDimens

internal data class CounterStepperColors(
    val container: Color,
    val content: Color,
)

internal fun extraCounterStepperColors(
    isLightTheme: Boolean,
    isIncrement: Boolean,
    primary: Color,
    onSurface: Color,
    surfaceVariant: Color,
    surfaceContainerHighest: Color,
): CounterStepperColors =
    if (isLightTheme) {
        CounterStepperColors(
            container = surfaceContainerHighest,
            content = if (isIncrement) primary else onSurface,
        )
    } else {
        CounterStepperColors(
            container = surfaceVariant,
            content = onSurface,
        )
    }

@Composable
fun CounterStepperButton(
    icon: ImageVector,
    isIncrement: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors =
        extraCounterStepperColors(
            isLightTheme = MaterialTheme.colorScheme.background.luminance() > 0.5f,
            isIncrement = isIncrement,
            primary = MaterialTheme.colorScheme.primary,
            onSurface = MaterialTheme.colorScheme.onSurface,
            surfaceVariant = MaterialTheme.colorScheme.surfaceVariant,
            surfaceContainerHighest = MaterialTheme.colorScheme.surfaceContainerHighest,
        )

    Box(
        modifier =
            modifier
                .size(CounterDimens.ExtraCounterStepperTouchSize)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(CounterDimens.ExtraCounterStepperVisualSize)
                    .clip(CircleShape)
                    .background(colors.container),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(CounterDimens.ExtraCounterStepperIconSize),
                tint = colors.content,
            )
        }
    }
}
