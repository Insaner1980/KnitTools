package com.finnvek.knittools.ui.screens.abbreviations

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.calculator.AbbreviationData
import com.finnvek.knittools.domain.model.CraftType
import com.finnvek.knittools.domain.model.KnittingAbbreviation
import com.finnvek.knittools.ui.components.SearchTextField
import com.finnvek.knittools.ui.components.ToolScreenScaffold
import com.finnvek.knittools.ui.theme.knitToolsColors

@Composable
fun AbbreviationsScreen(
    craftType: CraftType = CraftType.KNITTING,
    onBack: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var expandedAbbreviation by rememberSaveable { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val results = remember(query, context, craftType) { AbbreviationData.search(context, query, craftType) }

    ToolScreenScaffold(
        title = stringResource(R.string.abbreviations_title),
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
                    modifier = Modifier.padding(bottom = 8.dp),
                    value = query,
                    onValueChange = { query = it },
                    label = stringResource(R.string.search_abbreviation),
                    // CPD-OFF: Ruudun paikallinen Compose-rakenne pidetaan vastuun yhteydessa.
                )
            }
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
                items(results, key = { it.abbreviation }) { abbreviation ->
                    // CPD-ON
                    AbbreviationItem(
                        abbreviation = abbreviation,
                        isExpanded = expandedAbbreviation == abbreviation.abbreviation,
                        onClick = {
                            expandedAbbreviation =
                                if (expandedAbbreviation ==
                                    abbreviation.abbreviation
                                ) {
                                    null
                                } else {
                                    abbreviation.abbreviation
                                }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun AbbreviationItem(
    abbreviation: KnittingAbbreviation,
    isExpanded: Boolean,
    onClick: () -> Unit,
) {
    // Hiusviivarivi kuten Chart Symbolsissa: kortti per lyhenne mahdutti ruudulle vain kuusi riviä.
    Column(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            Text(
                text = abbreviation.abbreviation,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.knitToolsColors.primaryReadable,
            )
            Text(
                text = stringResource(abbreviation.meaningResId),
                style = MaterialTheme.typography.bodyMedium,
            )
            AnimatedVisibility(visible = isExpanded) {
                Text(
                    text = stringResource(abbreviation.descriptionResId),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}
