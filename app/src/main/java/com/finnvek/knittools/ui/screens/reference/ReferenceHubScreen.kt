package com.finnvek.knittools.ui.screens.reference

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.finnvek.knittools.R
import com.finnvek.knittools.ui.components.HubListItem
import com.finnvek.knittools.ui.components.ToolScreenScaffold
import com.finnvek.knittools.ui.navigation.Screen

@Composable
fun ReferenceHubScreen(
    onNavigate: (Screen) -> Unit,
    onBack: () -> Unit,
) {
    ToolScreenScaffold(
        title = stringResource(R.string.reference_hub_title),
        onBack = onBack,
    ) { padding ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column {
                    Text(
                        text = stringResource(R.string.reference_hero_title),
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.reference_hero_subtitle),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
            item {
                HubListItem(
                    icon = Icons.Filled.Straighten,
                    title = stringResource(R.string.tool_needle_sizes),
                    description = stringResource(R.string.desc_needle_sizes),
                    onClick = { onNavigate(Screen.Needles) },
                )
            }
            item {
                HubListItem(
                    icon = Icons.Filled.TableChart,
                    title = stringResource(R.string.size_charts_title),
                    description = stringResource(R.string.desc_size_charts),
                    onClick = { onNavigate(Screen.SizeCharts) },
                )
            }
            item {
                HubListItem(
                    icon = Icons.Filled.TextSnippet,
                    title = stringResource(R.string.abbreviations_title),
                    description = stringResource(R.string.desc_abbreviations),
                    onClick = { onNavigate(Screen.Abbreviations) },
                )
            }
            item {
                HubListItem(
                    icon = Icons.Filled.GridOn,
                    title = stringResource(R.string.chart_symbols_title),
                    description = stringResource(R.string.desc_chart_symbols),
                    onClick = { onNavigate(Screen.ChartSymbols) },
                )
            }
        }
    }
}
