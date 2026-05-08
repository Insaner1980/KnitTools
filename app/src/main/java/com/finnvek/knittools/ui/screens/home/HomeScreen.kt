package com.finnvek.knittools.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.knittools.R
import com.finnvek.knittools.pro.ProStatus
import com.finnvek.knittools.ui.components.HubListItem
import com.finnvek.knittools.ui.components.QuickTipCard
import com.finnvek.knittools.ui.navigation.Screen
import com.finnvek.knittools.ui.theme.RavelryTeal
import com.finnvek.knittools.ui.theme.knitToolsColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigate: (Screen) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val proState by viewModel.proState.collectAsStateWithLifecycle()
    val showTips by viewModel.showTips.collectAsStateWithLifecycle()
    val currentTip by viewModel.currentTip.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.tools_title),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                    ),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier =
                    Modifier
                        .widthIn(max = 600.dp)
                        .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Pro trial info
                if (proState.status == ProStatus.TRIAL_ACTIVE) {
                    item {
                        Text(
                            text = stringResource(R.string.pro_trial_days_left, proState.trialDaysRemaining),
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.knitToolsColors.brandWine,
                        )
                    }
                }

                // Trial expired
                if (proState.status == ProStatus.TRIAL_EXPIRED) {
                    item {
                        Text(
                            text = stringResource(R.string.unlock_all_tools),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onNavigate(Screen.ProUpgrade) }
                                    .padding(vertical = 8.dp),
                        )
                    }
                }

                // Laskimet — suora lista
                item {
                    HubListItem(
                        title = stringResource(R.string.tool_gauge_converter),
                        description = stringResource(R.string.desc_gauge_calculator),
                        onClick = { onNavigate(Screen.Gauge) },
                        titleColor = MaterialTheme.colorScheme.primary,
                    )
                }
                item {
                    HubListItem(
                        title = stringResource(R.string.tool_increase_decrease),
                        description = stringResource(R.string.desc_increase_decrease),
                        onClick = { onNavigate(Screen.IncreaseDecrease) },
                        titleColor = MaterialTheme.colorScheme.secondary,
                    )
                }
                item {
                    HubListItem(
                        title = stringResource(R.string.tool_cast_on_calculator),
                        description = stringResource(R.string.desc_cast_on),
                        onClick = { onNavigate(Screen.CastOn) },
                        titleColor = MaterialTheme.colorScheme.tertiary,
                    )
                }
                item {
                    HubListItem(
                        title = stringResource(R.string.tool_yarn_estimator),
                        description = stringResource(R.string.desc_yarn_estimator_card),
                        onClick = { onNavigate(Screen.Yarn) },
                        titleColor = MaterialTheme.knitToolsColors.brandWine,
                    )
                }
                item {
                    HubListItem(
                        title = stringResource(R.string.tool_ravelry),
                        description = stringResource(R.string.desc_ravelry),
                        onClick = { onNavigate(Screen.Ravelry) },
                        titleColor = RavelryTeal,
                    )
                }

                // Erotin + Quick Tip
                if (showTips) {
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
    }
}
