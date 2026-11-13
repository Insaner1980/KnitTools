package com.finnvek.knittools.ui.screens.backup

import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.knittools.data.backup.BackupError
import com.finnvek.knittools.data.backup.BackupException
import com.finnvek.knittools.data.backup.BackupPreview
import com.finnvek.knittools.repository.BackupRepository
import com.finnvek.knittools.repository.OnboardingStateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

enum class BackupPhase { IDLE, SELECTING, EXPORTING, VALIDATING, PREVIEW, RESTORING, EXPORTED, RESTORED }

@Immutable
data class BackupUiState(
    val phase: BackupPhase = BackupPhase.IDLE,
    val preview: BackupPreview? = null,
    val error: BackupError? = null,
    val previewSelectionId: UUID? = null,
) {
    val busy: Boolean get() = phase in setOf(BackupPhase.EXPORTING, BackupPhase.VALIDATING, BackupPhase.RESTORING)
}

@HiltViewModel
class BackupViewModel
    @Inject
    constructor(
        private val repository: BackupRepository,
        private val onboardingStateRepository: OnboardingStateRepository,
    ) : ViewModel() {
        private val mutableState = MutableStateFlow(BackupUiState())
        val state = mutableState.asStateFlow()
        private var job: Job? = null
        private var cancelling = false
        private var selectionId: UUID? = null

        override fun onCleared() {
            repository.releasePreview(selectionId)
        }

        fun select(): Boolean {
            if (cancelling ||
                state.value.busy ||
                state.value.phase == BackupPhase.SELECTING ||
                state.value.preview != null
            ) {
                return false
            }
            mutableState.value = BackupUiState(BackupPhase.SELECTING)
            return true
        }

        fun selectionFailed(error: BackupError) {
            if (state.value.phase == BackupPhase.SELECTING) {
                mutableState.value = BackupUiState(error = error)
            }
        }

        fun export(uri: Uri?) {
            if (cancelling || state.value.busy) return
            if (uri == null) {
                mutableState.value = BackupUiState()
                return
            }
            run(BackupPhase.EXPORTING) {
                repository.export(uri)
                recordOnboardingProgress(restored = false)
                BackupUiState(BackupPhase.EXPORTED)
            }
        }

        fun prepare(uri: Uri?) {
            if (cancelling || state.value.busy) return
            if (uri == null) {
                mutableState.value = BackupUiState()
                return
            }
            val selected = UUID.randomUUID()
            selectionId = selected
            run(BackupPhase.VALIDATING) {
                BackupUiState(BackupPhase.PREVIEW, repository.prepare(uri, selected), previewSelectionId = selected)
            }
        }

        fun restore(confirmedSelectionId: UUID) {
            if (state.value.phase != BackupPhase.PREVIEW ||
                state.value.previewSelectionId != confirmedSelectionId
            ) {
                return
            }
            run(BackupPhase.RESTORING) {
                repository.restore(confirmedSelectionId)
                recordOnboardingProgress(restored = true)
                BackupUiState(BackupPhase.RESTORED)
            }
        }

        private suspend fun recordOnboardingProgress(restored: Boolean) {
            currentCoroutineContext().ensureActive()
            try {
                if (restored) {
                    onboardingStateRepository.markBasicTipsSeen()
                } else {
                    onboardingStateRepository
                        .markBackupCreated()
                }
            } catch (failure: CancellationException) {
                throw failure
            } catch (_: Exception) {
                // Opastuksen tallennusvirhe ei muuta jo valmistunutta varmuuskopiota epäonnistuneeksi.
            }
        }

        fun cancel() {
            val phase = state.value.phase
            if (cancelling ||
                phase !in
                setOf(
                    BackupPhase.SELECTING,
                    BackupPhase.EXPORTING,
                    BackupPhase.VALIDATING,
                    BackupPhase.PREVIEW,
                )
            ) {
                return
            }
            cancelling = true
            val cancelledSelection = selectionId
            job?.cancel()
            job =
                viewModelScope.launch {
                    try {
                        repository.cancelPreview(cancelledSelection)
                        mutableState.value =
                            BackupUiState(error = if (phase == BackupPhase.EXPORTING) BackupError.WRITE else null)
                    } finally {
                        cancelling = false
                    }
                }
        }

        private fun run(
            phase: BackupPhase,
            block: suspend () -> BackupUiState,
        ) {
            if (cancelling || state.value.busy) return
            mutableState.value = BackupUiState(phase)
            job =
                viewModelScope.launch {
                    try {
                        val result = block()
                        currentCoroutineContext().ensureActive()
                        mutableState.value = result
                    } catch (failure: CancellationException) {
                        throw failure
                    } catch (failure: BackupException) {
                        currentCoroutineContext().ensureActive()
                        mutableState.value = BackupUiState(error = failure.error)
                    }
                }
        }
    }
