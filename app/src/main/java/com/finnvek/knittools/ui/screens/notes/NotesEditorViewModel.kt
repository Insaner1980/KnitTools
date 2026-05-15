package com.finnvek.knittools.ui.screens.notes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.knittools.ai.AiQuotaManager
import com.finnvek.knittools.di.IoDispatcher
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.ui.navigation.toPositiveRouteIdOrNull
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject

data class NotesEditorUiState(
    val projectName: String = "",
    val notes: String = "",
    val isLoaded: Boolean = false,
    val currentRow: Int = 0,
    val isPro: Boolean = false,
    val isAiAvailable: Boolean = false,
    val isMissingProject: Boolean = false,
)

@HiltViewModel
class NotesEditorViewModel
    @Inject
    constructor(
        private val repository: CounterRepository,
        private val proManager: ProManager,
        private val aiQuotaManager: AiQuotaManager,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
        savedStateHandle: SavedStateHandle,
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
                    val hasAiFeature = proManager.hasFeature(ProFeature.AI_FEATURES)
                    val hasQuota = hasAiFeature && aiQuotaManager.hasQuota()
                    val shouldAdoptNotes =
                        !_uiState.value.isLoaded ||
                            !hasLocalEdits ||
                            _uiState.value.notes == persistedNotes
                    if (shouldAdoptNotes) {
                        persistedNotes = project.notes
                        hasLocalEdits = false
                    }
                    _uiState.update { state ->
                        state.copy(
                            projectName = project.name,
                            notes = if (shouldAdoptNotes) project.notes else state.notes,
                            isLoaded = true,
                            currentRow = project.count,
                            isPro = canEditNotes,
                            isAiAvailable = hasAiFeature && hasQuota,
                            isMissingProject = false,
                        )
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
            if (!hasLocalEdits) return
            saveJob =
                viewModelScope.launch {
                    delay(DEBOUNCE_MS)
                    persistNotes(loadedProjectId, text)
                }
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

        /**
         * Liittää päiväkirjamerkinnän olemassa olevien muistiinpanojen perään.
         * Header: "{päivämäärä} · Row {currentRow}" (row-osuus vain jos count > 0).
         * Erotin "---" lisätään vain jos olemassa olevat muistiinpanot eivät ole tyhjät.
         */
        fun appendJournalEntry(cleanedText: String) {
            val state = _uiState.value
            if (state.isMissingProject || !state.isLoaded || !state.isPro) return
            val date = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).format(LocalDate.now())
            val rowPart = if (state.currentRow > 0) " · Row ${state.currentRow}" else ""
            val header = "$date$rowPart"
            val block = "$header\n\n$cleanedText"
            val newNotes =
                if (state.notes.isBlank()) {
                    block
                } else {
                    "${state.notes.trimEnd()}\n\n---\n\n$block"
                }
            onNotesChanged(newNotes)
            // Päivitä AI-käytettävyys (quota voi olla muuttunut kutsun jälkeen)
            viewModelScope.launch {
                val stillAvailable = proManager.hasFeature(ProFeature.AI_FEATURES) && aiQuotaManager.hasQuota()
                _uiState.update { it.copy(isAiAvailable = stillAvailable) }
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
            _uiState.update { state ->
                state.copy(
                    projectName = savedProject.name,
                    notes = if (shouldApplySavedNotes) savedProject.notes else state.notes,
                    currentRow = savedProject.count,
                    isMissingProject = false,
                )
            }
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
            CoroutineScope(ioDispatcher + NonCancellable).launch {
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
        }
    }
