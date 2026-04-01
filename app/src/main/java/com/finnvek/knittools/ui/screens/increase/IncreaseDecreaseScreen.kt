package com.finnvek.knittools.ui.screens.increase

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.finnvek.knittools.domain.calculator.IncreaseDecreaseCalculator
import com.finnvek.knittools.domain.model.IncreaseDecreaseMode
import com.finnvek.knittools.domain.model.KnittingStyle
import com.finnvek.knittools.ui.components.NumberInputField
import com.finnvek.knittools.ui.components.ResultCard
import com.finnvek.knittools.ui.components.ToolScreenScaffold

@Composable
fun IncreaseDecreaseScreen(onBack: () -> Unit) {
    var currentStitches by remember { mutableStateOf("") }
    var changeBy by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(IncreaseDecreaseMode.INCREASE) }
    var style by remember { mutableStateOf(KnittingStyle.FLAT) }

    val result by remember(currentStitches, changeBy, mode, style) {
        derivedStateOf {
            val current = currentStitches.toIntOrNull() ?: return@derivedStateOf null
            val change = changeBy.toIntOrNull() ?: return@derivedStateOf null
            IncreaseDecreaseCalculator.calculate(current, change, mode, style)
        }
    }

    ToolScreenScaffold(title = "Increase / Decrease", onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = mode == IncreaseDecreaseMode.INCREASE,
                    onClick = { mode = IncreaseDecreaseMode.INCREASE },
                    label = { Text("Increase") },
                )
                FilterChip(
                    selected = mode == IncreaseDecreaseMode.DECREASE,
                    onClick = { mode = IncreaseDecreaseMode.DECREASE },
                    label = { Text("Decrease") },
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = style == KnittingStyle.FLAT,
                    onClick = { style = KnittingStyle.FLAT },
                    label = { Text("Flat") },
                )
                FilterChip(
                    selected = style == KnittingStyle.CIRCULAR,
                    onClick = { style = KnittingStyle.CIRCULAR },
                    label = { Text("Circular") },
                )
            }

            NumberInputField(
                value = currentStitches,
                onValueChange = { currentStitches = it },
                label = "Current stitches",
                suffix = "st",
                modifier = Modifier.fillMaxWidth(),
            )
            val changeLabel = if (mode == IncreaseDecreaseMode.INCREASE) "Increase by" else "Decrease by"
            NumberInputField(
                value = changeBy,
                onValueChange = { changeBy = it },
                label = changeLabel,
                suffix = "st",
                modifier = Modifier.fillMaxWidth(),
            )

            result?.let { r ->
                if (!r.isValid) {
                    Text(
                        text = r.errorMessage ?: "Invalid input",
                        color = MaterialTheme.colorScheme.error,
                    )
                } else {
                    ResultCard(title = "Easy to remember") {
                        Text(text = r.easyPattern, style = MaterialTheme.typography.bodyLarge)
                    }
                    ResultCard(title = "Balanced") {
                        Text(text = r.balancedPattern, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}
