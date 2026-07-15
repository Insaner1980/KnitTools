package com.finnvek.knittools.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.finnvek.knittools.ui.theme.CounterDimens
import kotlin.math.min

private const val STEP_SYMBOL_LENGTH_FRACTION = 0.76f
private const val STEP_SYMBOL_STROKE_FRACTION = 0.14f
private const val COUNTER_STEPPER_DISABLED_ALPHA = 0.38f
private const val COUNTER_STEPPER_SUBDUED_ALPHA = 0.64f

enum class CounterStepSymbol {
    Plus,
    Minus,
}

internal data class CounterStepperColors(
    val container: Color,
    val content: Color,
)

data class CounterStepButtonFaceAppearance(
    val visualSize: Dp,
    val symbolSize: Dp,
    val containerColor: Color,
    val contentColor: Color,
)

internal fun extraCounterStepperColors(
    isLightTheme: Boolean,
    isIncrement: Boolean,
    primary: Color,
    neutralContent: Color,
    surfaceVariant: Color,
    surfaceContainerHighest: Color,
): CounterStepperColors =
    if (isLightTheme) {
        CounterStepperColors(
            container = surfaceContainerHighest,
            content = if (isIncrement) primary else neutralContent,
        )
    } else {
        CounterStepperColors(
            container = surfaceVariant,
            content = if (isIncrement) primary else neutralContent,
        )
    }

@Composable
fun CounterStepperButton(
    symbol: CounterStepSymbol,
    isIncrement: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    prominent: Boolean = true,
) {
    val colors =
        extraCounterStepperColors(
            isLightTheme = MaterialTheme.colorScheme.background.luminance() > 0.5f,
            isIncrement = isIncrement,
            primary = MaterialTheme.colorScheme.primary,
            neutralContent = MaterialTheme.colorScheme.onSurfaceVariant,
            surfaceVariant = MaterialTheme.colorScheme.surfaceVariant,
            surfaceContainerHighest = MaterialTheme.colorScheme.surfaceContainerHighest,
        )

    Box(
        modifier =
            modifier
                .size(CounterDimens.ExtraCounterStepperTouchSize)
                .clickable(
                    enabled = enabled,
                    onClick = onClick,
                ),
        contentAlignment = Alignment.Center,
    ) {
        CounterStepButtonFace(
            symbol = symbol,
            contentDescription = contentDescription,
            appearance =
                CounterStepButtonFaceAppearance(
                    visualSize = CounterDimens.ExtraCounterStepperVisualSize,
                    symbolSize = CounterDimens.ExtraCounterStepperIconSize,
                    containerColor = colors.container,
                    contentColor = colors.content,
                ),
            enabled = enabled,
            prominent = prominent,
        )
    }
}

@Composable
fun CounterStepButtonFace(
    symbol: CounterStepSymbol,
    appearance: CounterStepButtonFaceAppearance,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    enabled: Boolean = true,
    prominent: Boolean = true,
) {
    val stepColor =
        appearance.contentColor.copy(
            alpha = counterStepperVisualAlpha(enabled = enabled, prominent = prominent),
        )

    Box(
        modifier =
            modifier
                .size(appearance.visualSize)
                .clip(CircleShape)
                .background(appearance.containerColor)
                .border(
                    width = appearance.symbolSize * STEP_SYMBOL_STROKE_FRACTION,
                    color = stepColor,
                    shape = CircleShape,
                ),
        contentAlignment = Alignment.Center,
    ) {
        CounterStepSymbolIcon(
            symbol = symbol,
            contentDescription = contentDescription,
            modifier = Modifier.size(appearance.symbolSize),
            tint = stepColor,
        )
    }
}

private fun counterStepperVisualAlpha(
    enabled: Boolean,
    prominent: Boolean,
): Float =
    when {
        !enabled -> COUNTER_STEPPER_DISABLED_ALPHA
        !prominent -> COUNTER_STEPPER_SUBDUED_ALPHA
        else -> 1f
    }

@Composable
fun CounterStepSymbolIcon(
    symbol: CounterStepSymbol,
    tint: Color,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    Canvas(
        modifier =
            modifier.semantics {
                contentDescription?.let { this.contentDescription = it }
            },
    ) {
        val iconSize = min(size.width, size.height)
        val strokeWidth = iconSize * STEP_SYMBOL_STROKE_FRACTION
        val halfLength = iconSize * STEP_SYMBOL_LENGTH_FRACTION / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        drawLine(
            color = tint,
            start = Offset(center.x - halfLength, center.y),
            end = Offset(center.x + halfLength, center.y),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        when (symbol) {
            CounterStepSymbol.Plus ->
                drawLine(
                    color = tint,
                    start = Offset(center.x, center.y - halfLength),
                    end = Offset(center.x, center.y + halfLength),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                )

            CounterStepSymbol.Minus -> Unit
        }
    }
}
