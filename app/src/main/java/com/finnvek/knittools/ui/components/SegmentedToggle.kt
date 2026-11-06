package com.finnvek.knittools.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
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
    optionTrailingContent: @Composable (Int) -> Unit = {},
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
                optionTrailingContent = optionTrailingContent,
            )
        } else {
            SegmentedTogglePill(
                options = options,
                selectedIndex = selectedIndex,
                onSelect = onSelect,
                itemShape = itemShape,
                itemTestTagPrefix = itemTestTagPrefix,
                optionTrailingContent = optionTrailingContent,
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
    optionTrailingContent: @Composable (Int) -> Unit,
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
    onSelect: (Int) -> Unit,
    itemShape: Shape,
    itemTestTagPrefix: String?,
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
                onClick = { onSelect(index) },
                shape = itemShape,
                modifier = Modifier.weight(1f).fillMaxHeight().itemTestTag(itemTestTagPrefix, index),
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
    onSelect: (Int) -> Unit,
    itemShape: Shape,
    itemTestTagPrefix: String?,
    optionTrailingContent: @Composable (Int) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, label ->
            SegmentedToggleItem(
                label = label,
                isSelected = index == selectedIndex,
                onClick = { onSelect(index) },
                shape = itemShape,
                modifier = Modifier.weight(1f).itemTestTag(itemTestTagPrefix, index),
                trailingContent = { optionTrailingContent(index) },
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
    trailingContent: @Composable () -> Unit,
    // Ruudukossa merkki (esim. PRO tai TRIAL) on nimen alla: rinnalla se kavensi nimen kahdelle riville.
    stackTrailing: Boolean = false,
) {
    val itemModifier =
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
            MaterialTheme.colorScheme.onSurfaceVariant
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
