package com.finnvek.knittools.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.knittools.R
import com.finnvek.knittools.pro.ProStatus
import com.finnvek.knittools.ui.navigation.Screen

data class ToolCardData(
    val titleRes: Int,
    val imageRes: Int,
    val screen: Screen,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigate: (Screen) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val proState by viewModel.proState.collectAsStateWithLifecycle()

    val tools =
        listOf(
            ToolCardData(R.string.tool_row_counter, R.drawable.row_counter, Screen.Counter),
            ToolCardData(R.string.tool_increase_decrease, R.drawable.increase_decrease_calculator, Screen.IncreaseDecrease),
            ToolCardData(R.string.tool_gauge_converter, R.drawable.gauge_converter, Screen.Gauge),
            ToolCardData(R.string.tool_cast_on_calculator, R.drawable.cast_on_calculator, Screen.CastOn),
            ToolCardData(R.string.tool_yarn_estimator, R.drawable.yarn_estimator, Screen.Yarn),
            ToolCardData(R.string.tool_needle_sizes, R.drawable.needle_sizes, Screen.Needles),
        )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = { onNavigate(Screen.Settings) }) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings))
                    }
                },
            )
        },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (proState.status) {
                ProStatus.TRIAL_ACTIVE -> {
                    item(span = { GridItemSpan(2) }) {
                        Text(
                            text = stringResource(R.string.pro_trial_days_left, proState.trialDaysRemaining),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                ProStatus.TRIAL_EXPIRED -> {
                    item(span = { GridItemSpan(2) }) {
                        Text(
                            text = stringResource(R.string.unlock_all_tools),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onNavigate(Screen.ProUpgrade) },
                        )
                    }
                }

                ProStatus.PRO_PURCHASED -> { /* no indicator */ }
            }

            items(tools, key = { it.titleRes }) { tool ->
                val title = stringResource(tool.titleRes)
                ToolCard(
                    title = title,
                    imageRes = tool.imageRes,
                    onClick = { onNavigate(tool.screen) },
                )
            }
        }
    }
}

@Composable
private fun ToolCard(
    title: String,
    imageRes: Int,
    onClick: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)),
                            ),
                        ),
            )
            Text(
                text = title,
                modifier =
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
