package com.finnvek.knittools.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.finnvek.knittools.ui.theme.ComponentDimens

private val PillContainerShape = RoundedCornerShape(50)
private val PillItemShape = RoundedCornerShape(50)
private val GridContainerShape = RoundedCornerShape(16.dp)
private val GridItemShape = RoundedCornerShape(12.dp)

@Composable
fun SegmentedToggle(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemTestTagPrefix: String? = null,
) {
    val isGrid = options.size > 3
    val expandedLabels = LocalDensity.current.fontScale >= 1.5f
    val containerShape: Shape = if (isGrid || expandedLabels) GridContainerShape else PillContainerShape
    val itemShape: Shape = if (isGrid || expandedLabels) GridItemShape else PillItemShape

    Box(
        modifier =
            modifier
                // Täysleveä: keskitetty 70 %:n valitsin ei linjautunut kenttien reunoihin.
                .fillMaxWidth()
                .clip(containerShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .selectableGroup()
                .padding(ComponentDimens.SegmentedContainerPadding),
    ) {
        if (isGrid) {
            SegmentedToggleGrid(
                options = options,
                selectedIndex = selectedIndex,
                onSelect = onSelect,
                itemShape = itemShape,
                itemTestTagPrefix = itemTestTagPrefix,
            )
        } else {
            SegmentedTogglePill(
                options = options,
                selectedIndex = selectedIndex,
                onSelect = onSelect,
                itemShape = itemShape,
                itemTestTagPrefix = itemTestTagPrefix,
            )
        }
    }
}

@Composable
private fun SegmentedToggleGrid(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    itemShape: Shape,
    itemTestTagPrefix: String?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(ComponentDimens.CompactSpacing)) {
        for (rowStart in options.indices step 2) {
            SegmentedToggleGridRow(
                options = options,
                rowStart = rowStart,
                selectedIndex = selectedIndex,
                onSelect = onSelect,
                itemShape = itemShape,
                itemTestTagPrefix = itemTestTagPrefix,
            )
        }
    }
}

@Composable
private fun SegmentedToggleGridRow(
    options: List<String>,
    rowStart: Int,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    itemShape: Shape,
    itemTestTagPrefix: String?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ComponentDimens.CompactSpacing),
    ) {
        SegmentedToggleItem(
            label = options[rowStart],
            isSelected = rowStart == selectedIndex,
            onClick = { onSelect(rowStart) },
            shape = itemShape,
            modifier = Modifier.weight(1f).itemTestTag(itemTestTagPrefix, rowStart),
        )
        if (rowStart + 1 < options.size) {
            SegmentedToggleItem(
                label = options[rowStart + 1],
                isSelected = rowStart + 1 == selectedIndex,
                onClick = { onSelect(rowStart + 1) },
                shape = itemShape,
                modifier = Modifier.weight(1f).itemTestTag(itemTestTagPrefix, rowStart + 1),
            )
        }
    }
}

@Composable
private fun SegmentedTogglePill(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    itemShape: Shape,
    itemTestTagPrefix: String?,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, label ->
            SegmentedToggleItem(
                label = label,
                isSelected = index == selectedIndex,
                onClick = { onSelect(index) },
                shape = itemShape,
                modifier = Modifier.weight(1f).itemTestTag(itemTestTagPrefix, index),
            )
        }
    }
}

@Composable
private fun SegmentedToggleItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    shape: Shape,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .heightIn(min = ComponentDimens.SegmentedItemMinHeight)
                .clip(shape)
                .then(
                    // Tasainen kuten muut oranssit painikkeet: liukuväri teki valinnasta eri näköisen.
                    if (isSelected) {
                        Modifier.background(MaterialTheme.colorScheme.primary)
                    } else {
                        Modifier
                    },
                ).selectable(
                    selected = isSelected,
                    onClick = onClick,
                    role = Role.RadioButton,
                ),
        contentAlignment = Alignment.Center,
    ) {
        val labelStyle =
            if (isSelected) {
                MaterialTheme.typography.labelMedium
            } else {
                MaterialTheme.typography.bodySmall
            }
        val textColor =
            if (isSelected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        BasicText(
            modifier =
                Modifier.padding(
                    horizontal = ComponentDimens.SegmentedItemTextInset,
                    vertical = ComponentDimens.CompactSpacing,
                ),
            text = label,
            style =
                labelStyle.copy(
                    textAlign = TextAlign.Center,
                    color = textColor,
                ),
            autoSize =
                TextAutoSize.StepBased(
                    minFontSize = MaterialTheme.typography.labelSmall.fontSize,
                    maxFontSize = labelStyle.fontSize,
                ),
            maxLines = 3,
        )
    }
}

// Laitetestien tunniste vaihtoehdolle: sama "_option_n"-muoto kuin aiemmissa pudotusvalikoissa.
private fun Modifier.itemTestTag(
    prefix: String?,
    index: Int,
): Modifier = if (prefix == null) this else testTag("${prefix}_option_$index")
