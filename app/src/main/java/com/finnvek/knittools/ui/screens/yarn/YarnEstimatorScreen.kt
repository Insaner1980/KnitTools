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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.calculator.YarnEstimator
import com.finnvek.knittools.domain.model.YarnEstimate
import com.finnvek.knittools.ui.components.AnimatedResultNumber
import com.finnvek.knittools.ui.components.BadgePill
import com.finnvek.knittools.ui.components.InfoNote
import com.finnvek.knittools.ui.components.NumberInputField
import com.finnvek.knittools.ui.components.ResultCard
import com.finnvek.knittools.ui.components.ToolScreenScaffold
import com.finnvek.knittools.ui.screens.home.HomeViewModel
import com.finnvek.knittools.ui.screens.yarncard.YarnCardViewModel
import kotlinx.coroutines.launch

@Composable
fun YarnEstimatorScreen(
    onBack: () -> Unit,
    onScanLabel: () -> Unit = {},
    onSavedYarns: () -> Unit = {},
    yarnCardViewModel: YarnCardViewModel? = null,
    homeViewModel: HomeViewModel = hiltViewModel(),
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val useImperial by homeViewModel.useImperial.collectAsStateWithLifecycle()

    ToolScreenScaffold(
        title = stringResource(R.string.tool_yarn_estimator),
        onBack = onBack,
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            YarnEstimatorContent(
                yarnCardViewModel = yarnCardViewModel,
                onScanLabel = onScanLabel,
                onSavedYarns = onSavedYarns,
                snackbarHostState = snackbarHostState,
                useImperial = useImperial,
            )
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun YarnEstimatorContent(
    yarnCardViewModel: YarnCardViewModel?,
    onScanLabel: () -> Unit,
    onSavedYarns: () -> Unit,
    snackbarHostState: SnackbarHostState,
    useImperial: Boolean,
) {
    var totalYarn by rememberSaveable { mutableStateOf("") }
    var yarnPerSkein by rememberSaveable { mutableStateOf("") }
    var weightPerSkein by rememberSaveable { mutableStateOf("") }

    // Täytä kentät skannatuista arvoista (Save and Use / Use in Calculator)
    if (yarnCardViewModel != null) {
        ApplyPendingCalcValues(yarnCardViewModel) { w, l ->
            if (l.isNotBlank()) yarnPerSkein = l
            if (w.isNotBlank()) weightPerSkein = w
        }
    }

    val result by remember(totalYarn, yarnPerSkein, weightPerSkein) {
        derivedStateOf { calculateYarnEstimate(totalYarn, yarnPerSkein, weightPerSkein) }
    }

    val formState = yarnCardViewModel?.formState?.collectAsStateWithLifecycle()?.value
    val isScanning = formState?.isScanning == true
    val scanError = formState?.scanError

    LaunchedEffect(scanError) {
        if (!scanError.isNullOrBlank()) {
            snackbarHostState.showSnackbar(scanError, duration = SnackbarDuration.Short)
            yarnCardViewModel.updateField { copy(scanError = null) }
        }
    }
    val lengthUnit =
        if (useImperial) {
            stringResource(R.string.unit_yards)
        } else {
            stringResource(R.string.unit_meters)
        }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (isScanning) {
            ScanningIndicator()
        }
        yarnCardViewModel?.takeIf { it.isPro }?.let { vm ->
            ProActionBar(
                yarnCardViewModel = vm,
                onScanLabel = onScanLabel,
                onSavedYarns = onSavedYarns,
                snackbarHostState = snackbarHostState,
            )
        }

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                YarnInputFields(
                    totalYarn = totalYarn,
                    yarnPerSkein = yarnPerSkein,
                    weightPerSkein = weightPerSkein,
                    lengthUnit = lengthUnit,
                    onTotalYarnChange = { totalYarn = it },
                    onYarnPerSkeinChange = { yarnPerSkein = it },
                    onWeightPerSkeinChange = { weightPerSkein = it },
                )
            }
        }

        result?.let { r -> YarnResultCard(r) }
    }
}

@Composable
private fun ApplyPendingCalcValues(
    yarnCardViewModel: YarnCardViewModel,
    onApply: (weight: String, length: String) -> Unit,
) {
    val pending by yarnCardViewModel.pendingCalcValues.collectAsStateWithLifecycle()
    LaunchedEffect(pending) {
        val (w, l, _) = pending ?: return@LaunchedEffect
        onApply(w, l)
        yarnCardViewModel.clearPendingCalcValues()
    }
}

@Composable
private fun ScanningIndicator() {
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

@Composable
private fun ProActionBar(
    yarnCardViewModel: YarnCardViewModel,
    onScanLabel: () -> Unit,
    onSavedYarns: () -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingPhotoUriString by rememberSaveable { mutableStateOf<String?>(null) }
    val pendingPhotoUri = pendingPhotoUriString?.let(Uri::parse)
    val permDeniedMessage = stringResource(R.string.camera_permission_denied)
    val permDeniedPermanentMessage = stringResource(R.string.camera_permission_denied_permanent)
    val openSettingsLabel = stringResource(R.string.open_settings)

    val cameraLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && pendingPhotoUri != null) {
                yarnCardViewModel.scanWithGemini(pendingPhotoUri) {
                    pendingPhotoUriString = null
                    onScanLabel()
                }
            }
        }

    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                val uri = yarnCardViewModel.createScanPhotoUri()
                pendingPhotoUriString = uri.toString()
                cameraLauncher.launch(uri)
            } else {
                val activity = context as? Activity
                val permanentlyDenied =
                    activity != null &&
                        !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
                scope.launch {
                    showPermissionDeniedSnackbar(
                        snackbarHostState,
                        context,
                        permanentlyDenied,
                        permDeniedPermanentMessage,
                        permDeniedMessage,
                        openSettingsLabel,
                    )
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
        AnimatedResultNumber(
            targetValue = stringResource(R.string.skeins_result, result.skeinsNeeded),
        ) { value ->
            Text(
                text = value,
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        BadgePill(
            text = stringResource(R.string.total_weight, "%.0f".format(result.totalWeight)),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.exact_skeins, "%.2f".format(result.exactSkeins)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
    InfoNote(text = stringResource(R.string.extra_skein_note))
}

private fun calculateYarnEstimate(
    totalYarn: String,
    yarnPerSkein: String,
    weightPerSkein: String,
): YarnEstimate? {
    val total = totalYarn.toDoubleOrNull() ?: return null
    val perSkein = yarnPerSkein.toDoubleOrNull() ?: return null
    val weight = weightPerSkein.toDoubleOrNull() ?: return null
    if (total <= 0 || perSkein <= 0 || weight <= 0) return null
    return YarnEstimator.estimate(total, perSkein, weight)
}

private suspend fun showPermissionDeniedSnackbar(
    snackbarHostState: SnackbarHostState,
    context: android.content.Context,
    permanentlyDenied: Boolean,
    permanentMessage: String,
    shortMessage: String,
    openSettingsLabel: String,
) {
    if (permanentlyDenied) {
        val result =
            snackbarHostState.showSnackbar(
                message = permanentMessage,
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
            message = shortMessage,
            duration = SnackbarDuration.Short,
        )
    }
}
