package com.finnvek.knittools.ui.screens.yarn

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.finnvek.knittools.R
import com.finnvek.knittools.ai.ocr.YarnLabelScanner
import com.finnvek.knittools.domain.calculator.YarnEstimator
import com.finnvek.knittools.ui.components.NumberInputField
import com.finnvek.knittools.ui.components.ResultCard
import com.finnvek.knittools.ui.components.ToolScreenScaffold
import com.finnvek.knittools.ui.components.UnitToggle
import com.finnvek.knittools.ui.screens.yarncard.YarnCardViewModel
import com.finnvek.knittools.util.extensions.convertFieldValue
import kotlinx.coroutines.launch

@Composable
fun YarnEstimatorScreen(
    onBack: () -> Unit,
    onScanLabel: () -> Unit = {},
    onSavedYarns: () -> Unit = {},
    yarnCardViewModel: YarnCardViewModel? = null,
) {
    var totalYarn by remember { mutableStateOf("") }
    var yarnPerSkein by remember { mutableStateOf("") }
    var weightPerSkein by remember { mutableStateOf("") }
    var useImperial by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isPro = yarnCardViewModel?.isPro ?: false

    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success ->
        if (success && pendingPhotoUri != null) {
            scope.launch {
                yarnCardViewModel?.setScanning(true)
                val parsed = YarnLabelScanner.analyzeImage(context, pendingPhotoUri!!)
                yarnCardViewModel?.loadFromScan(parsed, pendingPhotoUri)
                yarnCardViewModel?.setScanning(false)
                onScanLabel()
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            val (_, uri) = YarnLabelScanner.createImageFile(context)
            pendingPhotoUri = uri
            cameraLauncher.launch(uri)
        }
    }

    val result by remember(totalYarn, yarnPerSkein, weightPerSkein) {
        derivedStateOf {
            val total = totalYarn.toDoubleOrNull() ?: return@derivedStateOf null
            val perSkein = yarnPerSkein.toDoubleOrNull() ?: return@derivedStateOf null
            val weight = weightPerSkein.toDoubleOrNull() ?: return@derivedStateOf null
            if (total <= 0 || perSkein <= 0 || weight <= 0) return@derivedStateOf null
            YarnEstimator.estimate(total, perSkein, weight)
        }
    }

    ToolScreenScaffold(title = stringResource(R.string.tool_yarn_estimator), onBack = onBack) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (isPro) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    IconButton(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                        Icon(
                            Icons.Filled.CameraAlt,
                            contentDescription = stringResource(R.string.scan_yarn_label),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    IconButton(onClick = onSavedYarns) {
                        Icon(
                            Icons.Filled.Inventory2,
                            contentDescription = stringResource(R.string.saved_yarns),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            UnitToggle(
                useImperial = useImperial,
                onToggle = { newImperial ->
                    if (newImperial != useImperial) {
                        totalYarn = convertFieldValue(totalYarn, newImperial, isLength = false)
                        yarnPerSkein = convertFieldValue(yarnPerSkein, newImperial, isLength = false)
                        useImperial = newImperial
                    }
                },
            )
            val lengthUnit = if (useImperial) stringResource(R.string.unit_yards) else stringResource(R.string.unit_meters)
            NumberInputField(
                value = totalYarn,
                onValueChange = { totalYarn = it },
                label = stringResource(R.string.total_yarn_needed),
                isDecimal = true,
                suffix = lengthUnit,
                modifier = Modifier.fillMaxWidth(),
            )
            NumberInputField(
                value = yarnPerSkein,
                onValueChange = { yarnPerSkein = it },
                label = stringResource(R.string.yarn_per_skein, lengthUnit),
                isDecimal = true,
                suffix = lengthUnit,
                modifier = Modifier.fillMaxWidth(),
            )
            NumberInputField(
                value = weightPerSkein,
                onValueChange = { weightPerSkein = it },
                label = stringResource(R.string.weight_per_skein),
                isDecimal = true,
                suffix = stringResource(R.string.unit_g),
                modifier = Modifier.fillMaxWidth(),
            )

            result?.let { r ->
                ResultCard(title = stringResource(R.string.result)) {
                    Text(
                        text = stringResource(R.string.skeins_result, r.skeinsNeeded),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        text = stringResource(R.string.total_weight, "%.0f".format(r.totalWeight)),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = stringResource(R.string.exact_skeins, "%.2f".format(r.exactSkeins)),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text = stringResource(R.string.extra_skein_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
