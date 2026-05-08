package com.finnvek.knittools.ui.screens.sizecharts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.calculator.SizeChartData
import com.finnvek.knittools.domain.model.SizeChartEntry
import com.finnvek.knittools.ui.components.ToolScreenScaffold
import com.finnvek.knittools.ui.screens.home.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SizeChartScreen(
    onBack: () -> Unit,
    homeViewModel: HomeViewModel = hiltViewModel(),
) {
    var selectedCategory by rememberSaveable { mutableStateOf(SizeChartData.Category.WOMEN) }
    val useImperial by homeViewModel.useImperial.collectAsStateWithLifecycle()
    var dropdownExpanded by remember { mutableStateOf(false) }

    val headers = remember(selectedCategory) { SizeChartData.headers(selectedCategory) }
    val entries = remember(selectedCategory) { SizeChartData.entries(selectedCategory) }

    val categoryLabels =
        mapOf(
            SizeChartData.Category.BABY to stringResource(R.string.category_baby),
            SizeChartData.Category.CHILD_YOUTH to stringResource(R.string.category_child_youth),
            SizeChartData.Category.WOMEN to stringResource(R.string.category_women),
            SizeChartData.Category.MEN to stringResource(R.string.category_men),
            SizeChartData.Category.HEAD to stringResource(R.string.category_head),
            SizeChartData.Category.HAND to stringResource(R.string.category_hand),
        )

    ToolScreenScaffold(
        title = stringResource(R.string.size_charts_title),
        onBack = onBack,
    ) { padding ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Kategoria-dropdown
            item {
                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded,
                    onExpandedChange = { dropdownExpanded = it },
                ) {
                    TextField(
                        value = categoryLabels[selectedCategory] ?: "",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                        modifier =
                            Modifier
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                        colors =
                            TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                            ),
                    )
                    ExposedDropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                    ) {
                        SizeChartData.Category.entries.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(categoryLabels[category] ?: category.name) },
                                onClick = {
                                    selectedCategory = category
                                    dropdownExpanded = false
                                },
                            )
                        }
                    }
                }
            }

            // Taulukko-otsikko
            item { SizeChartHeaderRow(headers) }

            // Data-rivit
            items(entries) { entry ->
                SizeChartDataRow(entry, useImperial)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun SizeChartHeaderRow(headerResIds: List<Int>) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .padding(vertical = 8.dp),
    ) {
        headerResIds.forEach { resId ->
            Text(
                text = stringResource(resId),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun SizeChartDataRow(
    entry: SizeChartEntry,
    useImperial: Boolean,
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
    ) {
        // Ensimmäinen sarake = koon nimi
        Text(
            text = entry.sizeLabel.resolve(context),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        // Loput sarakkeet = mitat
        entry.measurements.forEach { measurement ->
            val value = if (useImperial) measurement.inches else measurement.cm
            val formatted =
                if (value == value.toLong().toDouble()) {
                    value.toLong().toString()
                } else {
                    "%.1f".format(value)
                }
            Text(
                text = formatted,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}
