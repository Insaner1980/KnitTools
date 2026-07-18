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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.finnvek.knittools.domain.model.PatternCalloutSymbol
import com.finnvek.knittools.ui.theme.PatternAnnotationTokens

internal data class PatternAnnotationToolbarActions(
    val onToolSelected: (PatternAnnotationTool) -> Unit,
    val onPenArgbChange: (Int) -> Unit,
    val onHighlighterArgbChange: (Int) -> Unit,
    val onPenStrokeWidthChange: (Float) -> Unit,
    val onHighlighterStrokeWidthChange: (Float) -> Unit,
    val onPressureEnabledChange: (Boolean) -> Unit,
    val onHighlighterAxisLockChange: (PatternHighlighterAxisLock) -> Unit,
    val onMoveSelected: (Float, Float) -> Unit,
    val onResizeSelected: (Float) -> Unit,
    val onDuplicateSelected: () -> Unit,
    val onDeleteSelected: () -> Unit,
    val onBringSelectedForward: () -> Unit,
    val onSendSelectedBackward: () -> Unit,
    val onAddTextBox: (String) -> Unit,
    val onAddCallout: (String, String, PatternCalloutSymbol) -> Unit,
    val onUndo: () -> Unit,
    val onRedo: () -> Unit,
    val onClearPage: () -> Unit,
)

@Composable
internal fun PatternAnnotationToolbar(
    state: PatternAnnotationUiState,
    actions: PatternAnnotationToolbarActions,
    modifier: Modifier = Modifier,
) {
    var showTextEditor by rememberSaveable { mutableStateOf(false) }
    var showCalloutEditor by rememberSaveable { mutableStateOf(false) }
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
                    onClick = {
                        when (item.tool) {
                            PatternAnnotationTool.TEXT -> showTextEditor = true
                            PatternAnnotationTool.CALLOUT -> showCalloutEditor = true
                            else -> actions.onToolSelected(item.tool)
                        }
                    },
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
        AnnotationHistoryControls(state, actions)
        if (state.selectedAnnotationId != null) {
            AnnotationSelectionControls(actions)
        }
    }
    if (showTextEditor) {
        PatternTextEditorDialog(
            onDismiss = { showTextEditor = false },
            onConfirm = { text ->
                actions.onAddTextBox(text)
                showTextEditor = false
            },
        )
    }
    if (showCalloutEditor) {
        PatternCalloutEditorDialog(
            onDismiss = { showCalloutEditor = false },
            onConfirm = { title, description ->
                actions.onAddCallout(title, description, PatternCalloutSymbol.NOTE)
                showCalloutEditor = false
            },
        )
    }
}

@Composable
private fun AnnotationHistoryControls(
    state: PatternAnnotationUiState,
    actions: PatternAnnotationToolbarActions,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState()),
    ) {
        TextButton(onClick = actions.onUndo, enabled = state.canUndo) {
            Text(stringResource(R.string.pattern_annotation_undo))
        }
        TextButton(onClick = actions.onRedo, enabled = state.canRedo) {
            Text(stringResource(R.string.pattern_annotation_redo))
        }
        TextButton(onClick = actions.onClearPage) {
            Text(stringResource(R.string.pattern_annotation_clear_page))
        }
    }
}

@Composable
private fun AnnotationSelectionControls(actions: PatternAnnotationToolbarActions) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState()),
    ) {
        TextButton(onClick = { actions.onMoveSelected(-SELECTION_NUDGE, 0f) }) {
            Text(stringResource(R.string.pattern_annotation_move_left))
        }
        TextButton(onClick = { actions.onMoveSelected(SELECTION_NUDGE, 0f) }) {
            Text(stringResource(R.string.pattern_annotation_move_right))
        }
        TextButton(onClick = { actions.onMoveSelected(0f, -SELECTION_NUDGE) }) {
            Text(stringResource(R.string.pattern_annotation_move_up))
        }
        TextButton(onClick = { actions.onMoveSelected(0f, SELECTION_NUDGE) }) {
            Text(stringResource(R.string.pattern_annotation_move_down))
        }
        TextButton(onClick = { actions.onResizeSelected(SELECTION_SCALE_DOWN) }) {
            Text(stringResource(R.string.pattern_annotation_smaller))
        }
        TextButton(onClick = { actions.onResizeSelected(SELECTION_SCALE_UP) }) {
            Text(stringResource(R.string.pattern_annotation_larger))
        }
        TextButton(onClick = actions.onDuplicateSelected) {
            Text(stringResource(R.string.pattern_annotation_duplicate))
        }
        TextButton(onClick = actions.onBringSelectedForward) {
            Text(stringResource(R.string.pattern_annotation_bring_forward))
        }
        TextButton(onClick = actions.onSendSelectedBackward) {
            Text(stringResource(R.string.pattern_annotation_send_backward))
        }
        TextButton(onClick = actions.onDeleteSelected) {
            Text(stringResource(R.string.pattern_annotation_delete))
        }
    }
}

@Composable
private fun PatternTextEditorDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pattern_annotation_text_editor_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(stringResource(R.string.pattern_annotation_text_label)) },
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }, enabled = text.isNotBlank()) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun PatternCalloutEditorDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
) {
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pattern_annotation_callout_editor_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.pattern_annotation_callout_title_label)) },
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.pattern_annotation_callout_description_label)) },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(title, description) },
                enabled = title.isNotBlank() || description.isNotBlank(),
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
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
        ToolItem(PatternAnnotationTool.SELECT, R.string.pattern_annotation_tool_select),
        ToolItem(PatternAnnotationTool.LINE, R.string.pattern_annotation_tool_line),
        ToolItem(PatternAnnotationTool.ARROW, R.string.pattern_annotation_tool_arrow),
        ToolItem(PatternAnnotationTool.RECTANGLE, R.string.pattern_annotation_tool_rectangle),
        ToolItem(PatternAnnotationTool.ELLIPSE, R.string.pattern_annotation_tool_ellipse),
        ToolItem(PatternAnnotationTool.TEXT, R.string.pattern_annotation_tool_text),
        ToolItem(PatternAnnotationTool.CALLOUT, R.string.pattern_annotation_tool_callout),
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

private const val SELECTION_NUDGE = 0.01f
private const val SELECTION_SCALE_DOWN = 0.9f
private const val SELECTION_SCALE_UP = 1.1f
