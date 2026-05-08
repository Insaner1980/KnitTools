package com.finnvek.knittools.ui.screens.needles

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.calculator.NeedleSizeData
import com.finnvek.knittools.domain.model.NeedleSize
import com.finnvek.knittools.ui.components.InfoTip
import com.finnvek.knittools.ui.components.SearchTextField
import com.finnvek.knittools.ui.components.ToolScreenScaffold

@Composable
fun NeedleSizeScreen(onBack: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedMm by rememberSaveable { mutableStateOf<Double?>(null) }
    val results = remember(query) { NeedleSizeData.search(query) }

    ToolScreenScaffold(
        title = stringResource(R.string.tool_needle_sizes),
        onBack = onBack,
    ) { padding ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
        ) {
            item {
                SearchTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = stringResource(R.string.search_size),
                )
            }
            item {
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.needle_size_disclaimer),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    InfoTip(
                        title = stringResource(R.string.tip_needle_sizes_title),
                        description = stringResource(R.string.tip_needle_sizes_desc),
                    )
                }
            }
            item { HeaderRow() }
            if (results.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.no_results_found),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                }
            } else {
                items(results, key = { it.metricMm }) { needle ->
                    NeedleRow(
                        needle = needle,
                        isSelected = needle.metricMm == selectedMm,
                        onClick = { selectedMm = needle.metricMm },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun HeaderRow() {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        listOf(
            R.string.header_mm,
            R.string.header_us,
            R.string.header_uk,
            R.string.header_jp,
        ).forEach { headerRes ->
            Text(
                text = stringResource(headerRes),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun NeedleRow(
    needle: NeedleSize,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val bgColor =
        if (isSelected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        } else {
            Color.Transparent
        }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .background(bgColor)
                .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        listOf(
            needle.metricMm.toString(),
            needle.us,
            needle.ukCanadian,
            needle.japanese,
        ).forEach { value ->
            Text(
                text = value,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
