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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finnvek.knittools.R
import com.finnvek.knittools.ui.navigation.Screen

data class ToolCardData(
    val title: String,
    val imageRes: Int,
    val screen: Screen,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onNavigate: (Screen) -> Unit) {
    val tools =
        listOf(
            ToolCardData("Row Counter", R.drawable.row_counter, Screen.Counter),
            ToolCardData("Increase / Decrease", R.drawable.increase_decrease_calculator, Screen.IncreaseDecrease),
            ToolCardData("Gauge Converter", R.drawable.gauge_converter, Screen.Gauge),
            ToolCardData("Cast On Calculator", R.drawable.cast_on_calculator, Screen.CastOn),
            ToolCardData("Yarn Estimator", R.drawable.yarn_estimator, Screen.Yarn),
            ToolCardData("Needle Sizes", R.drawable.needle_sizes, Screen.Needles),
        )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("KnitTools") },
                actions = {
                    IconButton(onClick = { onNavigate(Screen.Settings) }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
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
            items(tools, key = { it.title }) { tool ->
                ToolCard(
                    title = tool.title,
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
