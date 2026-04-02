package com.finnvek.knittools.ui.screens.yarn

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.knittools.R
import com.finnvek.knittools.ai.ocr.YarnLabelScanner
import com.finnvek.knittools.domain.calculator.YarnEstimator
import com.finnvek.knittools.domain.model.YarnEstimate
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
    var totalYarn by rememberSaveable { mutableStateOf("") }
    var yarnPerSkein by rememberSaveable { mutableStateOf("") }
    var weightPerSkein by rememberSaveable { mutableStateOf("") }
    var useImperial by rememberSaveable { mutableStateOf(false) }
    val isPro = yarnCardViewModel?.isPro ?: false

    val result by remember(totalYarn, yarnPerSkein, weightPerSkein) {
        derivedStateOf {
            val total = totalYarn.toDoubleOrNull() ?: return@derivedStateOf null
            val perSkein = yarnPerSkein.toDoubleOrNull() ?: return@derivedStateOf null
            val weight = weightPerSkein.toDoubleOrNull() ?: return@derivedStateOf null
            if (total <= 0 || perSkein <= 0 || weight <= 0) return@derivedStateOf null
            YarnEstimator.estimate(total, perSkein, weight)
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val formState = yarnCardViewModel?.formState?.collectAsStateWithLifecycle()

    ToolScreenScaffold(title = stringResource(R.string.tool_yarn_estimator), onBack = onBack) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (formState?.value?.isScanning == true) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.scanning),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                if (isPro) {
                    ProActionBar(
                        yarnCardViewModel = yarnCardViewModel!!,
                        onScanLabel = onScanLabel,
                        onSavedYarns = onSavedYarns,
                        snackbarHostState = snackbarHostState,
                    )
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

                val lengthUnit =
                    if (useImperial) {
                        stringResource(
                            R.string.unit_yards,
                        )
                    } else {
                        stringResource(R.string.unit_meters)
                    }
                YarnInputFields(
                    totalYarn = totalYarn,
                    yarnPerSkein = yarnPerSkein,
                    weightPerSkein = weightPerSkein,
                    lengthUnit = lengthUnit,
                    onTotalYarnChange = { totalYarn = it },
                    onYarnPerSkeinChange = { yarnPerSkein = it },
                    onWeightPerSkeinChange = { weightPerSkein = it },
                )

                result?.let { r -> YarnResultCard(r) }
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun ProActionBar(
    yarnCardViewModel: YarnCardViewModel,
    onScanLabel: () -> Unit,
    onSavedYarns: () -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }
    val permDeniedMessage = stringResource(R.string.camera_permission_denied)
    val permDeniedPermanentMessage = stringResource(R.string.camera_permission_denied_permanent)
    val openSettingsLabel = stringResource(R.string.open_settings)

    val cameraLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && pendingPhotoUri != null) {
                scope.launch {
                    yarnCardViewModel.setScanning(true)
                    val parsed = YarnLabelScanner.analyzeImage(context, pendingPhotoUri!!)
                    yarnCardViewModel.loadFromScan(parsed, pendingPhotoUri)
                    yarnCardViewModel.setScanning(false)
                    onScanLabel()
                }
            }
        }

    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                val (_, uri) = YarnLabelScanner.createImageFile(context)
                pendingPhotoUri = uri
                cameraLauncher.launch(uri)
            } else {
                val activity = context as? Activity
                val permanentlyDenied =
                    activity != null &&
                        !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
                scope.launch {
                    if (permanentlyDenied) {
                        val result =
                            snackbarHostState.showSnackbar(
                                message = permDeniedPermanentMessage,
                                actionLabel = openSettingsLabel,
                                duration = SnackbarDuration.Long,
                            )
                        if (result == SnackbarResult.ActionPerformed) {
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                },
                            )
                        }
                    } else {
                        snackbarHostState.showSnackbar(
                            message = permDeniedMessage,
                            duration = SnackbarDuration.Short,
                        )
                    }
                }
            }
        }

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

@Composable
private fun YarnInputFields(
    totalYarn: String,
    yarnPerSkein: String,
    weightPerSkein: String,
    lengthUnit: String,
    onTotalYarnChange: (String) -> Unit,
    onYarnPerSkeinChange: (String) -> Unit,
    onWeightPerSkeinChange: (String) -> Unit,
) {
    NumberInputField(
        value = totalYarn,
        onValueChange = onTotalYarnChange,
        label = stringResource(R.string.total_yarn_needed),
        isDecimal = true,
        suffix = lengthUnit,
        modifier = Modifier.fillMaxWidth(),
    )
    NumberInputField(
        value = yarnPerSkein,
        onValueChange = onYarnPerSkeinChange,
        label = stringResource(R.string.yarn_per_skein, lengthUnit),
        isDecimal = true,
        suffix = lengthUnit,
        modifier = Modifier.fillMaxWidth(),
    )
    NumberInputField(
        value = weightPerSkein,
        onValueChange = onWeightPerSkeinChange,
        label = stringResource(R.string.weight_per_skein),
        isDecimal = true,
        suffix = stringResource(R.string.unit_g),
        modifier = Modifier.fillMaxWidth(),
        isLast = true,
    )
}

@Composable
private fun YarnResultCard(result: YarnEstimate) {
    ResultCard(title = stringResource(R.string.result)) {
        Text(
            text = stringResource(R.string.skeins_result, result.skeinsNeeded),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = stringResource(R.string.total_weight, "%.0f".format(result.totalWeight)),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = stringResource(R.string.exact_skeins, "%.2f".format(result.exactSkeins)),
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = stringResource(R.string.extra_skein_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
