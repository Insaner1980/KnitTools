package com.finnvek.knittools.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.outlined.Functions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        if (proState.status == ProStatus.TRIAL_ACTIVE) {
                            Text(
                                text = stringResource(R.string.pro_trial_days_left, proState.trialDaysRemaining),
                                style = MaterialTheme.typography.bodyMedium,
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigate(Screen.Settings) }) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.settings),
                            modifier = Modifier.padding(0.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
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
                        icon = Icons.Filled.Straighten,
                        title = stringResource(R.string.tool_gauge_converter),
                        description = stringResource(R.string.desc_gauge_calculator),
                        onClick = { onNavigate(Screen.Gauge) },
                    )
                }
                item {
                    HubListItem(
                        icon = Icons.Filled.SwapVert,
                        title = stringResource(R.string.tool_increase_decrease),
                        description = stringResource(R.string.desc_increase_decrease),
                        onClick = { onNavigate(Screen.IncreaseDecrease) },
                    )
                }
                item {
                    HubListItem(
                        icon = Icons.Filled.Calculate,
                        title = stringResource(R.string.tool_cast_on_calculator),
                        description = stringResource(R.string.desc_cast_on),
                        onClick = { onNavigate(Screen.CastOn) },
                    )
                }
                item {
                    HubListItem(
                        icon = Icons.Outlined.Functions,
                        title = stringResource(R.string.tool_yarn_estimator),
                        description = stringResource(R.string.desc_yarn_estimator_card),
                        onClick = { onNavigate(Screen.Yarn) },
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
