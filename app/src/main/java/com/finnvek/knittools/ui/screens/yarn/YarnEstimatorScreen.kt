package com.finnvek.knittools.ui.screens.yarn

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.finnvek.knittools.domain.calculator.YarnEstimator
import com.finnvek.knittools.ui.components.NumberInputField
import com.finnvek.knittools.ui.components.ResultCard
import com.finnvek.knittools.ui.components.ToolScreenScaffold
import com.finnvek.knittools.ui.components.UnitToggle

@Composable
fun YarnEstimatorScreen(onBack: () -> Unit) {
    var totalYarn by remember { mutableStateOf("") }
    var yarnPerSkein by remember { mutableStateOf("") }
    var weightPerSkein by remember { mutableStateOf("") }
    var useImperial by remember { mutableStateOf(false) }

    val result by remember(totalYarn, yarnPerSkein, weightPerSkein) {
        derivedStateOf {
            val total = totalYarn.toDoubleOrNull() ?: return@derivedStateOf null
            val perSkein = yarnPerSkein.toDoubleOrNull() ?: return@derivedStateOf null
            val weight = weightPerSkein.toDoubleOrNull() ?: return@derivedStateOf null
            if (total <= 0 || perSkein <= 0 || weight <= 0) return@derivedStateOf null
            YarnEstimator.estimate(total, perSkein, weight)
        }
    }

    ToolScreenScaffold(title = "Yarn Estimator", onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            UnitToggle(useImperial = useImperial, onToggle = { useImperial = it })
            val lengthUnit = if (useImperial) "yards" else "meters"
            NumberInputField(
                value = totalYarn,
                onValueChange = { totalYarn = it },
                label = "Total yarn needed",
                isDecimal = true,
                suffix = lengthUnit,
                modifier = Modifier.fillMaxWidth(),
            )
            NumberInputField(
                value = yarnPerSkein,
                onValueChange = { yarnPerSkein = it },
                label = "$lengthUnit per skein",
                isDecimal = true,
                suffix = lengthUnit,
                modifier = Modifier.fillMaxWidth(),
            )
            NumberInputField(
                value = weightPerSkein,
                onValueChange = { weightPerSkein = it },
                label = "Weight per skein",
                isDecimal = true,
                suffix = "g",
                modifier = Modifier.fillMaxWidth(),
            )

            result?.let { r ->
                ResultCard(title = "Result") {
                    Text(
                        text = "${r.skeinsNeeded} skeins",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        text = "Total weight: ${"%.0f".format(r.totalWeight)} g",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "Exact: ${"%.2f".format(r.exactSkeins)} skeins",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text = "Always buy one extra skein — dye lots may vary",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
