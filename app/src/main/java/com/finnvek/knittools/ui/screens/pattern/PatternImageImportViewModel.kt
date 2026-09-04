package com.finnvek.knittools.ui.screens.pattern

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.knittools.data.storage.PatternDocumentStorage
import com.finnvek.knittools.data.storage.PatternImageFailureReason
import com.finnvek.knittools.data.storage.PatternImageStageException
import com.finnvek.knittools.data.storage.PatternImageStageFailure
import com.finnvek.knittools.data.storage.PatternImageValidationException
import com.finnvek.knittools.di.IoDispatcher
import com.finnvek.knittools.repository.CounterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
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
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(restoredState(savedStateHandle))
        val uiState: StateFlow<PatternImageImportUiState> = _uiState.asStateFlow()

        private var operationJob: Job? = null

        fun authorizeGalleryPicker(projectId: Long): String {
            ensureSession(projectId, PatternImageImportOrigin.GALLERY)
            val requestId = UUID.randomUUID().toString()
            _uiState.update { it.copy(pickerRequestId = requestId, closeReady = false) }
            persistState()
            return requestId
        }

        fun authorizeCameraCapture(projectId: Long): String {
            ensureSession(projectId, PatternImageImportOrigin.CAMERA)
            return _uiState.value.sessionId.orEmpty()
        }

        @Suppress("kotlin:S6313") // Activity tarvitsee FileProvider-kohteen ennen kamerasopimusta.
        suspend fun createCameraCaptureTarget(projectId: Long): Pair<File, Uri>? {
            val state = _uiState.value
            if (state.projectId != projectId || state.origin != PatternImageImportOrigin.CAMERA) return null
            val sessionId = state.sessionId ?: return null
            return withContext(ioDispatcher) {
                storage.createCaptureImageFile(context, projectId, sessionId)
            }
        }

        fun discardCameraCapture(
            imageUri: Uri,
            imageFile: File,
        ) {
            viewModelScope.launch(ioDispatcher) { storage.deleteStagedPage(cameraPage(imageUri, imageFile)) }
        }

        @Suppress("SwallowedException")
        fun onGalleryPickerResult(
            requestId: String?,
            sourceUris: List<Uri>,
        ) {
            val state = _uiState.value
            if (requestId == null || requestId != state.pickerRequestId || state.isBusy) return
            _uiState.update { it.copy(pickerRequestId = null) }
            persistState()
            if (sourceUris.isEmpty()) return
            val projectId = state.projectId ?: return
            val sessionId = state.sessionId ?: return
            operationJob =
                viewModelScope.launch {
                    _uiState.update { it.copy(phase = PatternImageImportPhase.STAGING, error = null) }
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
                        val appended = state.selection.append(batch.pages)
                        val selection =
                            (appended as? PatternImageAppendResult.Accepted)?.selection
                                ?: state.selection
                        val duplicateCount =
                            batch.duplicatesIgnored +
                                ((appended as? PatternImageAppendResult.Accepted)?.duplicatesIgnored ?: 0)
                        _uiState.update {
                            it.copy(
                                selection = selection,
                                phase =
                                    if (selection.pages.isEmpty()) {
                                        PatternImageImportPhase.IDLE
                                    } else {
                                        PatternImageImportPhase.READY
                                    },
                                duplicatesIgnored = duplicateCount,
                            )
                        }
                        persistState()
                    } catch (failure: CancellationException) {
                        finishCancelled(projectId, sessionId)
                    } catch (failure: PatternImageStageException) {
                        showRecoverableError(failure.reason.toImportError())
                    }
                }
        }

        @Suppress("SwallowedException")
        fun acceptCameraCapture(
            projectId: Long,
            imageUri: Uri,
            imageFile: File,
        ) {
            ensureSession(projectId, PatternImageImportOrigin.CAMERA)
            operationJob =
                viewModelScope.launch {
                    _uiState.update { it.copy(phase = PatternImageImportPhase.STAGING, error = null) }
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
                        _uiState.update {
                            it.copy(
                                selection = PatternImageSelection(listOf(page)),
                                phase = PatternImageImportPhase.READY,
                            )
                        }
                        persistState()
                    } catch (failure: CancellationException) {
                        finishCancelled(projectId, _uiState.value.sessionId.orEmpty())
                    } catch (failure: PatternImageValidationException) {
                        withContext(ioDispatcher) { storage.deleteStagedPage(cameraPage(imageUri, imageFile)) }
                        showRecoverableFailure(PatternImageImportError.UNSUPPORTED, failure)
                    } catch (failure: PatternImageStageException) {
                        withContext(ioDispatcher) { storage.deleteStagedPage(cameraPage(imageUri, imageFile)) }
                        showRecoverableError(failure.reason.toImportError())
                    }
                }
        }

        fun moveEarlier(pageId: String) {
            updateSelection { selection ->
                val index = selection.pages.indexOfFirst { it.id == pageId }
                selection.moveEarlier(index)
            }
        }

        fun moveLater(pageId: String) {
            updateSelection { selection ->
                val index = selection.pages.indexOfFirst { it.id == pageId }
                selection.moveLater(index)
            }
        }

        fun removePage(pageId: String) {
            val page =
                _uiState.value.selection.pages
                    .firstOrNull { it.id == pageId } ?: return
            updateSelection { it.remove(pageId) }
            _uiState.update { it.copy(invalidPageIds = it.invalidPageIds - pageId) }
            viewModelScope.launch(ioDispatcher) { storage.deleteStagedPage(page) }
        }

        fun markPreviewFailed(pageId: String) {
            val pageExists =
                _uiState.value.selection.pages
                    .any { it.id == pageId }
            if (!pageExists) return
            _uiState.update {
                it.copy(
                    phase = PatternImageImportPhase.ERROR,
                    error = PatternImageImportError.UNSUPPORTED,
                    invalidPageIds = it.invalidPageIds + pageId,
                )
            }
        }

        fun clearDuplicateNotice() {
            _uiState.update { it.copy(duplicatesIgnored = 0) }
        }

        fun createPatternPdf(replaceExisting: Boolean) {
            if (replaceExisting) {
                _uiState.update { it.copy(replacementConfirmationPending = true) }
            } else {
                startCreation()
            }
        }

        fun confirmReplacement() {
            startCreation()
        }

        fun dismissReplacement() {
            _uiState.update { it.copy(replacementConfirmationPending = false) }
        }

        fun consumeCloseRequest() {
            _uiState.update { it.copy(closeReady = false) }
        }

        fun cancelImport() {
            val activeJob = operationJob
            if (activeJob?.isActive == true) {
                viewModelScope.launch {
                    activeJob.cancelAndJoin()
                }
                return
            }
            val projectId = _uiState.value.projectId ?: return
            val sessionId = _uiState.value.sessionId ?: return
            operationJob = viewModelScope.launch { finishCancelled(projectId, sessionId) }
        }

        private fun startCreation() {
            if (operationJob?.isActive == true) return
            val startState = _uiState.value.beginConversion() ?: return
            val projectId = startState.projectId ?: return
            val sessionId = startState.sessionId ?: return
            _uiState.value = startState
            operationJob =
                viewModelScope.launch {
                    createAndAttachPattern(startState, projectId, sessionId)
                }
        }

        @Suppress("SwallowedException", "TooGenericExceptionCaught")
        private suspend fun createAndAttachPattern(
            startState: PatternImageImportUiState,
            projectId: Long,
            sessionId: String,
        ) {
            var uncommittedOutputUri: String? = null
            try {
                if (!hasCreationSpace(startState)) {
                    showRecoverableError(PatternImageImportError.LOW_STORAGE)
                    return
                }
                val output = convertImages(startState, projectId)
                uncommittedOutputUri = output.first
                _uiState.update { it.copy(phase = PatternImageImportPhase.ATTACHING, progress = null) }
                if (!attachGeneratedPattern(projectId, output)) {
                    deleteGeneratedPdf(uncommittedOutputUri)
                    uncommittedOutputUri = null
                    showRecoverableError(PatternImageImportError.ATTACHMENT)
                    return
                }
                uncommittedOutputUri = null
                completeSuccessfulImport(projectId, sessionId)
            } catch (failure: CancellationException) {
                withContext(NonCancellable) {
                    deleteGeneratedPdf(uncommittedOutputUri)
                    finishCancelled(projectId, sessionId)
                }
            } catch (failure: Exception) {
                deleteGeneratedPdf(uncommittedOutputUri)
                showRecoverableFailure(PatternImageImportError.CONVERSION, failure)
            }
        }

        private suspend fun hasCreationSpace(state: PatternImageImportUiState): Boolean =
            withContext(ioDispatcher) {
                storage.hasCreationSpace(context, state.selection.totalBytes)
            }

        private suspend fun convertImages(
            state: PatternImageImportUiState,
            projectId: Long,
        ): Pair<String, String> =
            withContext(ioDispatcher) {
                storage.convertImagesToPdf(
                    context = context,
                    projectId = projectId,
                    pages = state.selection.pages,
                    fileName = "pattern-images-${System.currentTimeMillis()}.pdf",
                ) { current, total ->
                    _uiState.update { it.withProgress(current, total) }
                }
            }

        private suspend fun attachGeneratedPattern(
            projectId: Long,
            output: Pair<String, String>,
        ): Boolean {
            val failure =
                runCatching {
                    repository.attachPattern(
                        id = projectId,
                        patternUri = output.first,
                        patternName = output.second,
                        currentPatternPage = 0,
                        patternRowMapping = null,
                    )
                }.exceptionOrNull()
            if (failure == null) return true

            val committed =
                withContext(NonCancellable) {
                    runCatching { repository.isPatternDocumentAttached(projectId, output.first) }
                        .getOrDefault(false)
                }
            if (committed) return true
            if (failure is CancellationException) throw failure
            return false
        }

        private suspend fun completeSuccessfulImport(
            projectId: Long,
            sessionId: String,
        ) = finishImport(projectId, sessionId, PatternImageImportPhase.SUCCESS)

        private suspend fun deleteGeneratedPdf(uri: String?) {
            uri ?: return
            withContext(ioDispatcher) { storage.deleteGeneratedPdf(context, uri) }
        }

        private suspend fun finishCancelled(
            projectId: Long,
            sessionId: String,
        ) = finishImport(projectId, sessionId, PatternImageImportPhase.CANCELLED)

        private suspend fun finishImport(
            projectId: Long,
            sessionId: String,
            phase: PatternImageImportPhase,
        ) = withContext(NonCancellable) {
            withContext(ioDispatcher) { storage.deleteImportSession(context, projectId, sessionId) }
            clearSavedState()
            _uiState.update {
                it.copy(
                    selection = PatternImageSelection(),
                    phase = phase,
                    progress = null,
                    closeReady = true,
                )
            }
        }

        private fun updateSelection(transform: (PatternImageSelection) -> PatternImageSelection) {
            _uiState.update { state ->
                val selection = transform(state.selection)
                state.copy(
                    selection = selection,
                    phase =
                        if (selection.pages.isEmpty()) {
                            PatternImageImportPhase.IDLE
                        } else {
                            PatternImageImportPhase.READY
                        },
                    error = null,
                )
            }
            persistState()
        }

        private fun showRecoverableError(error: PatternImageImportError) {
            _uiState.update { it.copy(phase = PatternImageImportPhase.ERROR, error = error, progress = null) }
        }

        private fun showRecoverableFailure(
            fallback: PatternImageImportError,
            failure: Exception,
        ) {
            val error =
                if (failure is PatternImageValidationException) {
                    failure.reason.toImportError()
                } else {
                    fallback
                }
            showRecoverableError(error)
        }

        private fun ensureSession(
            projectId: Long,
            origin: PatternImageImportOrigin,
        ) {
            val current = _uiState.value
            if (current.projectId == projectId && current.sessionId != null && current.origin == origin) return
            _uiState.value =
                PatternImageImportUiState(
                    projectId = projectId,
                    sessionId = UUID.randomUUID().toString(),
                    origin = origin,
                    phase = PatternImageImportPhase.IDLE,
                )
            persistState()
        }

        private fun persistState() {
            val state = _uiState.value
            savedStateHandle[KEY_PROJECT_ID] = state.projectId
            savedStateHandle[KEY_SESSION_ID] = state.sessionId
            savedStateHandle[KEY_PAGES] = encodeStagedPatternPages(state.selection.pages)
            savedStateHandle[KEY_ORIGIN] = state.origin.name
            savedStateHandle[KEY_PICKER_REQUEST_ID] = state.pickerRequestId
        }

        private fun clearSavedState() {
            savedStateHandle.remove<Long>(KEY_PROJECT_ID)
            savedStateHandle.remove<String>(KEY_SESSION_ID)
            savedStateHandle.remove<String>(KEY_PAGES)
            savedStateHandle.remove<String>(KEY_ORIGIN)
            savedStateHandle.remove<String>(KEY_PICKER_REQUEST_ID)
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

        private companion object {
            const val KEY_PROJECT_ID = "pattern_image_project_id"
            const val KEY_SESSION_ID = "pattern_image_session_id"
            const val KEY_PAGES = "pattern_image_pages"
            const val KEY_ORIGIN = "pattern_image_origin"
            const val KEY_PICKER_REQUEST_ID = "pattern_image_picker_request_id"

            fun restoredState(handle: SavedStateHandle): PatternImageImportUiState {
                val projectId = handle.get<Long>(KEY_PROJECT_ID)
                val sessionId = handle.get<String>(KEY_SESSION_ID)
                val pages = decodeStagedPatternPages(handle.get<String>(KEY_PAGES).orEmpty())
                val origin =
                    runCatching {
                        PatternImageImportOrigin.valueOf(handle.get<String>(KEY_ORIGIN).orEmpty())
                    }.getOrDefault(PatternImageImportOrigin.GALLERY)
                return PatternImageImportUiState(
                    projectId = projectId,
                    sessionId = sessionId,
                    origin = origin,
                    pickerRequestId = handle.get<String>(KEY_PICKER_REQUEST_ID),
                    selection = PatternImageSelection(pages),
                    phase = if (pages.isEmpty()) PatternImageImportPhase.IDLE else PatternImageImportPhase.READY,
                )
            }
        }
    }

private fun PatternImageStageFailure.toImportError(): PatternImageImportError =
    when (this) {
        PatternImageStageFailure.PAGE_LIMIT -> PatternImageImportError.PAGE_LIMIT
        PatternImageStageFailure.IMAGE_TOO_LARGE -> PatternImageImportError.IMAGE_TOO_LARGE
        PatternImageStageFailure.TOTAL_TOO_LARGE -> PatternImageImportError.TOTAL_TOO_LARGE
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
