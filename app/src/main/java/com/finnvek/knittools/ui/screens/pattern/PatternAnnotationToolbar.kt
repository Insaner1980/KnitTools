package com.finnvek.knittools.ui.screens.pattern

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.ChartColumnDirection
import com.finnvek.knittools.domain.model.ChartCorner
import com.finnvek.knittools.domain.model.ChartRowDirection
import com.finnvek.knittools.domain.model.ChartTrackingMode
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
    val onAddChartTracker: (PatternChartTrackerDraft) -> Unit,
)

@Composable
internal fun PatternAnnotationToolbar(
    state: PatternAnnotationUiState,
    actions: PatternAnnotationToolbarActions,
    modifier: Modifier = Modifier,
) {
    val hapticFeedback = LocalHapticFeedback.current
    var showTextEditor by rememberSaveable { mutableStateOf(false) }
    var showCalloutEditor by rememberSaveable { mutableStateOf(false) }
    var showChartTrackerEditor by rememberSaveable { mutableStateOf(false) }
    Column(
        verticalArrangement = Arrangement.spacedBy(PatternAnnotationTokens.TOOLBAR_ITEM_SPACING),
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(max = PatternAnnotationTokens.TOOLBAR_MAX_HEIGHT)
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = PatternAnnotationTokens.TOOLBAR_HORIZONTAL_PADDING,
                    vertical = PatternAnnotationTokens.TOOLBAR_VERTICAL_PADDING,
                ),
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
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    label = { Text(stringResource(item.labelRes)) },
                    modifier = Modifier.heightIn(min = PatternAnnotationTokens.TOOL_TOUCH_TARGET),
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
        if (state.selectedAnnotationSupportsChartTracker && state.chartCounterOptions.isNotEmpty()) {
            TextButton(onClick = { showChartTrackerEditor = true }) {
                Text(stringResource(R.string.pattern_annotation_link_chart))
            }
        }
        if (state.selectedAnnotationIsEditable) {
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
    if (showChartTrackerEditor && state.chartCounterOptions.isNotEmpty()) {
        PatternChartTrackerDialog(
            counterOptions = state.chartCounterOptions,
            onDismiss = { showChartTrackerEditor = false },
            onConfirm = { draft ->
                actions.onAddChartTracker(draft)
                showChartTrackerEditor = false
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
private fun PatternChartTrackerDialog(
    counterOptions: List<PatternChartCounterOption>,
    onDismiss: () -> Unit,
    onConfirm: (PatternChartTrackerDraft) -> Unit,
) {
    var rows by rememberSaveable { mutableStateOf(DEFAULT_CHART_DIMENSION.toString()) }
    var columns by rememberSaveable { mutableStateOf(DEFAULT_CHART_DIMENSION.toString()) }
    var gridStartIndex by rememberSaveable { mutableStateOf("0") }
    var rowDirection by rememberSaveable { mutableStateOf(ChartRowDirection.BOTTOM_TO_TOP) }
    var columnDirection by rememberSaveable { mutableStateOf(ChartColumnDirection.LEFT_TO_RIGHT) }
    var trackingMode by rememberSaveable { mutableStateOf(ChartTrackingMode.ACTIVE_ROW) }
    var selectedCounterIndex by rememberSaveable { mutableIntStateOf(0) }
    var wrapAtEnd by rememberSaveable { mutableStateOf(false) }
    var c2cOrigin by rememberSaveable { mutableStateOf(ChartCorner.BOTTOM_LEFT) }
    val validRows = rows.toIntOrNull()?.takeIf { it in 1..MAX_CHART_DIMENSION }
    val validColumns = columns.toIntOrNull()?.takeIf { it in 1..MAX_CHART_DIMENSION }
    val validGridStart = gridStartIndex.toIntOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pattern_annotation_chart_setup_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = rows,
                        onValueChange = { rows = it },
                        label = { Text(stringResource(R.string.pattern_annotation_chart_rows)) },
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = columns,
                        onValueChange = { columns = it },
                        label = { Text(stringResource(R.string.pattern_annotation_chart_columns)) },
                        modifier = Modifier.weight(1f),
                    )
                }
                ChartChoiceRow(
                    choices = ChartRowDirection.entries,
                    selected = rowDirection,
                    label = { direction ->
                        if (direction == ChartRowDirection.BOTTOM_TO_TOP) {
                            stringResource(R.string.pattern_annotation_chart_bottom_to_top)
                        } else {
                            stringResource(R.string.pattern_annotation_chart_top_to_bottom)
                        }
                    },
                    onSelected = { rowDirection = it },
                )
                ChartChoiceRow(
                    choices = ChartColumnDirection.entries,
                    selected = columnDirection,
                    label = { direction -> stringResource(direction.columnDirectionLabel()) },
                    onSelected = { columnDirection = it },
                )
                ChartChoiceRow(
                    choices = ChartTrackingMode.entries,
                    selected = trackingMode,
                    label = { mode -> stringResource(mode.trackingModeLabel()) },
                    onSelected = { trackingMode = it },
                )
                ChartChoiceRow(
                    choices = counterOptions.indices.toList(),
                    selected = selectedCounterIndex.coerceIn(counterOptions.indices),
                    label = { index -> counterOptions[index].name },
                    onSelected = { selectedCounterIndex = it },
                )
                OutlinedTextField(
                    value = gridStartIndex,
                    onValueChange = { gridStartIndex = it },
                    label = { Text(stringResource(R.string.pattern_annotation_chart_grid_start)) },
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.pattern_annotation_chart_wrap), modifier = Modifier.weight(1f))
                    Switch(checked = wrapAtEnd, onCheckedChange = { wrapAtEnd = it })
                }
                if (trackingMode == ChartTrackingMode.C2C_DIAGONAL) {
                    ChartChoiceRow(
                        choices = ChartCorner.entries,
                        selected = c2cOrigin,
                        label = { corner -> stringResource(corner.cornerLabel()) },
                        onSelected = { c2cOrigin = it },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = validRows != null && validColumns != null && validGridStart != null,
                onClick = {
                    onConfirm(
                        PatternChartTrackerDraft(
                            rows = checkNotNull(validRows),
                            columns = checkNotNull(validColumns),
                            rowDirection = rowDirection,
                            columnDirection = columnDirection,
                            trackingMode = trackingMode,
                            counter = counterOptions[selectedCounterIndex.coerceIn(counterOptions.indices)],
                            gridStartIndex = checkNotNull(validGridStart),
                            wrapAtEnd = wrapAtEnd,
                            c2cOrigin = c2cOrigin,
                        ),
                    )
                },
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun <T> ChartChoiceRow(
    choices: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelected: (T) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState()),
    ) {
        choices.forEach { choice ->
            FilterChip(
                selected = choice == selected,
                onClick = { onSelected(choice) },
                label = { Text(label(choice)) },
                modifier = Modifier.heightIn(min = PatternAnnotationTokens.TOOL_TOUCH_TARGET),
            )
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
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.horizontalScroll(rememberScrollState()),
    ) {
        PatternAnnotationTokens.COLOR_PALETTE_ARGB.forEach { argb ->
            val selected = selectedArgb.rgbOnly() == argb.rgbOnly()
            val colorDescription =
                stringResource(
                    R.string.pattern_annotation_color_option,
                    stringResource(annotationColorLabel(argb)),
                )
            Box(
                modifier =
                    Modifier
                        .size(PatternAnnotationTokens.TOOL_TOUCH_TARGET)
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
                        ).selectable(
                            selected = selected,
                            onClick = { onArgbChange(argb) },
                            role = Role.RadioButton,
                        ).semantics { contentDescription = colorDescription },
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

@StringRes
private fun annotationColorLabel(argb: Int): Int =
    when (argb.rgbOnly()) {
        0x001F1F1F -> R.string.pattern_annotation_color_black
        0x00D32F2F -> R.string.pattern_annotation_color_red
        0x001976D2 -> R.string.pattern_annotation_color_blue
        0x00388E3C -> R.string.pattern_annotation_color_green
        0x00FFC107 -> R.string.pattern_annotation_color_yellow
        else -> R.string.pattern_annotation_color_custom
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
                modifier = Modifier.heightIn(min = PatternAnnotationTokens.TOOL_TOUCH_TARGET),
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
        ToolItem(PatternAnnotationTool.CHART, R.string.pattern_annotation_tool_chart),
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

private fun ChartColumnDirection.columnDirectionLabel(): Int =
    when (this) {
        ChartColumnDirection.LEFT_TO_RIGHT -> R.string.pattern_annotation_chart_left_to_right
        ChartColumnDirection.RIGHT_TO_LEFT -> R.string.pattern_annotation_chart_right_to_left
        ChartColumnDirection.ALTERNATING -> R.string.pattern_annotation_chart_alternating
    }

private fun ChartTrackingMode.trackingModeLabel(): Int =
    when (this) {
        ChartTrackingMode.ACTIVE_ROW -> R.string.pattern_annotation_chart_active_row
        ChartTrackingMode.ACTIVE_COLUMN -> R.string.pattern_annotation_chart_active_column
        ChartTrackingMode.CROSSHAIR -> R.string.pattern_annotation_chart_crosshair
        ChartTrackingMode.COMPLETED_CELLS -> R.string.pattern_annotation_chart_completed
        ChartTrackingMode.C2C_DIAGONAL -> R.string.pattern_annotation_chart_c2c
    }

private fun ChartCorner.cornerLabel(): Int =
    when (this) {
        ChartCorner.TOP_LEFT -> R.string.pattern_annotation_chart_top_left
        ChartCorner.TOP_RIGHT -> R.string.pattern_annotation_chart_top_right
        ChartCorner.BOTTOM_LEFT -> R.string.pattern_annotation_chart_bottom_left
        ChartCorner.BOTTOM_RIGHT -> R.string.pattern_annotation_chart_bottom_right
    }

private const val SELECTION_NUDGE = 0.01f
private const val SELECTION_SCALE_DOWN = 0.9f
private const val SELECTION_SCALE_UP = 1.1f
private const val DEFAULT_CHART_DIMENSION = 10
private const val MAX_CHART_DIMENSION = 999
