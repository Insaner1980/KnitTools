package com.finnvek.knittools.ui.screens.notes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.knittools.ai.AiQuotaManager
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.repository.CounterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
)

@HiltViewModel
class NotesEditorViewModel
    @Inject
    constructor(
        private val repository: CounterRepository,
        private val proManager: ProManager,
        private val aiQuotaManager: AiQuotaManager,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val projectId: Long = checkNotNull(savedStateHandle["projectId"])

        private val _uiState = MutableStateFlow(NotesEditorUiState())
        val uiState: StateFlow<NotesEditorUiState> = _uiState.asStateFlow()

        private var saveJob: Job? = null

        init {
            viewModelScope.launch {
                val project = repository.getProject(projectId) ?: return@launch
                val isPro = proManager.hasFeature(ProFeature.AI_FEATURES)
                val hasQuota = isPro && aiQuotaManager.hasQuota()
                _uiState.value =
                    NotesEditorUiState(
                        projectName = project.name,
                        notes = project.notes,
                        isLoaded = true,
                        currentRow = project.count,
                        isPro = isPro,
                        isAiAvailable = isPro && hasQuota,
                    )
            }
        }

        fun onNotesChanged(text: String) {
            _uiState.update { it.copy(notes = text) }
            saveJob?.cancel()
            saveJob =
                viewModelScope.launch {
                    delay(DEBOUNCE_MS)
                    repository.updateProjectNotes(projectId, text)
                }
        }

        fun saveImmediately() {
            saveJob?.cancel()
            viewModelScope.launch {
                repository.updateProjectNotes(projectId, _uiState.value.notes)
            }
        }

        /**
         * Liittää päiväkirjamerkinnän olemassa olevien muistiinpanojen perään.
         * Header: "{päivämäärä} · Row {currentRow}" (row-osuus vain jos count > 0).
         * Erotin "---" lisätään vain jos olemassa olevat muistiinpanot eivät ole tyhjät.
         */
        fun appendJournalEntry(cleanedText: String) {
            val state = _uiState.value
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
                val stillAvailable = state.isPro && aiQuotaManager.hasQuota()
                _uiState.update { it.copy(isAiAvailable = stillAvailable) }
            }
        }

        companion object {
            private const val DEBOUNCE_MS = 1000L
        }
    }
