package com.finnvek.knittools.ui.screens.pattern

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.knittools.R
import com.finnvek.knittools.analytics.UsageAnalytics
import com.finnvek.knittools.analytics.UsageDetail
import com.finnvek.knittools.analytics.UsageEvent
import com.finnvek.knittools.data.storage.PatternDocumentStorage
import com.finnvek.knittools.data.storage.PatternImageFailureReason
import com.finnvek.knittools.data.storage.PatternImageStageException
import com.finnvek.knittools.data.storage.PatternImageStageFailure
import com.finnvek.knittools.data.storage.PatternImageValidationException
import com.finnvek.knittools.di.ApplicationScope
import com.finnvek.knittools.di.IoDispatcher
import com.finnvek.knittools.domain.model.PatternDisplayNames
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.pro.ProStatus
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.repository.OnboardingStateRepository
import com.finnvek.knittools.repository.ProjectDocumentMutationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
// Julkiset toiminnot vastaavat yhden tuontinäkymän käyttäjätoimintoja ja elinkaarirajoja.
@Suppress("TooManyFunctions")
internal class PatternImageImportViewModel
    @Inject
    constructor(
        private val storage: PatternDocumentStorage,
        private val repository: CounterRepository,
        @param:ApplicationContext private val context: Context,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
        private val savedStateHandle: SavedStateHandle,
        private val proManager: ProManager,
        private val onboardingStateRepository: OnboardingStateRepository,
        @param:ApplicationScope private val applicationScope: CoroutineScope,
        private val analytics: UsageAnalytics = UsageAnalytics.NONE,
    ) : ViewModel() {
        private val restoredDraft = restoredState(savedStateHandle)
        private val _uiState =
            MutableStateFlow(
                if (restoredDraft.selection.pages.isEmpty()) {
                    restoredDraft
                } else {
                    restoredDraft.copy(selection = PatternImageSelection(), phase = PatternImageImportPhase.STAGING)
                },
            )
        val uiState: StateFlow<PatternImageImportUiState> = _uiState.asStateFlow()

        private var operationJob: Job? = null
        private var ownerProjectId = _uiState.value.projectId

        @Volatile
        private var activeOperationId: String? = null
        private var restoringDraft = false
        private var galleryResultDuringRestore: List<Uri>? = null

        init {
            val restored = restoredDraft
            if (restored.selection.pages.isNotEmpty()) {
                restoringDraft = true
                launchOperation { operationId ->
                    updateOperation(operationId) { it.copy(phase = PatternImageImportPhase.STAGING) }
                    try {
                        val restoredSelection =
                            withContext(ioDispatcher) {
                                storage.restoreStagedSelection(
                                    context,
                                    restored.projectId,
                                    restored.sessionId,
                                    restored.selection,
                                )
                            }
                        updateOperation(operationId) {
                            it.withRestoredSelection(restoredSelection, restored.selection.pages.size)
                        }
                        restoringDraft = false
                        persistState()
                        galleryResultDuringRestore?.let(::stageGallerySelection)
                    } catch (failure: CancellationException) {
                        val projectId = restored.projectId
                        val sessionId = restored.sessionId
                        if (projectId != null && sessionId != null) finishCancelled(projectId, sessionId, operationId)
                        throw failure
                    } finally {
                        restoringDraft = false
                        galleryResultDuringRestore = null
                    }
                }
            }
        }

        fun authorizeGalleryPicker(projectId: Long): String? {
            if (_uiState.value.attachmentPending) return null
            if (!ensureSession(projectId, PatternImageImportOrigin.GALLERY)) return null
            val requestId = UUID.randomUUID().toString()
            _uiState.update { it.copy(pickerRequestId = requestId, pickerLaunched = false, closeReady = false) }
            persistState()
            return requestId
        }

        fun markGalleryPickerLaunched(requestId: String): Boolean {
            val state = _uiState.value
            if (state.pickerRequestId != requestId || state.pickerLaunched) return false
            if (state.projectId == null || state.sessionId == null) return false
            _uiState.update { it.copy(pickerLaunched = true) }
            persistState()
            return true
        }

        fun onGalleryPickerLaunchFailed(requestId: String) {
            if (_uiState.value.pickerRequestId != requestId) return
            _uiState.update {
                it.copy(
                    pickerRequestId = null,
                    pickerLaunched = false,
                    phase = PatternImageImportPhase.ERROR,
                    error = PatternImageImportError.UNREADABLE,
                )
            }
            persistState()
        }

        fun authorizeCameraCapture(projectId: Long): String? {
            if (_uiState.value.attachmentPending) return null
            if (!ensureSession(projectId, PatternImageImportOrigin.CAMERA)) return null
            if (_uiState.value.selection.pages
                    .isNotEmpty() ||
                _uiState.value.cameraCapturePending
            ) {
                return null
            }
            _uiState.update { it.copy(sessionId = UUID.randomUUID().toString(), cameraCapturePending = true) }
            persistState()
            return _uiState.value.sessionId.orEmpty()
        }

        @Suppress("kotlin:S6313") // Activity tarvitsee FileProvider-kohteen ennen kamerasopimusta.
        suspend fun createCameraCaptureTarget(
            projectId: Long,
            sessionId: String? = _uiState.value.sessionId,
        ): Pair<File, Uri>? {
            val state = _uiState.value
            if (state.projectId != projectId ||
                state.origin != PatternImageImportOrigin.CAMERA ||
                !isCurrentSession(sessionId) ||
                !state.cameraCapturePending
            ) {
                return null
            }
            sessionId ?: return null
            return try {
                withContext(ioDispatcher) { storage.createCaptureImageFile(context, projectId, sessionId) }
            } catch (_: IOException) {
                if (isCurrentSession(sessionId)) {
                    onCameraCaptureStateLost(sessionId)
                    cancelCameraCaptureRequest(sessionId)
                }
                null
            }
        }

        fun discardCameraCapture(
            imageUri: Uri,
            imageFile: File,
            sessionId: String? = _uiState.value.sessionId,
        ) {
            cancelCameraCaptureRequest(sessionId)
            viewModelScope.launch(ioDispatcher) { storage.deleteStagedPage(cameraPage(imageUri, imageFile)) }
        }

        fun cancelCameraCaptureRequest(sessionId: String?) {
            if (!isCurrentSession(sessionId) || !_uiState.value.cameraCapturePending) return
            _uiState.update { it.copy(cameraCapturePending = false) }
            persistState()
        }

        fun onCameraCaptureStateLost(sessionId: String?) {
            if (!isCurrentSession(sessionId) || !_uiState.value.cameraCapturePending) return
            // Keep the owned session until the user explicitly cancels an unrestorable capture.
            _uiState.update {
                it.copy(
                    phase = PatternImageImportPhase.ERROR,
                    error = PatternImageImportError.UNREADABLE,
                )
            }
        }

        fun onGalleryPickerResult(
            requestId: String?,
            sourceUris: List<Uri>,
        ) {
            val state = _uiState.value
            if (requestId == null || requestId != state.pickerRequestId) return
            if (!restoringDraft && (state.isBusy || activeOperationId != null)) return
            _uiState.update { it.copy(pickerRequestId = null, pickerLaunched = false) }
            persistState()
            if (restoringDraft) {
                galleryResultDuringRestore = sourceUris
            } else {
                stageGallerySelection(sourceUris)
            }
        }

        @Suppress("SwallowedException")
        private fun stageGallerySelection(sourceUris: List<Uri>) {
            if (sourceUris.isEmpty()) return
            val state = _uiState.value
            val projectId = state.projectId ?: return
            val sessionId = state.sessionId ?: return
            launchOperation { operationId ->
                updateOperation(operationId) { it.copy(phase = PatternImageImportPhase.STAGING, error = null) }
                try {
                    @Suppress("kotlin:S6311") // Tiedostoputki kuuluu projektin injektoidulle IO-dispatcherille.
                    val batch =
                        withContext(ioDispatcher) {
                            storage.stageSelectedImages(
                                context = context,
                                projectId = projectId,
                                sessionId = sessionId,
                                existingSelection = state.selection,
                                sourceUris = sourceUris,
                            )
                        }
                    val accepted = state.selection.append(batch.pages) as? PatternImageAppendResult.Accepted
                    val selection = accepted?.selection ?: state.selection
                    val duplicateCount = batch.duplicatesIgnored + (accepted?.duplicatesIgnored ?: 0)
                    updateOperation(operationId) {
                        it.copy(
                            selection = selection,
                            phase = selectionPhase(selection.pages),
                            duplicatesIgnored = duplicateCount,
                        )
                    }
                    persistState()
                } catch (failure: CancellationException) {
                    finishCancelled(projectId, sessionId, operationId)
                } catch (failure: PatternImageStageException) {
                    showRecoverableError(failure.reason.toImportError(), operationId)
                }
            }
        }

        @Suppress("SwallowedException")
        fun acceptCameraCapture(
            projectId: Long,
            imageUri: Uri,
            imageFile: File,
            sessionId: String? = _uiState.value.sessionId,
        ) {
            val state = _uiState.value
            if (state.projectId != projectId ||
                state.origin != PatternImageImportOrigin.CAMERA ||
                !isCurrentSession(sessionId)
            ) {
                return
            }
            if (!state.cameraCapturePending || activeOperationId != null) return
            sessionId ?: return
            _uiState.update { it.copy(cameraCapturePending = false) }
            persistState()
            launchOperation { operationId ->
                updateOperation(operationId) { it.copy(phase = PatternImageImportPhase.STAGING, error = null) }
                try {
                    val info = withContext(ioDispatcher) { storage.inspectCameraCapture(imageFile) }
                    val page =
                        StagedPatternPage(
                            id = UUID.randomUUID().toString(),
                            sourceUri = imageUri.toString(),
                            stagedPath = imageFile.absolutePath,
                            byteCount = imageFile.length(),
                            width = info.width,
                            height = info.height,
                        )
                    updateOperation(operationId) {
                        it.copy(
                            selection = PatternImageSelection(listOf(page)),
                            phase = PatternImageImportPhase.READY,
                        )
                    }
                    persistState()
                } catch (failure: CancellationException) {
                    finishCancelled(projectId, sessionId, operationId)
                } catch (failure: PatternImageValidationException) {
                    withContext(ioDispatcher) { storage.deleteStagedPage(cameraPage(imageUri, imageFile)) }
                    showRecoverableFailure(PatternImageImportError.UNSUPPORTED, failure, operationId)
                } catch (failure: PatternImageStageException) {
                    withContext(ioDispatcher) { storage.deleteStagedPage(cameraPage(imageUri, imageFile)) }
                    showRecoverableError(failure.reason.toImportError(), operationId)
                }
            }
        }

        fun moveEarlier(
            pageId: String,
            sessionId: String? = _uiState.value.sessionId,
        ) = movePage(pageId, sessionId, PatternImageSelection::moveEarlier)

        fun moveLater(
            pageId: String,
            sessionId: String? = _uiState.value.sessionId,
        ) = movePage(pageId, sessionId, PatternImageSelection::moveLater)

        private fun movePage(
            pageId: String,
            sessionId: String?,
            move: PatternImageSelection.(Int) -> PatternImageSelection,
        ) {
            if (!isCurrentSession(sessionId) || activeOperationId != null || _uiState.value.attachmentPending) return
            updateSelection { selection ->
                selection.move(selection.pages.indexOfFirst { it.id == pageId })
            }
        }

        fun removePage(
            pageId: String,
            sessionId: String? = _uiState.value.sessionId,
        ) {
            if (!isCurrentSession(sessionId) || activeOperationId != null || _uiState.value.attachmentPending) return
            val page =
                _uiState.value.selection.pages
                    .firstOrNull { it.id == pageId } ?: return
            _uiState.update { it.copy(invalidPageIds = it.invalidPageIds - pageId) }
            updateSelection { it.remove(pageId) }
            viewModelScope.launch(ioDispatcher) { storage.deleteStagedPage(page) }
        }

        fun markPreviewFailed(
            pageId: String,
            sessionId: String? = _uiState.value.sessionId,
        ) {
            if (!isCurrentSession(sessionId) || _uiState.value.attachmentPending) return
            val pageExists =
                _uiState.value.selection.pages
                    .any { it.id == pageId }
            if (!pageExists) return
            _uiState.update {
                it.copy(
                    phase = if (it.isBusy) it.phase else PatternImageImportPhase.ERROR,
                    error = PatternImageImportError.UNSUPPORTED,
                    invalidPageIds = it.invalidPageIds + pageId,
                )
            }
        }

        fun createPatternPdf(sessionId: String? = _uiState.value.sessionId) {
            if (!isCurrentSession(sessionId) || activeOperationId != null) return
            if (_uiState.value.pickerRequestId != null || _uiState.value.cameraCapturePending) return
            startCreation()
        }

        fun consumeCloseRequest(sessionId: String? = _uiState.value.sessionId): Boolean {
            if (!isCurrentSession(sessionId) || !_uiState.value.closeReady) return false
            _uiState.update { it.copy(closeReady = false) }
            return true
        }

        fun cancelImport(sessionId: String? = _uiState.value.sessionId) {
            if (!isCurrentSession(sessionId)) return
            if (_uiState.value.phase == PatternImageImportPhase.SUCCESS ||
                _uiState.value.phase == PatternImageImportPhase.CANCELLED
            ) {
                return
            }
            analytics.track(UsageEvent.PDF_IMPORT_CANCELLED, _uiState.value.origin.analyticsDetail())
            if (activeOperationId != null) {
                operationJob?.cancel()
                return
            }
            val projectId = _uiState.value.projectId ?: return
            sessionId ?: return
            _uiState.update { it.copy(pickerRequestId = null, pickerLaunched = false, cameraCapturePending = false) }
            persistState()
            launchOperation { operationId -> finishCancelled(projectId, sessionId, operationId) }
        }

        private fun startCreation() {
            if (activeOperationId != null) return
            if (_uiState.value.pickerRequestId != null || _uiState.value.cameraCapturePending) return
            val startState = _uiState.value.beginConversion() ?: return
            val projectId = startState.projectId ?: return
            val sessionId = startState.sessionId ?: return
            analytics.track(UsageEvent.PDF_IMPORT_STARTED, _uiState.value.origin.analyticsDetail())
            _uiState.value = startState
            launchOperation { operationId ->
                createAndAttachPattern(startState, projectId, sessionId, operationId)
            }
        }

        @Suppress("SwallowedException", "TooGenericExceptionCaught")
        private suspend fun createAndAttachPattern(
            startState: PatternImageImportUiState,
            projectId: Long,
            sessionId: String,
            operationId: String,
        ) {
            var uncommittedOutputUri: String? = null
            try {
                val pendingOutput = savedStateHandle.get<ArrayList<String>>(KEY_PENDING_OUTPUT)
                val pendingPdf = pendingOutput?.takeIf { it.size == 2 }?.let { it[0] to it[1] }
                if (pendingPdf == null && !hasCreationSpace(startState)) {
                    showRecoverableError(PatternImageImportError.LOW_STORAGE, operationId)
                    return
                }
                val output =
                    pendingPdf ?: withContext(ioDispatcher) {
                        storage
                            .convertImagesToPdf(
                                context = context,
                                projectId = projectId,
                                pages = startState.selection.pages,
                                fileName = "pattern-images-${System.currentTimeMillis()}.pdf",
                            ) { current, total ->
                                updateOperation(operationId) { it.withProgress(current, total) }
                            }.also { uncommittedOutputUri = it.first }
                    }
                savedStateHandle[KEY_PENDING_OUTPUT] = arrayListOf(output.first, output.second)
                uncommittedOutputUri = null
                updateOperation(operationId) {
                    it.copy(phase = PatternImageImportPhase.ATTACHING, progress = null, attachmentPending = true)
                }
                val attached =
                    try {
                        attachGeneratedPattern(projectId, output)
                    } catch (failure: CancellationException) {
                        // The attachment check confirmed that cancellation did not commit the file.
                        uncommittedOutputUri = output.first
                        throw failure
                    }
                if (attached != true) {
                    if (attached == false) {
                        savedStateHandle.remove<ArrayList<String>>(KEY_PENDING_OUTPUT)
                        updateOperation(operationId) { it.copy(attachmentPending = false) }
                        deleteGeneratedPdf(output.first)
                    }
                    showRecoverableError(PatternImageImportError.ATTACHMENT, operationId)
                    return
                }
                analytics.track(UsageEvent.PDF_IMPORT_SUCCEEDED, _uiState.value.origin.analyticsDetail())
                recordTrialPhotoPdfCreated()
                finishImport(projectId, sessionId, PatternImageImportPhase.SUCCESS, operationId)
            } catch (failure: CancellationException) {
                withContext(NonCancellable) {
                    deleteGeneratedPdf(uncommittedOutputUri)
                    finishCancelled(projectId, sessionId, operationId)
                }
            } catch (failure: Exception) {
                deleteGeneratedPdf(uncommittedOutputUri)
                showRecoverableFailure(PatternImageImportError.CONVERSION, failure, operationId)
            }
        }

        private fun recordTrialPhotoPdfCreated() {
            if (!proManager.initialStateReady.value) return
            val proState = proManager.proState.value
            if (proState.status != ProStatus.TRIAL_ACTIVE || proState.trialStartTimestamp <= 0L) return
            applicationScope.launch {
                try {
                    onboardingStateRepository.markTrialPhotoPdfCreated(proState.trialStartTimestamp)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    // Kokeilulistan kirjausvirhe ei muuta onnistunutta PDF-tuontia virheeksi.
                }
            }
        }

        private suspend fun hasCreationSpace(state: PatternImageImportUiState): Boolean =
            withContext(ioDispatcher) {
                storage.hasCreationSpace(context, state.selection.totalBytes)
            }

        private suspend fun attachGeneratedPattern(
            projectId: Long,
            output: Pair<String, String>,
        ): Boolean? {
            val attachment =
                runCatching {
                    val project = withContext(ioDispatcher) { repository.getProject(projectId) }
                    val label =
                        if (project != null) {
                            context.getString(R.string.pattern_images_document_label, project.name)
                        } else {
                            context.getString(R.string.pattern_images_document_fallback)
                        }
                    repository.attachPattern(
                        id = projectId,
                        patternUri = output.first,
                        patternName = PatternDisplayNames.documentLabel(label),
                        currentPatternPage = 0,
                        patternRowMapping = null,
                        makeLabelUnique = true,
                    )
                }
            when (attachment.getOrNull()) {
                is ProjectDocumentMutationResult.Added,
                ProjectDocumentMutationResult.AlreadyAttached,
                ProjectDocumentMutationResult.DuplicateUri,
                ProjectDocumentMutationResult.DuplicateDocumentKey,
                -> return true
                else -> Unit
            }
            val failure = attachment.exceptionOrNull()

            val committed =
                withContext(NonCancellable) {
                    runCatching { repository.isPatternDocumentAttached(projectId, output.first) }
                        .getOrNull()
                }
            // An unreadable attachment state must not authorize deleting a potentially committed PDF.
            if (committed == null) return null
            if (committed) return true
            if (failure is CancellationException) throw failure
            return false
        }

        private suspend fun deleteGeneratedPdf(uri: String?) {
            uri ?: return
            withContext(ioDispatcher) { storage.deleteGeneratedPdf(context, uri) }
        }

        private suspend fun finishCancelled(
            projectId: Long,
            sessionId: String,
            operationId: String,
        ) = finishImport(projectId, sessionId, PatternImageImportPhase.CANCELLED, operationId)

        private suspend fun finishImport(
            projectId: Long,
            sessionId: String,
            phase: PatternImageImportPhase,
            operationId: String,
        ) = withContext(NonCancellable) {
            withContext(ioDispatcher) { storage.deleteImportSession(context, projectId, sessionId) }
            if (activeOperationId == operationId && isCurrentSession(sessionId)) {
                clearSavedState()
                _uiState.value =
                    PatternImageImportUiState(
                        projectId = projectId,
                        sessionId = sessionId,
                        phase = phase,
                        closeReady = true,
                    )
            }
        }

        private fun updateSelection(transform: (PatternImageSelection) -> PatternImageSelection) {
            _uiState.update { state ->
                val selection = transform(state.selection)
                state.copy(
                    selection = selection,
                    phase = if (state.invalidPageIds.isEmpty()) selectionPhase(selection.pages) else state.phase,
                    error = PatternImageImportError.UNSUPPORTED.takeIf { state.invalidPageIds.isNotEmpty() },
                )
            }
            persistState()
        }

        private fun showRecoverableError(
            error: PatternImageImportError,
            operationId: String,
        ) {
            if (operationId != activeOperationId) return
            analytics.track(
                UsageEvent.PDF_IMPORT_FAILED,
                _uiState.value.origin.analyticsDetail(),
                error.analyticsDetail(),
            )
            updateOperation(
                operationId,
            ) { it.copy(phase = PatternImageImportPhase.ERROR, error = error, progress = null) }
        }

        private fun showRecoverableFailure(
            fallback: PatternImageImportError,
            failure: Exception,
            operationId: String,
        ) {
            val error =
                if (failure is PatternImageValidationException) {
                    failure.reason.toImportError()
                } else {
                    fallback
                }
            showRecoverableError(error, operationId)
        }

        private fun ensureSession(
            projectId: Long,
            origin: PatternImageImportOrigin,
        ): Boolean {
            val current = _uiState.value
            if (ownerProjectId != null && ownerProjectId != projectId) return false
            if (activeOperationId != null) return false
            val finished =
                current.phase == PatternImageImportPhase.SUCCESS || current.phase == PatternImageImportPhase.CANCELLED
            if (!finished && current.sessionId != null) {
                if (current.origin == origin) return true
                if (current.selection.pages.isNotEmpty() ||
                    current.pickerRequestId != null ||
                    current.cameraCapturePending
                ) {
                    return false
                }
            }
            ownerProjectId = projectId
            _uiState.value =
                PatternImageImportUiState(
                    projectId = projectId,
                    sessionId = UUID.randomUUID().toString(),
                    origin = origin,
                    phase = PatternImageImportPhase.IDLE,
                )
            persistState()
            return true
        }

        fun isCurrentSession(sessionId: String?): Boolean = sessionId != null && _uiState.value.sessionId == sessionId

        private fun launchOperation(block: suspend (String) -> Unit) {
            val operationId = UUID.randomUUID().toString()
            activeOperationId = operationId
            operationJob =
                viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
                    try {
                        block(operationId)
                    } finally {
                        if (activeOperationId == operationId) activeOperationId = null
                    }
                }
        }

        private fun updateOperation(
            operationId: String,
            transform: (PatternImageImportUiState) -> PatternImageImportUiState,
        ) {
            _uiState.update { if (activeOperationId == operationId) transform(it) else it }
        }

        private fun persistState() {
            val state = _uiState.value
            savedStateHandle[KEY_PROJECT_ID] = state.projectId
            savedStateHandle[KEY_SESSION_ID] = state.sessionId
            if (!restoringDraft) savedStateHandle[KEY_PAGES] = encodeStagedPatternPages(state.selection.pages)
            savedStateHandle[KEY_ORIGIN] = state.origin.name
            savedStateHandle[KEY_PICKER_REQUEST_ID] = state.pickerRequestId
            savedStateHandle[KEY_PICKER_LAUNCHED] = state.pickerLaunched
            savedStateHandle[KEY_CAMERA_PENDING] = state.cameraCapturePending
        }

        private fun clearSavedState() {
            savedStateHandle.remove<Long>(KEY_PROJECT_ID)
            savedStateHandle.remove<String>(KEY_SESSION_ID)
            savedStateHandle.remove<String>(KEY_PAGES)
            savedStateHandle.remove<String>(KEY_ORIGIN)
            savedStateHandle.remove<String>(KEY_PICKER_REQUEST_ID)
            savedStateHandle.remove<Boolean>(KEY_PICKER_LAUNCHED)
            savedStateHandle.remove<Boolean>(KEY_CAMERA_PENDING)
            savedStateHandle.remove<ArrayList<String>>(KEY_PENDING_OUTPUT)
        }

        private companion object {
            const val KEY_PROJECT_ID = "pattern_image_project_id"
            const val KEY_SESSION_ID = "pattern_image_session_id"
            const val KEY_PAGES = "pattern_image_pages"
            const val KEY_ORIGIN = "pattern_image_origin"
            const val KEY_PICKER_REQUEST_ID = "pattern_image_picker_request_id"
            const val KEY_PICKER_LAUNCHED = "pattern_image_picker_launched"
            const val KEY_CAMERA_PENDING = "pattern_image_camera_pending"
            const val KEY_PENDING_OUTPUT = "pattern_image_pending_output"

            fun restoredState(handle: SavedStateHandle): PatternImageImportUiState {
                val projectId = handle.get<Long>(KEY_PROJECT_ID)
                val sessionId = handle.get<String>(KEY_SESSION_ID)
                val pages = decodeStagedPatternPages(handle.get<String>(KEY_PAGES).orEmpty())
                val attachmentPending = handle.get<ArrayList<String>>(KEY_PENDING_OUTPUT)?.size == 2
                val origin =
                    runCatching {
                        PatternImageImportOrigin.valueOf(handle.get<String>(KEY_ORIGIN).orEmpty())
                    }.getOrDefault(PatternImageImportOrigin.GALLERY)
                return PatternImageImportUiState(
                    projectId = projectId,
                    sessionId = sessionId,
                    origin = origin,
                    pickerRequestId = handle.get<String>(KEY_PICKER_REQUEST_ID),
                    pickerLaunched =
                        handle.get<Boolean>(KEY_PICKER_LAUNCHED) ?: (handle.get<String>(KEY_PICKER_REQUEST_ID) != null),
                    cameraCapturePending =
                        handle.get<Boolean>(KEY_CAMERA_PENDING)
                            ?: (origin == PatternImageImportOrigin.CAMERA && pages.isEmpty()),
                    selection = PatternImageSelection(pages),
                    phase = if (attachmentPending) PatternImageImportPhase.ERROR else selectionPhase(pages),
                    error = PatternImageImportError.ATTACHMENT.takeIf { attachmentPending },
                    attachmentPending = attachmentPending,
                )
            }
        }
    }

private fun cameraPage(
    imageUri: Uri,
    imageFile: File,
) = StagedPatternPage(
    id = imageFile.name,
    sourceUri = imageUri.toString(),
    stagedPath = imageFile.absolutePath,
    byteCount = imageFile.length(),
    width = 1,
    height = 1,
)

private fun PatternImageImportUiState.withRestoredSelection(
    restoredSelection: PatternImageSelection?,
    expectedPageCount: Int,
): PatternImageImportUiState {
    val selection = restoredSelection ?: PatternImageSelection()
    val rejectedPages = selection.pages.size != expectedPageCount
    val restoredError = if (rejectedPages) PatternImageImportError.UNREADABLE else this.error
    return copy(
        selection = selection,
        sessionId = if (restoredSelection != null) sessionId else UUID.randomUUID().toString(),
        cameraCapturePending = restoredSelection != null && cameraCapturePending,
        phase = if (restoredError != null) PatternImageImportPhase.ERROR else selectionPhase(selection.pages),
        error = restoredError,
    )
}

private fun PatternImageImportOrigin.analyticsDetail(): UsageDetail =
    if (this == PatternImageImportOrigin.CAMERA) UsageDetail.SOURCE_CAMERA else UsageDetail.SOURCE_GALLERY

private fun PatternImageImportError.analyticsDetail(): UsageDetail =
    when (this) {
        PatternImageImportError.PAGE_LIMIT,
        PatternImageImportError.IMAGE_TOO_LARGE,
        PatternImageImportError.TOTAL_TOO_LARGE,
        PatternImageImportError.LOW_STORAGE,
        -> UsageDetail.ERROR_TOO_LARGE
        PatternImageImportError.UNREADABLE -> UsageDetail.ERROR_READ
        PatternImageImportError.UNSUPPORTED, PatternImageImportError.ANIMATED -> UsageDetail.ERROR_INVALID
        else -> UsageDetail.ERROR_WRITE
    }

private fun selectionPhase(pages: List<StagedPatternPage>): PatternImageImportPhase =
    if (pages.isEmpty()) PatternImageImportPhase.IDLE else PatternImageImportPhase.READY

private fun PatternImageStageFailure.toImportError(): PatternImageImportError =
    when (this) {
        PatternImageStageFailure.PAGE_LIMIT -> PatternImageImportError.PAGE_LIMIT
        PatternImageStageFailure.IMAGE_TOO_LARGE -> PatternImageImportError.IMAGE_TOO_LARGE
        PatternImageStageFailure.TOTAL_TOO_LARGE -> PatternImageImportError.TOTAL_TOO_LARGE
        PatternImageStageFailure.LOW_STORAGE -> PatternImageImportError.LOW_STORAGE
        PatternImageStageFailure.UNREADABLE -> PatternImageImportError.UNREADABLE
        PatternImageStageFailure.UNSUPPORTED -> PatternImageImportError.UNSUPPORTED
        PatternImageStageFailure.ANIMATED -> PatternImageImportError.ANIMATED
    }

private fun PatternImageFailureReason.toImportError(): PatternImageImportError =
    when (this) {
        PatternImageFailureReason.ANIMATED -> PatternImageImportError.ANIMATED

        PatternImageFailureReason.UNSUPPORTED,
        PatternImageFailureReason.INVALID_DIMENSIONS,
        -> PatternImageImportError.UNSUPPORTED
    }
