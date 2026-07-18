package com.finnvek.knittools.ui.screens.pattern

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.PatternAnnotationLimits
import com.finnvek.knittools.ui.theme.PatternAnnotationTokens

internal data class PatternAnnotationToolbarActions(
    val onToolSelected: (PatternAnnotationTool) -> Unit,
    val onPenArgbChange: (Int) -> Unit,
    val onHighlighterArgbChange: (Int) -> Unit,
    val onPenStrokeWidthChange: (Float) -> Unit,
    val onHighlighterStrokeWidthChange: (Float) -> Unit,
    val onPressureEnabledChange: (Boolean) -> Unit,
    val onHighlighterAxisLockChange: (PatternHighlighterAxisLock) -> Unit,
)

@Composable
internal fun PatternAnnotationToolbar(
    state: PatternAnnotationUiState,
    actions: PatternAnnotationToolbarActions,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            TOOL_ITEMS.forEach { item ->
                FilterChip(
                    selected = state.activeTool == item.tool,
                    onClick = { actions.onToolSelected(item.tool) },
                    label = { Text(stringResource(item.labelRes)) },
                )
            }
        }
        when (state.activeTool) {
            PatternAnnotationTool.PEN ->
                PatternStrokeStyleControls(
                    selectedArgb = state.penArgb,
                    strokeWidth = state.penStrokeWidth,
                    pressureEnabled = state.pressureEnabled,
                    onArgbChange = actions.onPenArgbChange,
                    onStrokeWidthChange = actions.onPenStrokeWidthChange,
                    onPressureEnabledChange = actions.onPressureEnabledChange,
                )

            PatternAnnotationTool.HIGHLIGHTER -> {
                PatternStrokeStyleControls(
                    selectedArgb = state.highlighterArgb,
                    strokeWidth = state.highlighterStrokeWidth,
                    pressureEnabled = false,
                    onArgbChange = { actions.onHighlighterArgbChange(it.withHighlighterAlpha()) },
                    onStrokeWidthChange = actions.onHighlighterStrokeWidthChange,
                    onPressureEnabledChange = {},
                    showPressureToggle = false,
                )
                HighlighterAxisControls(
                    selected = state.highlighterAxisLock,
                    onSelected = actions.onHighlighterAxisLockChange,
                )
            }

            else -> Unit
        }
    }
}

@Composable
private fun PatternStrokeStyleControls(
    selectedArgb: Int,
    strokeWidth: Float,
    pressureEnabled: Boolean,
    onArgbChange: (Int) -> Unit,
    onStrokeWidthChange: (Float) -> Unit,
    onPressureEnabledChange: (Boolean) -> Unit,
    showPressureToggle: Boolean = true,
) {
    val colorDescription = stringResource(R.string.pattern_annotation_color_option)
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.horizontalScroll(rememberScrollState()),
    ) {
        PatternAnnotationTokens.COLOR_PALETTE_ARGB.forEach { argb ->
            val selected = selectedArgb.rgbOnly() == argb.rgbOnly()
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(argb))
                        .border(
                            width = if (selected) 3.dp else 1.dp,
                            color =
                                if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outline
                                },
                            shape = CircleShape,
                        ).clickable { onArgbChange(argb) }
                        .semantics { contentDescription = colorDescription },
            )
        }
    }
    Text(
        text = stringResource(R.string.pattern_annotation_stroke_width, strokeWidth),
        style = MaterialTheme.typography.labelMedium,
    )
    Slider(
        value = strokeWidth,
        onValueChange = onStrokeWidthChange,
        valueRange = PatternAnnotationLimits.MIN_STROKE_WIDTH..PatternAnnotationLimits.MAX_STROKE_WIDTH,
    )
    if (showPressureToggle) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.pattern_annotation_pressure),
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = pressureEnabled,
                onCheckedChange = onPressureEnabledChange,
            )
        }
    }
}

@Composable
private fun HighlighterAxisControls(
    selected: PatternHighlighterAxisLock,
    onSelected: (PatternHighlighterAxisLock) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState()),
    ) {
        AXIS_ITEMS.forEach { item ->
            FilterChip(
                selected = selected == item.axisLock,
                onClick = { onSelected(item.axisLock) },
                label = { Text(stringResource(item.labelRes)) },
            )
        }
    }
}

private data class ToolItem(
    val tool: PatternAnnotationTool,
    val labelRes: Int,
)

private data class AxisItem(
    val axisLock: PatternHighlighterAxisLock,
    val labelRes: Int,
)

private val TOOL_ITEMS =
    listOf(
        ToolItem(PatternAnnotationTool.BROWSE, R.string.pattern_annotation_tool_browse),
        ToolItem(PatternAnnotationTool.PEN, R.string.pattern_annotation_tool_pen),
        ToolItem(PatternAnnotationTool.HIGHLIGHTER, R.string.pattern_annotation_tool_highlighter),
        ToolItem(PatternAnnotationTool.ERASER, R.string.pattern_annotation_tool_eraser),
    )

private val AXIS_ITEMS =
    listOf(
        AxisItem(PatternHighlighterAxisLock.FREE, R.string.pattern_annotation_axis_free),
        AxisItem(PatternHighlighterAxisLock.HORIZONTAL, R.string.pattern_annotation_axis_horizontal),
        AxisItem(PatternHighlighterAxisLock.VERTICAL, R.string.pattern_annotation_axis_vertical),
        AxisItem(PatternHighlighterAxisLock.DOMINANT_AXIS, R.string.pattern_annotation_axis_auto),
    )

private fun Int.rgbOnly(): Int = this and 0x00FFFFFF

private fun Int.withHighlighterAlpha(): Int = rgbOnly() or PatternAnnotationTokens.HIGHLIGHTER_DEFAULT_ALPHA.shl(24)
