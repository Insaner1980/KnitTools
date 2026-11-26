package com.finnvek.knittools.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.finnvek.knittools.R
import com.finnvek.knittools.ui.theme.ComponentDimens
import com.finnvek.knittools.ui.theme.FontScaleThresholds
import com.finnvek.knittools.ui.theme.knitToolsColors

private val PillContainerShape = RoundedCornerShape(50)
private val PillItemShape = RoundedCornerShape(50)
private val GridContainerShape = RoundedCornerShape(16.dp)
private val GridItemShape = RoundedCornerShape(12.dp)

enum class SegmentedToggleSurface { Default, Dialog }

@Composable
fun SegmentedToggle(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemTestTagPrefix: String? = null,
    surface: SegmentedToggleSurface = SegmentedToggleSurface.Default,
    pendingIndex: Int? = null,
    enabled: Boolean = true,
    optionTrailingContent: @Composable (Int) -> Unit = {},
) {
    val isGrid = options.size > 3
    val expandedLabels = LocalDensity.current.fontScale >= FontScaleThresholds.LARGE
    val containerShape: Shape = if (isGrid || expandedLabels) GridContainerShape else PillContainerShape
    val itemShape: Shape = if (isGrid || expandedLabels) GridItemShape else PillItemShape
    val inDialog = surface == SegmentedToggleSurface.Dialog
    val trackColor =
        if (inDialog) MaterialTheme.knitToolsColors.segmentedTrackOnDialog else MaterialTheme.colorScheme.surfaceVariant
    val unselectedTextColor =
        if (inDialog) {
            MaterialTheme.knitToolsColors.onSegmentedTrackOnDialog
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }

    Box(
        modifier =
            modifier
                // Täysleveä: keskitetty 70 %:n valitsin ei linjautunut kenttien reunoihin.
                .fillMaxWidth()
                .clip(containerShape)
                .background(trackColor)
                .selectableGroup()
                .padding(ComponentDimens.SegmentedContainerPadding),
    ) {
        CompositionLocalProvider(LocalContentColor provides unselectedTextColor) {
            if (isGrid) {
                SegmentedToggleGrid(
                    options = options,
                    selectedIndex = selectedIndex,
                    pendingIndex = pendingIndex,
                    onSelect = onSelect,
                    itemShape = itemShape,
                    itemTestTagPrefix = itemTestTagPrefix,
                    enabled = enabled,
                    optionTrailingContent = optionTrailingContent,
                )
            } else {
                SegmentedTogglePill(
                    options = options,
                    selectedIndex = selectedIndex,
                    pendingIndex = pendingIndex,
                    onSelect = onSelect,
                    itemShape = itemShape,
                    itemTestTagPrefix = itemTestTagPrefix,
                    enabled = enabled,
                    optionTrailingContent = optionTrailingContent,
                )
            }
        }
    }
}

@Composable
private fun SegmentedToggleGrid(
    options: List<String>,
    selectedIndex: Int,
    pendingIndex: Int?,
    onSelect: (Int) -> Unit,
    itemShape: Shape,
    itemTestTagPrefix: String?,
    enabled: Boolean,
    optionTrailingContent: @Composable (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(ComponentDimens.CompactSpacing)) {
        for (rowStart in options.indices step 2) {
            SegmentedToggleGridRow(
                options = options,
                rowStart = rowStart,
                selectedIndex = selectedIndex,
                pendingIndex = pendingIndex,
                onSelect = onSelect,
                itemShape = itemShape,
                itemTestTagPrefix = itemTestTagPrefix,
                enabled = enabled,
                optionTrailingContent = optionTrailingContent,
            )
        }
    }
}

@Composable
private fun SegmentedToggleGridRow(
    options: List<String>,
    rowStart: Int,
    selectedIndex: Int,
    pendingIndex: Int?,
    onSelect: (Int) -> Unit,
    itemShape: Shape,
    itemTestTagPrefix: String?,
    enabled: Boolean,
    optionTrailingContent: @Composable (Int) -> Unit,
) {
    // Saman rivin vaihtoehdot ovat yhtä korkeita, vaikka toisessa on merkki nimen alla.
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(ComponentDimens.CompactSpacing),
    ) {
        for (index in rowStart until minOf(rowStart + 2, options.size)) {
            // CPD-OFF: Jaettu valintaelementti saa samat argumentit ruudukossa ja pillissä; rivien asettelu eroaa.
            SegmentedToggleItem(
                label = options[index],
                isSelected = index == selectedIndex,
                isPending = index == pendingIndex,
                onClick = { onSelect(index) },
                shape = itemShape,
                modifier = Modifier.weight(1f).fillMaxHeight().itemTestTag(itemTestTagPrefix, index),
                enabled = enabled,
                trailingContent = { optionTrailingContent(index) },
                stackTrailing = true,
            )
            // CPD-ON
        }
    }
}

@Composable
private fun SegmentedTogglePill(
    options: List<String>,
    selectedIndex: Int,
    pendingIndex: Int?,
    onSelect: (Int) -> Unit,
    itemShape: Shape,
    itemTestTagPrefix: String?,
    enabled: Boolean,
    optionTrailingContent: @Composable (Int) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, label ->
            SegmentedToggleItem(
                label = label,
                isSelected = index == selectedIndex,
                isPending = index == pendingIndex,
                onClick = { onSelect(index) },
                shape = itemShape,
                modifier = Modifier.weight(1f).itemTestTag(itemTestTagPrefix, index),
                enabled = enabled,
                trailingContent = { optionTrailingContent(index) },
            )
        }
    }
}

@Composable
private fun SegmentedToggleItem(
    label: String,
    isSelected: Boolean,
    isPending: Boolean,
    onClick: () -> Unit,
    shape: Shape,
    modifier: Modifier = Modifier,
    enabled: Boolean,
    trailingContent: @Composable () -> Unit,
    // Ruudukossa merkki (esim. PRO tai TRIAL) on nimen alla: rinnalla se kavensi nimen kahdelle riville.
    stackTrailing: Boolean = false,
) {
    val primary = MaterialTheme.colorScheme.primary
    val selectionAppearance =
        when {
            isSelected -> {
                Modifier.background(primary)
            }

            isPending -> {
                val description = stringResource(R.string.segmented_option_pending_state)
                Modifier.border(1.5.dp, primary, shape).semantics { stateDescription = description }
            }

            else -> {
                Modifier
            }
        }
    val itemModifier =
        modifier
            .heightIn(min = ComponentDimens.SegmentedItemMinHeight)
            .clip(shape)
            .then(selectionAppearance)
            .selectable(
                selected = isSelected,
                enabled = enabled,
                onClick = onClick,
                role = Role.RadioButton,
            )
    // Sama merkkisisältö kahdessa asettelussa: movableContentOf säilyttää sen tilan haarasta riippumatta.
    val trailing = remember(trailingContent) { movableContentOf(trailingContent) }
    if (stackTrailing) {
        Column(
            modifier = itemModifier.padding(vertical = ComponentDimens.CompactSpacing),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ComponentDimens.CompactSpacing, Alignment.CenterVertically),
        ) {
            SegmentedToggleLabel(label, isSelected, Modifier.fillMaxWidth())
            trailing()
        }
    } else {
        Row(modifier = itemModifier, verticalAlignment = Alignment.CenterVertically) {
            SegmentedToggleLabel(label, isSelected, Modifier.weight(1f))
            trailing()
        }
    }
}

@Composable
private fun SegmentedToggleLabel(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
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
            LocalContentColor.current
        }
    BasicText(
        modifier =
            modifier.padding(
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

// Laitetestien tunniste vaihtoehdolle: sama "_option_n"-muoto kuin aiemmissa pudotusvalikoissa.
private fun Modifier.itemTestTag(
    prefix: String?,
    index: Int,
): Modifier = if (prefix == null) this else testTag("${prefix}_option_$index")
