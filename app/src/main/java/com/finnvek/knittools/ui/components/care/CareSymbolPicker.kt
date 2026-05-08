package com.finnvek.knittools.ui.components.care

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.finnvek.knittools.R

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CareSymbolPicker(
    careSymbols: Long,
    onToggle: (CareSymbol) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.care_symbols),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )

        CareCategory.entries.forEach { category ->
            val symbols = CareSymbol.entries.filter { it.category == category }
            val categoryLabel =
                when (category) {
                    CareCategory.WASHING -> stringResource(R.string.care_category_washing)
                    CareCategory.BLEACHING -> stringResource(R.string.care_category_bleaching)
                    CareCategory.DRYING -> stringResource(R.string.care_category_drying)
                    CareCategory.IRONING -> stringResource(R.string.care_category_ironing)
                    CareCategory.DRY_CLEANING -> stringResource(R.string.care_category_dry_cleaning)
                }

            Text(
                text = categoryLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                symbols.forEach { symbol ->
                    val isSelected = careSymbols.hasCareSymbol(symbol)
                    CareChip(
                        symbol = symbol,
                        isSelected = isSelected,
                        onClick = { onToggle(symbol) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CareChip(
    symbol: CareSymbol,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val chipShape = MaterialTheme.shapes.small
    val borderColor =
        if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outlineVariant
        }
    val tint =
        if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }

    Row(
        modifier =
            Modifier
                .clip(chipShape)
                .border(1.dp, borderColor, chipShape)
                .clickable(onClick = onClick)
                .heightIn(min = 48.dp)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        CareSymbolIcon(
            symbol = symbol,
            modifier = Modifier.size(24.dp),
            tint = tint,
        )
        Text(
            text = symbol.label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
        )
    }
}
