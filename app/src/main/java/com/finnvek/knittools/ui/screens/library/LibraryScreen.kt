package com.finnvek.knittools.ui.screens.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.knittools.R
import com.finnvek.knittools.ui.components.HubListItem
import com.finnvek.knittools.ui.components.QuickTipCard
import com.finnvek.knittools.ui.navigation.Screen
import com.finnvek.knittools.ui.theme.knitToolsColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onNavigate: (Screen) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val savedPatternCount by viewModel.savedPatternCount.collectAsStateWithLifecycle(initialValue = 0)
    val yarnCardCount by viewModel.yarnCardCount.collectAsStateWithLifecycle(initialValue = 0)
    val photoCount by viewModel.photoCount.collectAsStateWithLifecycle(initialValue = 0)

    val tips = stringArrayResource(R.array.knitting_tips)
    val currentTip = remember { tips.random() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.library_title),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // MY COLLECTION -osio
            item {
                Text(
                    text = stringResource(R.string.library_my_collection),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.knitToolsColors.brandWine,
                )
            }
            item {
                HubListItem(
                    title = stringResource(R.string.saved_patterns_title),
                    description = stringResource(R.string.desc_saved_patterns),
                    onClick = { onNavigate(Screen.SavedPatterns) },
                    titleColor = MaterialTheme.colorScheme.primary,
                    trailingText = savedPatternCount.toString(),
                )
            }
            item {
                HubListItem(
                    title = stringResource(R.string.my_yarn_title),
                    description = stringResource(R.string.desc_my_yarn),
                    onClick = { onNavigate(Screen.MyYarn) },
                    titleColor = MaterialTheme.colorScheme.secondary,
                    trailingText = yarnCardCount.toString(),
                )
            }
            item {
                HubListItem(
                    title = stringResource(R.string.all_photos_title),
                    description = stringResource(R.string.desc_all_photos),
                    onClick = { onNavigate(Screen.AllPhotos) },
                    titleColor = MaterialTheme.colorScheme.tertiary,
                    trailingText = photoCount.toString(),
                )
            }

            // REFERENCE -osio
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.library_reference),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.knitToolsColors.brandWine,
                )
            }
            item {
                HubListItem(
                    title = stringResource(R.string.tool_needle_sizes),
                    description = stringResource(R.string.desc_needle_sizes),
                    onClick = { onNavigate(Screen.Needles) },
                    titleColor = MaterialTheme.colorScheme.primary,
                )
            }
            item {
                HubListItem(
                    title = stringResource(R.string.size_charts_title),
                    description = stringResource(R.string.desc_size_charts),
                    onClick = { onNavigate(Screen.SizeCharts) },
                    titleColor = MaterialTheme.colorScheme.secondary,
                )
            }
            item {
                HubListItem(
                    title = stringResource(R.string.abbreviations_title),
                    description = stringResource(R.string.desc_abbreviations),
                    onClick = { onNavigate(Screen.Abbreviations) },
                    titleColor = MaterialTheme.colorScheme.tertiary,
                )
            }
            item {
                HubListItem(
                    title = stringResource(R.string.chart_symbols_title),
                    description = stringResource(R.string.desc_chart_symbols),
                    onClick = { onNavigate(Screen.ChartSymbols) },
                    titleColor = MaterialTheme.knitToolsColors.brandWine,
                )
            }

            item {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            item {
                QuickTipCard(tipText = currentTip)
            }
        }
    }
}
