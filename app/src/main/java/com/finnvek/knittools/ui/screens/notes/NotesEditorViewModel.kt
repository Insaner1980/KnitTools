package com.finnvek.knittools.ui.screens.notes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.knittools.analytics.UsageAnalytics
import com.finnvek.knittools.analytics.UsageEvent
import com.finnvek.knittools.data.storage.NotesDraft
import com.finnvek.knittools.data.storage.NotesDraftStorage
import com.finnvek.knittools.di.ApplicationScope
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.repository.ProjectNotesSaveResult
import com.finnvek.knittools.repository.mergeProjectNotes
import com.finnvek.knittools.ui.navigation.toPositiveRouteIdOrNull
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import javax.inject.Inject

data class NotesEditorUiState(
    val projectName: String = "",
    val notes: String = "",
    val notesCreated: Boolean = false,
    val isLoaded: Boolean = false,
    val canEditNotes: Boolean = false,
    val isMissingProject: Boolean = false,
    val saveFailed: Boolean = false,
    val exitRequested: Boolean = false,
)

@HiltViewModel
class NotesEditorViewModel
    @Inject
    constructor(
        private val repository: CounterRepository,
        private val proManager: ProManager,
        @param:ApplicationScope private val applicationScope: CoroutineScope,
        private val savedStateHandle: SavedStateHandle,
        private val draftStorage: NotesDraftStorage,
        private val analytics: UsageAnalytics = UsageAnalytics.NONE,
    ) : ViewModel() {
        val proState = proManager.proState
        val proAccessReady get() = proManager.initialStateReady
        private val projectId: Long? = savedStateHandle.get<Long>("projectId")?.toPositiveRouteIdOrNull()

        private val _uiState = MutableStateFlow(NotesEditorUiState())
        val uiState: StateFlow<NotesEditorUiState> = _uiState.asStateFlow()

        private var saveJob: Job? = null
        private var activeWrite: NotesWrite? = null
        private var draftWriteJob: Job? = null
        private val draftFileMutex = Mutex()
        private var persistedNotes: String = ""
        private var latestStoredNotes: String = ""
        private var notesObservationVersion = 0L
        private var hasLocalEdits: Boolean = false

        init {
            viewModelScope.launch {
                val loadedProjectId = projectId
                if (loadedProjectId == null) {
                    _uiState.update { it.copy(isMissingProject = true) }
                    return@launch
                }
                val projects =
                    repository.observeProject(loadedProjectId).onEach { project ->
                        if (project != null) {
                            latestStoredNotes = project.notes
                            notesObservationVersion++
                        }
                    }
                combine(projects, proState) { project, _ ->
                    project
                }.collect { project ->
                    if (project == null) {
                        _uiState.update { it.copy(isMissingProject = true) }
                        return@collect
                    }
                    val canEditNotes =
                        project.notesCreated ||
                            proManager.hasFeature(ProFeature.NOTES) ||
                            savedStateHandle.get<Boolean>(NOTES_CREATION_AUTHORIZED_KEY) == true
                    if (!canEditNotes) {
                        _uiState.update { state ->
                            state.copy(
                                projectName = project.name,
                                notesCreated = project.notesCreated,
                                isLoaded = true,
                                canEditNotes = false,
                                isMissingProject = false,
                            )
                        }
                        return@collect
                    }
                    val restored =
                        if (!_uiState.value.isLoaded || !_uiState.value.canEditNotes) {
                            restoreDraft(loadedProjectId)
                        } else {
                            null
                        }
                    val restoredDraft = restored?.notes
                    val hasRestoredDraft = restoredDraft != null && restoredDraft != project.notes
                    if (restoredDraft == project.notes) {
                        clearSavedDraft()
                    }
                    val shouldAdoptNotes =
                        hasRestoredDraft ||
                            !_uiState.value.isLoaded ||
                            !hasLocalEdits ||
                            _uiState.value.notes == persistedNotes
                    if (shouldAdoptNotes) {
                        persistedNotes =
                            if (hasRestoredDraft) {
                                restored.baseNotes ?: project.notes
                            } else {
                                project.notes
                            }
                        hasLocalEdits = hasRestoredDraft
                    }
                    _uiState.update { state ->
                        state.copy(
                            projectName = project.name,
                            notesCreated = project.notesCreated,
                            notes =
                                when {
                                    hasRestoredDraft -> restoredDraft.orEmpty()
                                    shouldAdoptNotes -> project.notes
                                    else -> state.notes
                                },
                            isLoaded = true,
                            canEditNotes = canEditNotes,
                            isMissingProject = false,
                        )
                    }
                    if (hasRestoredDraft) {
                        if (savedStateHandle.contains(NOTES_DRAFT_KEY) &&
                            !fitsSavedState(restoredDraft.orEmpty(), persistedNotes)
                        ) {
                            saveDraft(restoredDraft.orEmpty(), persistedNotes)
                        }
                        scheduleSave(loadedProjectId)
                    }
                }
            }
        }

        fun onNotesChanged(text: String) {
            val loadedProjectId = projectId ?: return
            val state = _uiState.value
            if (state.isMissingProject || !state.isLoaded || !state.canEditNotes) return
            _uiState.update { it.copy(notes = text, exitRequested = false) }
            hasLocalEdits = text != persistedNotes
            saveJob?.cancel()
            if (!hasLocalEdits) {
                persistedNotes = latestStoredNotes
                clearSavedDraft()
                _uiState.update { it.copy(notes = persistedNotes, saveFailed = false) }
                return
            }
            saveDraft(text, persistedNotes)
            scheduleSave(loadedProjectId)
        }

        fun authorizeFirstNotesCreation() {
            savedStateHandle[NOTES_CREATION_AUTHORIZED_KEY] = true
            _uiState.update { it.copy(canEditNotes = true) }
        }

        fun cancelFirstNotesCreation() {
            if (persistedNotes.isEmpty() && _uiState.value.notes.isEmpty()) {
                savedStateHandle.remove<Boolean>(NOTES_CREATION_AUTHORIZED_KEY)
                _uiState.update { it.copy(canEditNotes = false) }
            }
        }

        fun saveImmediately(exitAfterSave: Boolean = false) {
            val loadedProjectId = projectId
            val state = _uiState.value
            if (loadedProjectId == null || state.isMissingProject || !state.isLoaded || !state.canEditNotes) {
                if (exitAfterSave) requestExit()
                return
            }
            val write = activeWrite
            if (write != null && saveJob?.isActive == true) {
                if (exitAfterSave) write.exitAfterSave = true
                return
            }
            if (!hasLocalEdits) {
                if (exitAfterSave) requestExit()
                return
            }
            scheduleSave(loadedProjectId, debounce = false, exitAfterSave = exitAfterSave)
        }

        private fun requestExit() {
            _uiState.update { it.copy(exitRequested = true) }
        }

        fun consumeExitRequest(): Boolean = _uiState.getAndUpdate { it.copy(exitRequested = false) }.exitRequested

        private var saveReported = false

        private suspend fun persistNotes(
            loadedProjectId: Long,
            requestedNotes: String,
        ): Boolean {
            val observationVersion = notesObservationVersion
            val result =
                repository.saveProjectNotes(
                    id = loadedProjectId,
                    baseNotes = persistedNotes,
                    requestedNotes = requestedNotes,
                    creationAuthorized =
                        savedStateHandle.get<Boolean>(NOTES_CREATION_AUTHORIZED_KEY) == true,
                )
            val savedProject =
                when (result) {
                    is ProjectNotesSaveResult.Saved -> {
                        result.project
                    }

                    ProjectNotesSaveResult.ProjectUnavailable -> {
                        _uiState.update { it.copy(isMissingProject = true) }
                        return false
                    }

                    ProjectNotesSaveResult.FeatureUnavailable -> {
                        _uiState.update { it.copy(canEditNotes = false) }
                        return false
                    }

                    ProjectNotesSaveResult.PersistenceFailure -> {
                        _uiState.update { it.copy(saveFailed = true) }
                        return false
                    }
                }
            if (!saveReported) {
                saveReported = true
                analytics.track(UsageEvent.NOTES_SAVED)
            }
            persistedNotes = savedProject.notes
            if (observationVersion == notesObservationVersion) latestStoredNotes = savedProject.notes
            if (savedProject.notesCreated) {
                savedStateHandle.remove<Boolean>(NOTES_CREATION_AUTHORIZED_KEY)
            }
            val notes =
                mergeProjectNotes(
                    baseNotes = requestedNotes,
                    requestedNotes = _uiState.value.notes,
                    currentNotes = persistedNotes,
                )
            hasLocalEdits = notes != persistedNotes
            if (hasLocalEdits) {
                saveDraft(notes, persistedNotes)
            } else {
                clearSavedDraft()
            }
            _uiState.update { state ->
                state.copy(
                    projectName = savedProject.name,
                    notesCreated = savedProject.notesCreated,
                    notes = notes,
                    isMissingProject = false,
                    saveFailed = false,
                )
            }
            return true
        }

        private fun scheduleSave(
            loadedProjectId: Long,
            debounce: Boolean = true,
            exitAfterSave: Boolean = false,
        ) {
            saveJob?.cancel()
            val write = NotesWrite(exitAfterSave)
            activeWrite = if (debounce) null else write
            saveJob =
                viewModelScope.launch {
                    if (debounce) delay(DEBOUNCE_MS)
                    activeWrite = write
                    try {
                        val saved = !hasLocalEdits || persistNotes(loadedProjectId, _uiState.value.notes)
                        if (saved && isActive && write.exitAfterSave) requestExit()
                    } finally {
                        if (activeWrite === write) activeWrite = null
                    }
                }
        }

        fun discardChanges() {
            saveJob?.cancel()
            persistedNotes = latestStoredNotes
            hasLocalEdits = false
            clearSavedDraft()
            _uiState.update { it.copy(notes = persistedNotes, saveFailed = false, exitRequested = false) }
        }

        private fun saveDraft(
            notes: String,
            baseNotes: String,
        ) {
            val loadedProjectId = projectId ?: return
            if (fitsSavedState(notes, baseNotes)) {
                clearDraftFile()
                savedStateHandle[NOTES_DRAFT_KEY] = notes
                savedStateHandle[NOTES_DRAFT_BASE_KEY] = baseNotes
                return
            }
            val fileId = draftFileId() ?: UUID.randomUUID()
            savedStateHandle[NOTES_DRAFT_FILE_ID_KEY] = fileId.toString()
            draftWriteJob?.cancel()
            draftWriteJob =
                viewModelScope.launch {
                    draftFileMutex.withLock {
                        if (draftStorage.write(loadedProjectId, fileId, NotesDraft(notes, baseNotes))) {
                            ensureActive()
                            savedStateHandle.remove<String>(NOTES_DRAFT_KEY)
                            savedStateHandle.remove<String>(NOTES_DRAFT_BASE_KEY)
                        }
                    }
                }
        }

        private suspend fun restoreDraft(loadedProjectId: Long): NotesDraft? {
            val diskDraft = draftFileId()?.let { draftStorage.read(loadedProjectId, it) }
            return diskDraft ?: savedStateHandle.get<String>(NOTES_DRAFT_KEY)?.let {
                NotesDraft(it, savedStateHandle[NOTES_DRAFT_BASE_KEY])
            }
        }

        private fun draftFileId(): UUID? =
            savedStateHandle.get<String>(NOTES_DRAFT_FILE_ID_KEY)?.let { value ->
                runCatching { UUID.fromString(value) }.getOrNull()
            }

        private fun clearDraftFile() {
            draftWriteJob?.cancel()
            val loadedProjectId = projectId ?: return
            val fileId = draftFileId() ?: return
            savedStateHandle.remove<String>(NOTES_DRAFT_FILE_ID_KEY)
            applicationScope.launch {
                draftFileMutex.withLock { draftStorage.delete(loadedProjectId, fileId) }
            }
        }

        private fun clearSavedDraft() {
            savedStateHandle.remove<String>(NOTES_DRAFT_KEY)
            savedStateHandle.remove<String>(NOTES_DRAFT_BASE_KEY)
            clearDraftFile()
        }

        private fun fitsSavedState(
            notes: String,
            baseNotes: String,
        ): Boolean = notes.length.toLong() + baseNotes.length <= MAX_INLINE_DRAFT_CHARS

        override fun onCleared() {
            val state = _uiState.value
            val loadedProjectId = projectId ?: return
            val shouldFlush =
                state.isLoaded &&
                    state.canEditNotes &&
                    !state.isMissingProject &&
                    hasLocalEdits
            val notesToSave = state.notes
            val baseNotes = persistedNotes
            if (!shouldFlush) return
            @Suppress("TooGenericExceptionCaught")
            applicationScope.launch {
                try {
                    val result =
                        repository.saveProjectNotes(
                            id = loadedProjectId,
                            baseNotes = baseNotes,
                            requestedNotes = notesToSave,
                            creationAuthorized =
                                savedStateHandle.get<Boolean>(NOTES_CREATION_AUTHORIZED_KEY) == true,
                        )
                    if (result is ProjectNotesSaveResult.Saved) clearDraftFile()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    // Viimeinen poistumistallennus ei saa kaataa sovellusta.
                }
            }
        }

        private class NotesWrite(
            var exitAfterSave: Boolean,
        )

        companion object {
            private const val DEBOUNCE_MS = 1000L
            private const val MAX_INLINE_DRAFT_CHARS = 16_384L
            private const val NOTES_DRAFT_KEY = "notesDraft"
            private const val NOTES_DRAFT_BASE_KEY = "notesDraftBase"
            private const val NOTES_DRAFT_FILE_ID_KEY = "notesDraftFileId"
            private const val NOTES_CREATION_AUTHORIZED_KEY = "notesCreationAuthorized"
        }
    }
