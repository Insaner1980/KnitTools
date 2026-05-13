package com.finnvek.knittools.ui.screens.notes

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.knittools.ai.journal.JournalEntryProcessor
import com.finnvek.knittools.ai.journal.JournalProcessResult
import com.finnvek.knittools.ai.speech.SimpleSpeechRecognizer
import com.finnvek.knittools.ai.speech.SpeechError
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class JournalMode { ModeSelect, Speak, Type, Processing }

data class JournalEntryUiState(
    val mode: JournalMode = JournalMode.ModeSelect,
    val typedText: String = "",
    val partialSpeech: String = "",
    val isListening: Boolean = false,
    val errorMessage: Int? = null,
    val speechAvailable: Boolean = true,
    val pendingEntry: JournalEvent.EntryReady? = null,
)

sealed class JournalEvent {
    /** Merkintä valmis lisättäväksi notesien perään. `aiUsed = false` tarkoittaa fallbackia. */
    data class EntryReady(
        val text: String,
        val aiUsed: Boolean,
        val reason: JournalProcessResult.Fallback.Reason? = null,
    ) : JournalEvent()
}

@HiltViewModel
class JournalEntryViewModel
    @Inject
    constructor(
        @ApplicationContext context: Context,
        private val processor: JournalEntryProcessor,
    ) : ViewModel() {
        private val speechRecognizer = SimpleSpeechRecognizer(context)
        private var processingJob: Job? = null

        private val _uiState = MutableStateFlow(JournalEntryUiState(speechAvailable = speechRecognizer.isAvailable()))
        val uiState: StateFlow<JournalEntryUiState> = _uiState.asStateFlow()

        init {
            speechRecognizer.isListening
                .onEach { listening -> _uiState.update { it.copy(isListening = listening) } }
                .launchIn(viewModelScope)

            speechRecognizer.partialText
                .onEach { text -> _uiState.update { it.copy(partialSpeech = text) } }
                .launchIn(viewModelScope)

            speechRecognizer.error
                .onEach { err -> _uiState.update { it.copy(errorMessage = mapSpeechError(err)) } }
                .launchIn(viewModelScope)
        }

        fun selectSpeakMode() {
            _uiState.update { it.copy(mode = JournalMode.Speak, errorMessage = null, partialSpeech = "") }
        }

        fun selectTypeMode() {
            _uiState.update { it.copy(mode = JournalMode.Type, errorMessage = null) }
        }

        fun backToModeSelect() {
            speechRecognizer.stop()
            _uiState.update {
                it.copy(
                    mode = JournalMode.ModeSelect,
                    partialSpeech = "",
                    typedText = "",
                    errorMessage = null,
                )
            }
        }

        fun onTypedTextChange(text: String) {
            _uiState.update { it.copy(typedText = text, errorMessage = null) }
        }

        fun startListening() {
            _uiState.update { it.copy(partialSpeech = "", errorMessage = null) }
            speechRecognizer.start()
        }

        fun stopListening() {
            speechRecognizer.stop()
        }

        /** Lähetä nykyinen teksti (speak tai type) AI-käsittelyyn. */
        fun submit() {
            val state = _uiState.value
            val raw =
                when (state.mode) {
                    JournalMode.Speak -> state.partialSpeech.trim()
                    JournalMode.Type -> state.typedText.trim()
                    else -> ""
                }
            if (raw.isEmpty()) {
                _uiState.update { it.copy(errorMessage = com.finnvek.knittools.R.string.journal_empty_error) }
                return
            }
            processingJob?.cancel()
            _uiState.update { it.copy(mode = JournalMode.Processing, errorMessage = null, pendingEntry = null) }
            processingJob =
                viewModelScope.launch {
                    val result = processor.process(raw)
                    if (!isActive) return@launch
                    val entry =
                        when (result) {
                            is JournalProcessResult.Success -> {
                                JournalEvent.EntryReady(result.cleaned, aiUsed = true)
                            }

                            is JournalProcessResult.Fallback -> {
                                JournalEvent.EntryReady(result.raw, aiUsed = false, reason = result.reason)
                            }
                        }
                    _uiState.update { it.copy(pendingEntry = entry) }
                }
        }

        fun consumePendingEntry() {
            speechRecognizer.stop()
            processingJob = null
            resetEntryState()
        }

        fun dismissEntry() {
            processingJob?.cancel()
            processingJob = null
            speechRecognizer.stop()
            resetEntryState()
        }

        private fun resetEntryState() {
            _uiState.update {
                JournalEntryUiState(speechAvailable = it.speechAvailable)
            }
        }

        override fun onCleared() {
            super.onCleared()
            speechRecognizer.destroy()
        }

        private fun mapSpeechError(error: SpeechError): Int =
            when (error) {
                SpeechError.Permission -> com.finnvek.knittools.R.string.journal_mic_permission_required
                SpeechError.Network -> com.finnvek.knittools.R.string.journal_speech_network_error
                SpeechError.NoMatch -> com.finnvek.knittools.R.string.journal_speech_no_match
                SpeechError.Unavailable -> com.finnvek.knittools.R.string.journal_speech_unavailable
                SpeechError.Generic -> com.finnvek.knittools.R.string.journal_speech_error
            }
    }
