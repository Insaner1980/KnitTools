package com.finnvek.knittools.ui.screens.gauge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.finnvek.knittools.R
import com.finnvek.knittools.ui.components.NumberInputField
import com.finnvek.knittools.ui.components.NumberInputOptions
import java.util.Locale

@Composable
internal fun <T> GaugeSelector(
    label: String,
    selectedValue: T,
    choices: List<Pair<T, String>>,
    onSelect: (T) -> Unit,
    tag: String,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val selectedLabel = choices.first { it.first == selectedValue }.second
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val menuWidth = maxWidth
        Surface(
            onClick = {
                focusManager.clearFocus()
                expanded = !expanded
            },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag(tag)
                    .semantics {
                        role = Role.Button
                        stateDescription = selectedLabel
                    },
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(selectedLabel, style = MaterialTheme.typography.bodyLarge)
                }
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(menuWidth),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = MaterialTheme.shapes.medium,
        ) {
            choices.forEachIndexed { index, (value, text) ->
                DropdownMenuItem(
                    text = { Text(text, style = MaterialTheme.typography.bodyLarge) },
                    onClick = {
                        expanded = false
                        onSelect(value)
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag(tag + "_option_" + index)
                            .semantics { selected = value == selectedValue },
                    trailingIcon = {
                        if (value == selectedValue) Icon(Icons.Default.Check, contentDescription = null)
                    },
                )
            }
        }
    }
}

@Composable
internal fun GaugeNumericField(
    state: GaugeUiState,
    field: GaugeField,
    onAction: (GaugeAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val input = state.input(field)
    val fieldLabel = stringResource(gaugeFieldLabels.getValue(field))
    val unit =
        when {
            field == GaugeField.CONVERSION -> state.fromUnit
            field.isLength -> state.lengthUnit
            else -> null
        }
    val visibleUnit = unit?.let { stringResource(it.shortResource()) }
    val fullUnit = unit?.let { stringResource(it.titleResource()) }
    val basisLabel = if (field.isGauge) stringResource(state.basis.titleResource()) else null
    val visibleLabel =
        (basisLabel ?: visibleUnit)?.let { stringResource(R.string.measurement_field_with_unit, fieldLabel, it) }
            ?: fieldLabel
    val spokenLabel =
        (basisLabel ?: fullUnit)?.let { stringResource(R.string.measurement_field_with_unit, fieldLabel, it) }
            ?: fieldLabel
    val error = if (input.touched) input.error?.let { stringResource(it.messageResource()) } else null
    key(field) {
        NumberInputField(
            value = input.text,
            onValueChange = { onAction(GaugeAction.Edit(field, it)) },
            label = visibleLabel,
            modifier = modifier.fillMaxWidth(),
            options =
                NumberInputOptions(
                    isDecimal = !field.isCount,
                    isLast = field in lastFields,
                    preserveRawInput = true,
                    allowZero = field == GaugeField.CONVERSION,
                ),
            errorMessage = error,
            semanticLabel = spokenLabel,
            onFocusLost = { onAction(GaugeAction.Blur(field)) },
            inputModifier = Modifier.testTag("measurement_input_" + field.name.lowercase(Locale.ROOT)),
        )
    }
}

@Composable
internal fun GaugeHeading(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier.semantics { heading() },
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

@Composable
internal fun GaugeResults(
    presentation: GaugePresentation,
    onCopy: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (presentation.sections.isEmpty()) return
    val focusManager = LocalFocusManager.current
    Column(
        modifier = modifier.fillMaxWidth().testTag("measurement_results"),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        GaugeHeading(stringResource(R.string.measurement_result))
        presentation.sections.forEach { section ->
            Column(
                modifier = Modifier.fillMaxWidth().testTag("measurement_result_" + section.id),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (section.id != "conversion") GaugeHeading(section.title)
                (section.inputs + section.results).forEach { line ->
                    GaugeResultValue(section.id, line)
                }
            }
        }
        Button(
            onClick = {
                focusManager.clearFocus()
                onCopy(presentation.copyText)
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("measurement_copy"),
        ) {
            Text(stringResource(R.string.measurement_copy))
        }
    }
}

@Composable
private fun GaugeResultValue(
    sectionId: String,
    line: GaugeResultLine,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            line.label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = line.value,
            style =
                if (line.id ==
                    "nearest_count"
                ) {
                    MaterialTheme.typography.headlineSmall
                } else {
                    MaterialTheme.typography.bodyLarge
                },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .testTag("measurement_result_" + sectionId + "_" + line.id)
                    .semantics { contentDescription = line.spokenValue },
        )
    }
}

private val lastFields =
    setOf(
        GaugeField.CONVERSION,
        GaugeField.SWATCH_ROWS,
        GaugeField.PATTERN_ROW_COUNT,
        GaugeField.TARGET_WIDTH,
        GaugeField.TARGET_HEIGHT,
        GaugeField.STITCH_COUNT,
        GaugeField.ROW_COUNT,
    )

private val gaugeFieldLabels =
    mapOf(
        GaugeField.CONVERSION to R.string.measurement_value,
        GaugeField.SWATCH_WIDTH to R.string.measured_width,
        GaugeField.SWATCH_STITCHES to R.string.stitch_count_in_swatch,
        GaugeField.SWATCH_HEIGHT to R.string.measured_height,
        GaugeField.SWATCH_ROWS to R.string.row_count_in_swatch,
        GaugeField.ACTUAL_STITCHES to R.string.measurement_actual_stitches,
        GaugeField.ACTUAL_ROWS to R.string.measurement_actual_rows,
        GaugeField.PATTERN_STITCHES to R.string.measurement_pattern_stitches,
        GaugeField.PATTERN_ROWS to R.string.measurement_pattern_rows,
        GaugeField.PATTERN_STITCH_COUNT to R.string.stitches_in_pattern,
        GaugeField.PATTERN_ROW_COUNT to R.string.rows_in_pattern,
        GaugeField.TARGET_WIDTH to R.string.measurement_target_width,
        GaugeField.TARGET_HEIGHT to R.string.measurement_target_height,
        GaugeField.STITCH_COUNT to R.string.measurement_stitch_count,
        GaugeField.ROW_COUNT to R.string.measurement_row_count,
    )
