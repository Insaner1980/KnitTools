package com.finnvek.knittools.ui.tips

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import com.finnvek.knittools.R
import com.finnvek.knittools.ui.theme.ComponentDimens
import com.finnvek.knittools.ui.theme.OnboardingDimens
import com.finnvek.knittools.ui.theme.knitToolsColors

@Composable
internal fun StitchMarkerTip(
    text: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
    exposeSemantics: Boolean = true,
) {
    val actionColors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.knitToolsColors.primaryReadable)
    val tipSemantics =
        if (exposeSemantics) {
            Modifier
                .semantics {
                    liveRegion = LiveRegionMode.Polite
                    paneTitle = text
                }.focusable()
        } else {
            Modifier.clearAndSetSemantics { }
        }
    Surface(
        modifier =
            modifier
                .widthIn(max = OnboardingDimens.TipMaxWidth)
                .then(tipSemantics),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.knitToolsColors.cardContainer,
    ) {
        Column(
            modifier = Modifier.padding(OnboardingDimens.TipPadding),
            verticalArrangement = Arrangement.spacedBy(ComponentDimens.StandardSpacing),
        ) {
            Text(text, style = MaterialTheme.typography.bodyMedium)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                if (actionLabel != null) {
                    TextButton(
                        onClick = onAction,
                        enabled = exposeSemantics,
                        colors = actionColors,
                    ) { Text(actionLabel) }
                }
                TextButton(onClick = onDismiss, enabled = exposeSemantics, colors = actionColors) {
                    Text(stringResource(R.string.tip_got_it))
                }
            }
        }
    }
}

/** Koristeet eivät ota kosketuksia: lovi osoittaa ankkuriin ja lukittava silmukkamerkki sen reunaan. */
@Composable
internal fun TipAnchorDecoration(
    bubble: Rect,
    anchor: Rect,
    above: Boolean,
    modifier: Modifier = Modifier,
) {
    val bubbleColor = MaterialTheme.knitToolsColors.cardContainer
    val markerColor = MaterialTheme.colorScheme.primary
    Canvas(modifier) {
        val notch = OnboardingDimens.TipNotchSize.toPx()
        val x = anchor.center.x.coerceIn(bubble.left + notch, bubble.right - notch)
        val edge = if (above) bubble.bottom else bubble.top
        val direction = if (above) 1f else -1f
        val path =
            Path().apply {
                moveTo(x - notch, edge)
                lineTo(x, edge + direction * notch)
                lineTo(x + notch, edge)
                close()
            }
        drawPath(path, bubbleColor)
        val markerSize = OnboardingDimens.TipMarkerSize.toPx()
        val markerTop = (if (above) anchor.top else anchor.bottom) - markerSize / 2f
        val stroke = OnboardingDimens.TipMarkerStroke.toPx()
        val markerLeft = anchor.center.x - markerSize / 2f
        drawArc(
            color = markerColor,
            startAngle = OnboardingDimens.TIP_MARKER_GAP_START,
            sweepAngle = OnboardingDimens.TIP_MARKER_SWEEP,
            useCenter = false,
            topLeft = Offset(markerLeft + stroke, markerTop + stroke),
            size = Size(markerSize - stroke * 2f, markerSize - stroke * 2f),
            style = Stroke(stroke, cap = StrokeCap.Round),
        )
        drawLine(
            markerColor,
            start = Offset(markerLeft + markerSize * OnboardingDimens.TIP_CLASP_START_X, markerTop + stroke),
            end = Offset(markerLeft + markerSize - stroke, markerTop + markerSize * OnboardingDimens.TIP_CLASP_END_Y),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}
