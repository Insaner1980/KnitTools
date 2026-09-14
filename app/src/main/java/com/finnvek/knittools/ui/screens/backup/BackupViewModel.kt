package com.finnvek.knittools.ui.screens.backup

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.knittools.data.backup.BackupError
import com.finnvek.knittools.data.backup.BackupException
import com.finnvek.knittools.data.backup.BackupPreview
import com.finnvek.knittools.repository.BackupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

enum class BackupPhase { IDLE, SELECTING, EXPORTING, VALIDATING, PREVIEW, RESTORING, EXPORTED, RESTORED }

data class BackupUiState(
    val phase: BackupPhase = BackupPhase.IDLE,
    val preview: BackupPreview? = null,
    val error: BackupError? = null,
) {
    val busy: Boolean get() = phase in setOf(BackupPhase.EXPORTING, BackupPhase.VALIDATING, BackupPhase.RESTORING)
}

@HiltViewModel
class BackupViewModel
    @Inject
    constructor(
        private val repository: BackupRepository,
    ) : ViewModel() {
        private val mutableState = MutableStateFlow(BackupUiState())
        val state = mutableState.asStateFlow()
        private var job: Job? = null
        private var selectionId: UUID? = null

        override fun onCleared() {
            repository.releasePreview(selectionId)
        }

        fun select(): Boolean {
            if (state.value.busy ||
                state.value.phase == BackupPhase.SELECTING ||
                state.value.preview != null
            ) {
                return false
            }
            mutableState.value = BackupUiState(BackupPhase.SELECTING)
            return true
        }

        fun export(uri: Uri?) {
            if (uri == null) {
                mutableState.value = BackupUiState()
                return
            }
            run(BackupPhase.EXPORTING) {
                repository.export(uri)
                BackupUiState(BackupPhase.EXPORTED)
            }
        }

        fun prepare(uri: Uri?) {
            if (state.value.busy) return
            if (uri == null) {
                mutableState.value = BackupUiState()
                return
            }
            val selected = UUID.randomUUID()
            selectionId = selected
            run(BackupPhase.VALIDATING) { BackupUiState(BackupPhase.PREVIEW, repository.prepare(uri, selected)) }
        }

        fun restore() {
            if (state.value.phase != BackupPhase.PREVIEW) return
            val selected = selectionId ?: return
            run(BackupPhase.RESTORING) {
                repository.restore(selected)
                BackupUiState(BackupPhase.RESTORED)
            }
        }

        fun cancel() {
            if (state.value.phase == BackupPhase.RESTORING) return
            val cancelledSelection = selectionId
            job?.cancel()
            job =
                viewModelScope.launch {
                    repository.cancelPreview(cancelledSelection)
                    mutableState.value = BackupUiState()
                }
        }

        private fun run(
            phase: BackupPhase,
            block: suspend () -> BackupUiState,
        ) {
            if (state.value.busy) return
            mutableState.value = BackupUiState(phase)
            job =
                viewModelScope.launch {
                    try {
                        mutableState.value = block()
                    } catch (failure: CancellationException) {
                        throw failure
                    } catch (failure: BackupException) {
                        mutableState.value = BackupUiState(error = failure.error)
                    }
                }
        }
    }
