package com.finnvek.knittools.ui.screens.pattern

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.SavedPattern
import com.finnvek.knittools.pro.ProStatus
import com.finnvek.knittools.ui.components.ProBadge
import com.finnvek.knittools.ui.components.ProPromptRequest
import com.finnvek.knittools.ui.components.ProPromptSheet
import com.finnvek.knittools.ui.components.ProPromptSource
import kotlinx.coroutines.launch
import java.io.File

private data class PatternPickerActions(
    val openRavelryImport: () -> Unit,
    val openDeviceFiles: () -> Unit,
    val openCloudProviderFiles: () -> Unit,
    val chooseImages: () -> Unit,
    val authorizeAndChooseImages: () -> Unit,
    val startCameraScan: () -> Unit,
    val authorizeAndStartCameraScan: () -> Unit,
    val continueWithoutPattern: () -> Unit,
)

enum class PatternPickerMode {
    INITIAL_PROJECT_PATTERN,
    ADD_READABLE_PROJECT_DOCUMENT,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatternPickerSheet(
    projectId: Long?,
    savedPatterns: List<SavedPattern>,
    canUseCameraScan: Boolean,
    proStatus: ProStatus,
    hasExistingPattern: Boolean,
    mode: PatternPickerMode = PatternPickerMode.INITIAL_PROJECT_PATTERN,
    excludedSavedPatternIds: Set<Long> = emptySet(),
    onSavedPatternSelected: (SavedPattern) -> Unit,
    onDocumentSelected: (String, String) -> Unit,
    onImportFromRavelry: () -> Unit,
    onSeePro: () -> Unit,
    onDismiss: () -> Unit,
) {
    val imageImportViewModel: PatternImageImportViewModel = hiltViewModel()
    val imageImportState by imageImportViewModel.uiState.collectAsStateWithLifecycle()
    var pendingProAction by rememberSaveable { mutableStateOf<PendingPatternProAction?>(null) }
    var showDiscardConfirmation by rememberSaveable { mutableStateOf(false) }
    val actions =
        rememberPatternPickerActions(
            projectId = projectId,
            canUseCameraScan = canUseCameraScan,
            imageImportViewModel = imageImportViewModel,
            onDocumentSelected = onDocumentSelected,
            onImportFromRavelry = onImportFromRavelry,
            onLockedGalleryImport = { pendingProAction = PendingPatternProAction.GalleryImages },
            onLockedCameraScan = { pendingProAction = PendingPatternProAction.CameraCapture },
            onDismiss = onDismiss,
        )

    LaunchedEffect(imageImportState.closeReady) {
        if (imageImportState.closeReady) onDismiss()
    }

    pendingProAction?.let {
        ProPromptSheet(
            request =
                ProPromptRequest(
                    source = ProPromptSource.PatternCamera,
                ),
            onDismiss = { pendingProAction = null },
            onTrialStarted = {
                val action = pendingProAction
                pendingProAction = null
                when (action) {
                    PendingPatternProAction.GalleryImages -> actions.authorizeAndChooseImages()
                    PendingPatternProAction.CameraCapture -> actions.authorizeAndStartCameraScan()
                    null -> Unit
                }
            },
            onSeePro = onSeePro,
        )
    }

    ModalBottomSheet(
        onDismissRequest = {
            when {
                imageImportState.isBusy -> imageImportViewModel.cancelImport()
                imageImportState.selection.pages.isNotEmpty() -> showDiscardConfirmation = true
                else -> onDismiss()
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        val showImageImport =
            imageImportState.selection.pages.isNotEmpty() ||
                imageImportState.isBusy ||
                imageImportState.phase == PatternImageImportPhase.ERROR
        if (showImageImport) {
            PatternImageImportSurface(
                state = imageImportState,
                onAddMore = actions.chooseImages,
                onMoveEarlier = imageImportViewModel::moveEarlier,
                onMoveLater = imageImportViewModel::moveLater,
                onRemove = imageImportViewModel::removePage,
                onCreate = {
                    if (imageImportState.replacementConfirmationPending) {
                        imageImportViewModel.confirmReplacement()
                    } else {
                        imageImportViewModel.createPatternPdf(hasExistingPattern)
                    }
                },
                onCancel = {
                    if (imageImportState.replacementConfirmationPending) {
                        imageImportViewModel.dismissReplacement()
                    } else {
                        imageImportViewModel.cancelImport()
                    }
                },
                onPreviewFailed = imageImportViewModel::markPreviewFailed,
            )
        } else {
            PatternPickerSheetContent(
                savedPatterns =
                    if (mode == PatternPickerMode.ADD_READABLE_PROJECT_DOCUMENT) {
                        savedPatterns.filter {
                            !it.localPdfUri.isNullOrBlank() && it.id !in excludedSavedPatternIds
                        }
                    } else {
                        savedPatterns
                    },
                proStatus = proStatus,
                projectId = projectId,
                mode = mode,
                actions = actions,
                onSavedPatternSelected = { pattern ->
                    onSavedPatternSelected(pattern)
                    onDismiss()
                },
            )
        }
    }

    if (showDiscardConfirmation) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirmation = false },
            title = { Text(stringResource(R.string.pattern_image_discard_title)) },
            text = { Text(stringResource(R.string.pattern_image_discard_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardConfirmation = false
                        imageImportViewModel.cancelImport()
                    },
                ) {
                    Text(stringResource(R.string.pattern_image_discard_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirmation = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun rememberPatternPickerActions(
    projectId: Long?,
    canUseCameraScan: Boolean,
    imageImportViewModel: PatternImageImportViewModel,
    onDocumentSelected: (String, String) -> Unit,
    onImportFromRavelry: () -> Unit,
    onLockedGalleryImport: () -> Unit,
    onLockedCameraScan: () -> Unit,
    onDismiss: () -> Unit,
): PatternPickerActions {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentProjectId by rememberUpdatedState(projectId)
    val currentCanUseCameraScan by rememberUpdatedState(canUseCameraScan)
    var pendingGalleryRequestId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingCaptureImageUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingCaptureFilePath by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingCaptureAuthorized by rememberSaveable { mutableStateOf(false) }
    val clearPendingCapture = {
        pendingCaptureImageUriString = null
        pendingCaptureFilePath = null
        pendingCaptureAuthorized = false
    }

    val imagePickerLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.PickMultipleVisualMedia(PatternImageImportLimits.MAX_PAGES),
        ) { uris ->
            val requestId = pendingGalleryRequestId
            pendingGalleryRequestId = null
            imageImportViewModel.onGalleryPickerResult(requestId, uris)
        }

    val openDocumentLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri ?: return@rememberLauncherForActivityResult
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, flags)
            }
            onDocumentSelected(uri.toString(), resolvePatternName(context, uri))
            onDismiss()
        }
    val cameraLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            val imageUri = pendingCaptureImageUriString?.let(Uri::parse)
            val imageFile = pendingCaptureFilePath?.let(::File)
            scope.launch {
                try {
                    if (!success || imageUri == null || imageFile == null) {
                        if (imageUri != null && imageFile != null) {
                            imageImportViewModel.discardCameraCapture(imageUri, imageFile)
                        }
                        return@launch
                    }
                    if (
                        canStartPatternCameraScan(
                            currentProjectId,
                            currentCanUseCameraScan || pendingCaptureAuthorized,
                        )
                    ) {
                        imageImportViewModel.acceptCameraCapture(currentProjectId ?: return@launch, imageUri, imageFile)
                    } else {
                        imageImportViewModel.discardCameraCapture(imageUri, imageFile)
                    }
                } finally {
                    clearPendingCapture()
                }
            }
        }
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                pendingCaptureAuthorized = false
                showCameraPermissionDeniedToast(context)
                return@rememberLauncherForActivityResult
            }
            val pendingProjectId = currentProjectId ?: return@rememberLauncherForActivityResult
            if (!canStartPatternCameraScan(pendingProjectId, currentCanUseCameraScan || pendingCaptureAuthorized)) {
                return@rememberLauncherForActivityResult
            }
            scope.launch {
                val (file, uri) = imageImportViewModel.createCameraCaptureTarget(pendingProjectId) ?: return@launch
                pendingCaptureImageUriString = uri.toString()
                pendingCaptureFilePath = file.absolutePath
                try {
                    cameraLauncher.launch(uri)
                } catch (_: ActivityNotFoundException) {
                    imageImportViewModel.discardCameraCapture(uri, file)
                    clearPendingCapture()
                } catch (_: IllegalStateException) {
                    imageImportViewModel.discardCameraCapture(uri, file)
                    clearPendingCapture()
                }
            }
        }

    val openPdfDocumentPicker = { openDocumentLauncher.launch(pdfMimeTypes()) }
    val launchGalleryPicker = {
        val pendingProjectId = currentProjectId
        if (pendingProjectId != null) {
            pendingGalleryRequestId = imageImportViewModel.authorizeGalleryPicker(pendingProjectId)
            imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }

    return remember(
        imagePickerLauncher,
        openDocumentLauncher,
        permissionLauncher,
        onDismiss,
        onImportFromRavelry,
        onLockedCameraScan,
        onLockedGalleryImport,
    ) {
        PatternPickerActions(
            openRavelryImport = {
                onDismiss()
                onImportFromRavelry()
            },
            openDeviceFiles = openPdfDocumentPicker,
            openCloudProviderFiles = openPdfDocumentPicker,
            chooseImages = {
                if (canStartPatternCameraScan(currentProjectId, currentCanUseCameraScan)) {
                    launchGalleryPicker()
                } else {
                    onLockedGalleryImport()
                }
            },
            authorizeAndChooseImages = launchGalleryPicker,
            startCameraScan = {
                if (canStartPatternCameraScan(currentProjectId, currentCanUseCameraScan)) {
                    currentProjectId?.let(imageImportViewModel::authorizeCameraCapture)
                    pendingCaptureAuthorized = true
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                } else {
                    onLockedCameraScan()
                }
            },
            authorizeAndStartCameraScan = {
                currentProjectId?.let { authorizedProjectId ->
                    imageImportViewModel.authorizeCameraCapture(authorizedProjectId)
                    pendingCaptureAuthorized = true
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }
            },
            continueWithoutPattern = onDismiss,
        )
    }
}

@Composable
private fun PatternPickerSheetContent(
    savedPatterns: List<SavedPattern>,
    proStatus: ProStatus,
    projectId: Long?,
    mode: PatternPickerMode,
    actions: PatternPickerActions,
    onSavedPatternSelected: (SavedPattern) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text =
                stringResource(
                    if (mode == PatternPickerMode.ADD_READABLE_PROJECT_DOCUMENT) {
                        R.string.project_documents_add
                    } else {
                        R.string.attach_pattern
                    },
                ),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        if (savedPatterns.isNotEmpty()) {
            Text(
                text = stringResource(R.string.pattern_picker_saved_patterns),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
            )
            PatternPickerSavedPatterns(
                savedPatterns = savedPatterns,
                onSavedPatternSelected = onSavedPatternSelected,
            )
        }

        if (mode == PatternPickerMode.INITIAL_PROJECT_PATTERN) {
            OutlinedButton(
                onClick = actions.openRavelryImport,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.pattern_picker_import_from_ravelry))
            }
        }

        OutlinedButton(
            onClick = actions.openDeviceFiles,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.pattern_picker_import_pdf))
        }

        OutlinedButton(
            onClick = actions.openCloudProviderFiles,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.pattern_picker_import_cloud_pdf))
        }

        Button(
            onClick = actions.chooseImages,
            enabled = projectId != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.pattern_picker_choose_images))
                ProBadge(status = proStatus)
            }
        }

        Button(
            onClick = actions.startCameraScan,
            enabled = projectId != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.pattern_picker_camera_scan))
                ProBadge(status = proStatus)
            }
        }

        if (mode == PatternPickerMode.INITIAL_PROJECT_PATTERN) {
            OutlinedButton(
                onClick = actions.continueWithoutPattern,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.pattern_picker_continue_without_pattern))
            }
        }
    }
}

@Composable
private fun PatternPickerSavedPatterns(
    savedPatterns: List<SavedPattern>,
    onSavedPatternSelected: (SavedPattern) -> Unit,
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(savedPatterns, key = { it.id }) { pattern ->
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = RoundedCornerShape(16.dp),
                        ).clickable { onSavedPatternSelected(pattern) }
                        .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = pattern.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                if (pattern.designerName.isNotBlank()) {
                    Text(
                        text = pattern.designerName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

internal fun canStartPatternCameraScan(
    projectId: Long?,
    canUseCameraScan: Boolean,
): Boolean = canUseCameraScan && projectId != null

private fun pdfMimeTypes(): Array<String> = arrayOf(PATTERN_PDF_MIME_TYPE)

private fun showCameraPermissionDeniedToast(context: android.content.Context) {
    val activity = context as? Activity
    val permanentlyDenied =
        activity != null &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
    val messageRes =
        if (permanentlyDenied) {
            R.string.camera_permission_denied_permanent
        } else {
            R.string.camera_permission_denied
        }
    Toast.makeText(context, context.getString(messageRes), Toast.LENGTH_SHORT).show()
}

private fun resolvePatternName(
    context: android.content.Context,
    uri: Uri,
): String {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        val columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (columnIndex >= 0 && cursor.moveToFirst()) {
            val fileName = cursor.getString(columnIndex)
            if (!fileName.isNullOrBlank()) return fileName
        }
    }
    return uri.lastPathSegment ?: context.getString(R.string.pattern_pdf_fallback_name)
}

private const val PATTERN_PDF_MIME_TYPE = "application/pdf"

private enum class PendingPatternProAction {
    GalleryImages,
    CameraCapture,
}
