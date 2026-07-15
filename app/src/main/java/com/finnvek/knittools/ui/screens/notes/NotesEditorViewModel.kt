package com.finnvek.knittools.ui.screens.notes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.knittools.di.ApplicationScope
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.ui.navigation.toPositiveRouteIdOrNull
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotesEditorUiState(
    val projectName: String = "",
    val notes: String = "",
    val isLoaded: Boolean = false,
    val isPro: Boolean = false,
    val isMissingProject: Boolean = false,
)

@HiltViewModel
class NotesEditorViewModel
    @Inject
    constructor(
        private val repository: CounterRepository,
        private val proManager: ProManager,
        @param:ApplicationScope private val applicationScope: CoroutineScope,
        private val savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val projectId: Long? = savedStateHandle.get<Long>("projectId")?.toPositiveRouteIdOrNull()

        private val _uiState = MutableStateFlow(NotesEditorUiState())
        val uiState: StateFlow<NotesEditorUiState> = _uiState.asStateFlow()

        private var saveJob: Job? = null
        private var persistedNotes: String = ""
        private var hasLocalEdits: Boolean = false

        init {
            viewModelScope.launch {
                val loadedProjectId = projectId
                if (loadedProjectId == null) {
                    _uiState.update { it.copy(isMissingProject = true) }
                    return@launch
                }
                repository.observeProject(loadedProjectId).collect { project ->
                    if (project == null) {
                        _uiState.update { it.copy(isMissingProject = true) }
                        return@collect
                    }
                    val canEditNotes = proManager.hasFeature(ProFeature.NOTES)
                    if (!canEditNotes) {
                        _uiState.update { state ->
                            state.copy(
                                projectName = project.name,
                                notes = "",
                                isLoaded = true,
                                isPro = false,
                                isMissingProject = false,
                            )
                        }
                        return@collect
                    }
                    val restoredDraft =
                        if (!_uiState.value.isLoaded) {
                            savedStateHandle.get<String>(NOTES_DRAFT_KEY)
                        } else {
                            null
                        }
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
                                savedStateHandle.get<String>(NOTES_DRAFT_BASE_KEY) ?: project.notes
                            } else {
                                project.notes
                            }
                        hasLocalEdits = hasRestoredDraft
                    }
                    _uiState.update { state ->
                        state.copy(
                            projectName = project.name,
                            notes =
                                when {
                                    hasRestoredDraft -> restoredDraft.orEmpty()
                                    shouldAdoptNotes -> project.notes
                                    else -> state.notes
                                },
                            isLoaded = true,
                            isPro = canEditNotes,
                            isMissingProject = false,
                        )
                    }
                    if (hasRestoredDraft) {
                        scheduleSave(loadedProjectId, restoredDraft.orEmpty())
                    }
                }
            }
        }

        fun onNotesChanged(text: String) {
            val loadedProjectId = projectId ?: return
            val state = _uiState.value
            if (state.isMissingProject || !state.isLoaded || !state.isPro) return
            _uiState.update { it.copy(notes = text) }
            hasLocalEdits = text != persistedNotes
            saveJob?.cancel()
            if (!hasLocalEdits) {
                clearSavedDraft()
                return
            }
            saveDraft(text, persistedNotes)
            scheduleSave(loadedProjectId, text)
        }

        fun saveImmediately(onSaved: () -> Unit = {}) {
            val loadedProjectId = projectId
            val state = _uiState.value
            if (loadedProjectId == null || state.isMissingProject || !state.isLoaded || !state.isPro) {
                onSaved()
                return
            }
            saveJob?.cancel()
            if (!hasLocalEdits && state.notes == persistedNotes) {
                onSaved()
                return
            }
            viewModelScope.launch {
                try {
                    persistNotes(loadedProjectId, _uiState.value.notes)
                } finally {
                    onSaved()
                }
            }
        }

        private suspend fun persistNotes(
            loadedProjectId: Long,
            requestedNotes: String,
        ) {
            val savedProject =
                repository.saveProjectNotes(
                    id = loadedProjectId,
                    baseNotes = persistedNotes,
                    requestedNotes = requestedNotes,
                ) ?: run {
                    _uiState.update { it.copy(isMissingProject = true) }
                    return
                }
            persistedNotes = savedProject.notes
            val shouldApplySavedNotes = _uiState.value.notes == requestedNotes
            hasLocalEdits = !shouldApplySavedNotes && _uiState.value.notes != persistedNotes
            if (hasLocalEdits) {
                saveDraft(_uiState.value.notes, persistedNotes)
            } else {
                clearSavedDraft()
            }
            _uiState.update { state ->
                state.copy(
                    projectName = savedProject.name,
                    notes = if (shouldApplySavedNotes) savedProject.notes else state.notes,
                    isMissingProject = false,
                )
            }
        }

        private fun scheduleSave(
            loadedProjectId: Long,
            notes: String,
        ) {
            saveJob =
                viewModelScope.launch {
                    delay(DEBOUNCE_MS)
                    persistNotes(loadedProjectId, notes)
                }
        }

        private fun saveDraft(
            notes: String,
            baseNotes: String,
        ) {
            savedStateHandle[NOTES_DRAFT_KEY] = notes
            savedStateHandle[NOTES_DRAFT_BASE_KEY] = baseNotes
        }

        private fun clearSavedDraft() {
            savedStateHandle.remove<String>(NOTES_DRAFT_KEY)
            savedStateHandle.remove<String>(NOTES_DRAFT_BASE_KEY)
        }

        override fun onCleared() {
            val state = _uiState.value
            val loadedProjectId =
                projectId ?: run {
                    super.onCleared()
                    return
                }
            val shouldFlush =
                state.isLoaded &&
                    state.isPro &&
                    !state.isMissingProject &&
                    hasLocalEdits
            val notesToSave = state.notes
            val baseNotes = persistedNotes
            super.onCleared()
            if (!shouldFlush) return
            @Suppress("TooGenericExceptionCaught")
            applicationScope.launch {
                try {
                    repository.saveProjectNotes(
                        id = loadedProjectId,
                        baseNotes = baseNotes,
                        requestedNotes = notesToSave,
                    )
                } catch (_: Exception) {
                    // Viimeinen poistumistallennus ei saa kaataa sovellusta.
                }
            }
        }

        companion object {
            private const val DEBOUNCE_MS = 1000L
            private const val NOTES_DRAFT_KEY = "notesDraft"
            private const val NOTES_DRAFT_BASE_KEY = "notesDraftBase"
        }
    }
