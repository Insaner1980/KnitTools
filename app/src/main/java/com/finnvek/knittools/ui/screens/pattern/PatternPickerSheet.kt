package com.finnvek.knittools.ui.screens.pattern

import android.content.ActivityNotFoundException
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.SavedPattern
import com.finnvek.knittools.domain.model.isWebPatternCompatible
import com.finnvek.knittools.pro.ProActionAccess
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProState
import com.finnvek.knittools.pro.proActionAccess
import com.finnvek.knittools.ui.components.ConfirmationDialog
import com.finnvek.knittools.ui.components.ProMarker
import com.finnvek.knittools.ui.components.ProPromptRequest
import com.finnvek.knittools.ui.components.ProPromptSheet
import com.finnvek.knittools.ui.components.ProPromptSource
import com.finnvek.knittools.ui.components.SectionLabel
import com.finnvek.knittools.ui.components.SheetOptionRow
import com.finnvek.knittools.ui.components.SheetTitle
import com.finnvek.knittools.ui.layout.AdaptiveModalWindow
import com.finnvek.knittools.ui.theme.knitToolsColors
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private data class PatternPickerActions(
    val openWebPatternEditor: () -> Unit,
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

internal fun shouldShowWebPatternEntry(mode: PatternPickerMode): Boolean =
    mode == PatternPickerMode.INITIAL_PROJECT_PATTERN

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("kotlin:S107", "kotlin:S3776") // Picker kokoaa kaikki tuontilähteet ja niiden eksplisiittiset callbackit.
fun PatternPickerSheet(
    projectId: Long?,
    savedPatterns: List<SavedPattern>,
    canUseCameraScan: Boolean,
    proState: ProState,
    proAccessReady: Boolean,
    hasExistingPattern: Boolean,
    mode: PatternPickerMode = PatternPickerMode.INITIAL_PROJECT_PATTERN,
    isRestoringProject: Boolean = false,
    excludedSavedPatternIds: Set<Long> = emptySet(),
    onSavedPatternSelected: (SavedPattern) -> Unit,
    onDocumentSelected: (String, String, Long) -> Unit,
    onImportFromRavelry: () -> Unit,
    onAddWebPattern: () -> Unit = {},
    onSeePro: () -> Unit,
    onDismiss: () -> Unit,
    initialCameraRequestId: String? = null,
) = key(projectId) {
    val imageImportViewModel: PatternImageImportViewModel =
        hiltViewModel(key = patternImageImportViewModelKey(projectId))
    val imageImportState by imageImportViewModel.uiState.collectAsStateWithLifecycle()
    val sessionId = imageImportState.sessionId
    val completedBeforeOpening =
        remember(imageImportViewModel) {
            imageImportViewModel.uiState.value
                .takeIf { it.closeReady }
                ?.sessionId
        }
    var pendingProAction by rememberSaveable { mutableStateOf<PendingPatternProAction?>(null) }
    var showDiscardConfirmation by rememberSaveable { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    val keepSelectedImages: () -> Unit = {
        showDiscardConfirmation = false
        scope.launch { sheetState.show() }
    }
    val actions =
        rememberPatternPickerActions(
            projectId = projectId,
            isRestoringProject = isRestoringProject,
            canUseCameraScan = canUseCameraScan,
            imageImportViewModelProvider = { imageImportViewModel },
            onDocumentSelected = onDocumentSelected,
            onImportFromRavelry = onImportFromRavelry,
            onAddWebPattern = onAddWebPattern,
            onLockedGalleryImport = { pendingProAction = PendingPatternProAction.GalleryImages },
            onLockedCameraScan = { pendingProAction = PendingPatternProAction.CameraCapture },
            onDismiss = onDismiss,
        )

    var handledCameraRequestId by rememberSaveable { mutableStateOf<String?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentCameraAction by rememberUpdatedState(actions.startCameraScan)
    LaunchedEffect(initialCameraRequestId, projectId, isRestoringProject, proAccessReady, imageImportState.isBusy) {
        val requestId = initialCameraRequestId ?: return@LaunchedEffect
        if (handledCameraRequestId == requestId || projectId == null || isRestoringProject) return@LaunchedEffect
        if (!proAccessReady || imageImportState.isBusy) return@LaunchedEffect
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            if (handledCameraRequestId != requestId) {
                handledCameraRequestId = requestId
                currentCameraAction()
            }
        }
    }

    LaunchedEffect(sessionId, imageImportState.closeReady) {
        if (
            imageImportState.closeReady &&
            imageImportViewModel.consumeCloseRequest(sessionId) &&
            sessionId != completedBeforeOpening
        ) {
            onDismiss()
        }
    }

    pendingProAction?.let {
        ProPromptSheet(
            request =
                ProPromptRequest(
                    source =
                        when (pendingProAction) {
                            PendingPatternProAction.GalleryImages -> ProPromptSource.PatternGallery

                            PendingPatternProAction.CameraCapture,
                            null,
                            -> ProPromptSource.PatternCamera
                        },
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
        sheetState = sheetState,
        onDismissRequest = {
            when {
                imageImportState.isBusy -> imageImportViewModel.cancelImport(sessionId)
                imageImportState.selection.pages.isNotEmpty() -> showDiscardConfirmation = true
                else -> onDismiss()
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        AdaptiveModalWindow()
        val showImageImport =
            imageImportState.selection.pages.isNotEmpty() ||
                imageImportState.isBusy ||
                imageImportState.phase == PatternImageImportPhase.ERROR
        if (showImageImport) {
            PatternImageImportSurface(
                state = imageImportState,
                onAddMore = { if (imageImportViewModel.isCurrentSession(sessionId)) actions.chooseImages() },
                onMoveEarlier = { imageImportViewModel.moveEarlier(it, sessionId) },
                onMoveLater = { imageImportViewModel.moveLater(it, sessionId) },
                onRemove = { imageImportViewModel.removePage(it, sessionId) },
                onCreate = {
                    if (imageImportState.replacementConfirmationPending) {
                        imageImportViewModel.confirmReplacement(sessionId)
                    } else {
                        imageImportViewModel.createPatternPdf(hasExistingPattern, sessionId)
                    }
                },
                onCancel = {
                    if (imageImportState.replacementConfirmationPending) {
                        imageImportViewModel.dismissReplacement(sessionId)
                    } else {
                        imageImportViewModel.cancelImport(sessionId)
                    }
                },
                onPreviewFailed = { imageImportViewModel.markPreviewFailed(it, sessionId) },
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
                proState = proState,
                proAccessReady = proAccessReady,
                projectId = projectId,
                mode = mode,
                actions = actions,
                onSavedPatternSelected = { pattern ->
                    onSavedPatternSelected(pattern)
                    if (!pattern.isWebPatternCompatible) {
                        onDismiss()
                    }
                },
            )
        }
    }

    if (showDiscardConfirmation) {
        ConfirmationDialog(
            title = stringResource(R.string.pattern_image_discard_title),
            message = stringResource(R.string.pattern_image_discard_message),
            confirmText = stringResource(R.string.pattern_image_discard_action),
            isDestructive = true,
            onConfirm = {
                showDiscardConfirmation = false
                imageImportViewModel.cancelImport(sessionId)
            },
            onDismiss = keepSelectedImages,
        )
    }
}

@Composable
@Suppress("kotlin:S107", "kotlin:S3776") // Muistetut picker-toiminnot sitovat eri tuontisopimukset yhteen omistajaan.
private fun rememberPatternPickerActions(
    projectId: Long?,
    isRestoringProject: Boolean,
    canUseCameraScan: Boolean,
    imageImportViewModelProvider: @Composable () -> PatternImageImportViewModel,
    onDocumentSelected: (String, String, Long) -> Unit,
    onImportFromRavelry: () -> Unit,
    onAddWebPattern: () -> Unit,
    onLockedGalleryImport: () -> Unit,
    onLockedCameraScan: () -> Unit,
    onDismiss: () -> Unit,
): PatternPickerActions {
    val imageImportViewModel = imageImportViewModelProvider()
    val context = LocalContext.current
    val currentProjectId by rememberUpdatedState(projectId)
    val currentCanUseCameraScan by rememberUpdatedState(canUseCameraScan)
    var pendingDocumentProjectId by rememberSaveable { mutableStateOf<Long?>(null) }
    var pendingDocumentUriString by rememberSaveable { mutableStateOf<String?>(null) }
    val imageImportState by imageImportViewModel.uiState.collectAsStateWithLifecycle()
    var pendingCaptureImageUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingCaptureFilePath by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingCaptureAuthorized by rememberSaveable { mutableStateOf(false) }
    var pendingCaptureProjectId by rememberSaveable { mutableStateOf<Long?>(null) }
    var pendingCaptureSessionId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingCaptureLaunch by rememberSaveable { mutableStateOf(false) }
    var pendingCaptureResult by rememberSaveable { mutableStateOf<Boolean?>(null) }
    val clearPendingCapture = {
        pendingCaptureImageUriString = null
        pendingCaptureFilePath = null
        pendingCaptureAuthorized = false
        pendingCaptureProjectId = null
        pendingCaptureSessionId = null
        pendingCaptureLaunch = false
        pendingCaptureResult = null
    }

    val registry = checkNotNull(LocalActivityResultRegistryOwner.current).activityResultRegistry
    val requestId = imageImportState.pickerRequestId
    DisposableEffect(registry, imageImportViewModel, requestId) {
        // The stable request key also receives a pending result after this project's sheet is reopened.
        val launcher =
            requestId?.let {
                registry.register("pattern-images-$requestId", PatternImagePickerContract()) { uris ->
                    imageImportViewModel.onGalleryPickerResult(requestId, uris)
                }
            }
        if (requestId != null && launcher != null && imageImportViewModel.markGalleryPickerLaunched(requestId)) {
            try {
                launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            } catch (_: ActivityNotFoundException) {
                imageImportViewModel.onGalleryPickerLaunchFailed(requestId)
            } catch (_: IllegalStateException) {
                imageImportViewModel.onGalleryPickerLaunchFailed(requestId)
            } catch (_: SecurityException) {
                imageImportViewModel.onGalleryPickerLaunchFailed(requestId)
            }
        }
        onDispose { launcher?.unregister() }
    }

    val openDocumentLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (pendingDocumentProjectId == null) return@rememberLauncherForActivityResult
            pendingDocumentUriString = uri?.toString()
            if (uri == null) pendingDocumentProjectId = null
        }
    LaunchedEffect(pendingDocumentUriString, projectId, isRestoringProject) {
        val uri = pendingDocumentUriString ?: return@LaunchedEffect
        if (isRestoringProject) return@LaunchedEffect
        val owner = pendingDocumentProjectId
        pendingDocumentUriString = null
        pendingDocumentProjectId = null
        if (owner != null && owner == currentProjectId) {
            onDocumentSelected(uri, "", owner)
            onDismiss()
        }
    }
    val captureSessionId = pendingCaptureSessionId
    LaunchedEffect(imageImportState.sessionId, captureSessionId) {
        if (imageImportState.cameraCapturePending && captureSessionId == null) {
            imageImportViewModel.onCameraCaptureStateLost(imageImportState.sessionId)
        }
    }
    val cameraLauncher =
        key(captureSessionId) {
            rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
                if (captureSessionId != null &&
                    captureSessionId == pendingCaptureSessionId &&
                    pendingCaptureImageUriString != null
                ) {
                    pendingCaptureResult = success
                }
            }
        }
    LaunchedEffect(pendingCaptureResult, projectId, isRestoringProject) {
        val success = pendingCaptureResult ?: return@LaunchedEffect
        if (success && isRestoringProject) return@LaunchedEffect
        val imageUri = pendingCaptureImageUriString?.let(Uri::parse)
        val imageFile = pendingCaptureFilePath?.let(::File)
        val owner = pendingCaptureProjectId
        val session = pendingCaptureSessionId
        val authorized = pendingCaptureAuthorized
        clearPendingCapture()
        if (imageUri == null || imageFile == null) return@LaunchedEffect
        val acceptedOwner =
            owner?.takeIf {
                it == currentProjectId && canStartPatternCameraScan(it, currentCanUseCameraScan || authorized)
            }
        if (success && acceptedOwner != null) {
            imageImportViewModel.acceptCameraCapture(acceptedOwner, imageUri, imageFile, session)
        } else {
            imageImportViewModel.discardCameraCapture(imageUri, imageFile, session)
        }
    }
    LaunchedEffect(pendingCaptureLaunch, captureSessionId, projectId, isRestoringProject) {
        if (!pendingCaptureLaunch || isRestoringProject) return@LaunchedEffect
        val owner = pendingCaptureProjectId
        val session = captureSessionId ?: return@LaunchedEffect
        if (owner == null ||
            owner != currentProjectId ||
            !canStartPatternCameraScan(owner, currentCanUseCameraScan || pendingCaptureAuthorized)
        ) {
            imageImportViewModel.cancelCameraCaptureRequest(session)
            clearPendingCapture()
            return@LaunchedEffect
        }
        try {
            // Retain the created target even if this sheet closes while its bounded file write finishes.
            val target = withContext(NonCancellable) { imageImportViewModel.createCameraCaptureTarget(owner, session) }
            if (target != null) {
                val (file, uri) = target
                if (!isActive ||
                    owner != currentProjectId ||
                    owner != pendingCaptureProjectId ||
                    session != pendingCaptureSessionId
                ) {
                    imageImportViewModel.discardCameraCapture(uri, file, session)
                    if (session == pendingCaptureSessionId) clearPendingCapture()
                    return@LaunchedEffect
                }
                pendingCaptureImageUriString = uri.toString()
                pendingCaptureFilePath = file.absolutePath

                fun abandonCapture() {
                    imageImportViewModel.discardCameraCapture(uri, file, session)
                    clearPendingCapture()
                    Toast.makeText(context, R.string.generic_error_unknown, Toast.LENGTH_SHORT).show()
                }
                try {
                    cameraLauncher.launch(uri)
                } catch (_: ActivityNotFoundException) {
                    abandonCapture()
                } catch (_: IllegalStateException) {
                    abandonCapture()
                } catch (_: SecurityException) {
                    abandonCapture()
                }
            }
        } finally {
            if (session == pendingCaptureSessionId) pendingCaptureLaunch = false
        }
    }

    val openPdfDocumentPicker: () -> Unit = {
        currentProjectId?.let { owner ->
            pendingDocumentProjectId = owner
            openDocumentLauncher.launch(pdfMimeTypes())
        }
    }
    val launchGalleryPicker = {
        val pendingProjectId = currentProjectId
        if (pendingProjectId != null) {
            imageImportViewModel.authorizeGalleryPicker(pendingProjectId)
        }
    }
    val launchCameraScan: () -> Unit = {
        currentProjectId?.let { authorizedProjectId ->
            val session = imageImportViewModel.authorizeCameraCapture(authorizedProjectId) ?: return@let
            pendingCaptureSessionId = session
            pendingCaptureProjectId = authorizedProjectId
            pendingCaptureAuthorized = true
            pendingCaptureLaunch = true
        }
    }

    return remember(
        imageImportViewModel,
        openDocumentLauncher,
        cameraLauncher,
        onDismiss,
        onAddWebPattern,
        onImportFromRavelry,
        onLockedCameraScan,
        onLockedGalleryImport,
    ) {
        PatternPickerActions(
            openWebPatternEditor = {
                onDismiss()
                onAddWebPattern()
            },
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
                    launchCameraScan()
                } else {
                    onLockedCameraScan()
                }
            },
            authorizeAndStartCameraScan = launchCameraScan,
            continueWithoutPattern = onDismiss,
        )
    }
}

@Composable
private fun PatternPickerSheetContent(
    savedPatterns: List<SavedPattern>,
    proState: ProState,
    proAccessReady: Boolean,
    projectId: Long?,
    mode: PatternPickerMode,
    actions: PatternPickerActions,
    onSavedPatternSelected: (SavedPattern) -> Unit,
) {
    val cameraAccess = proActionAccess(proState, proAccessReady, ProFeature.PATTERN_CAMERA_SCAN)
    val cameraActionReady =
        cameraAccess != ProActionAccess.Unknown || proState.hasFeature(ProFeature.PATTERN_CAMERA_SCAN)
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Dokumentin lisäyksessä otsikko kertoo mitä lisätään: pelkkä "Add" ei sanonut mitään.
        SheetTitle(
            text =
                stringResource(
                    if (mode == PatternPickerMode.ADD_READABLE_PROJECT_DOCUMENT) {
                        R.string.project_content_add_pattern
                    } else {
                        R.string.attach_pattern
                    },
                ),
        )

        if (savedPatterns.isNotEmpty()) {
            SectionLabel(text = stringResource(R.string.pattern_picker_saved_patterns))
            PatternPickerSavedPatterns(
                savedPatterns = savedPatterns,
                onSavedPatternSelected = onSavedPatternSelected,
            )
        }

        if (shouldShowWebPatternEntry(mode)) {
            PickerAction(stringResource(R.string.web_pattern_add), actions.openWebPatternEditor)
            PickerAction(stringResource(R.string.pattern_picker_import_from_ravelry), actions.openRavelryImport)
        }
        PickerAction(stringResource(R.string.pattern_picker_import_pdf), actions.openDeviceFiles)
        PickerAction(stringResource(R.string.pattern_picker_import_cloud_pdf), actions.openCloudProviderFiles)
        // Pro-toiminnot samalla painiketyylillä kuin muut: täytettyinä maksulliset vaihtoehdot näyttivät
        // päätoiminnolta. PRO-merkki on aina oikeassa reunassa.
        PickerAction(
            label = stringResource(R.string.pattern_picker_choose_images),
            onClick = actions.chooseImages,
            enabled = projectId != null && cameraActionReady,
            badge = { ProMarker(access = cameraAccess) },
        )
        PickerAction(
            label = stringResource(R.string.pattern_picker_camera_scan),
            onClick = actions.startCameraScan,
            enabled = projectId != null && cameraActionReady,
            badge = { ProMarker(access = cameraAccess) },
        )
        if (mode == PatternPickerMode.INITIAL_PROJECT_PATTERN) {
            PickerAction(
                label = stringResource(R.string.pattern_picker_continue_without_pattern),
                onClick = actions.continueWithoutPattern,
            )
        }
    }
}

// Sama vaihtoehtorivi kuin langan valinnassa; ääriviivapillerit näyttivät eri listalta.
@Composable
private fun PickerAction(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    badge: (@Composable () -> Unit)? = null,
) {
    SheetOptionRow(title = label, onClick = onClick, enabled = enabled, badge = badge)
}

@Composable
private fun PatternPickerSavedPatterns(
    savedPatterns: List<SavedPattern>,
    onSavedPatternSelected: (SavedPattern) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.heightIn(max = 240.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(savedPatterns, key = { it.id }) { pattern ->
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.knitToolsColors.cardContainer,
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

private const val PATTERN_PDF_MIME_TYPE = "application/pdf"

private enum class PendingPatternProAction {
    GalleryImages,
    CameraCapture,
}
