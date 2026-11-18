package com.finnvek.knittools.ui.screens.backup

import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.knittools.analytics.UsageAnalytics
import com.finnvek.knittools.analytics.UsageDetail
import com.finnvek.knittools.analytics.UsageEvent
import com.finnvek.knittools.data.backup.BackupError
import com.finnvek.knittools.data.backup.BackupException
import com.finnvek.knittools.data.backup.BackupPreview
import com.finnvek.knittools.repository.BackupRepository
import com.finnvek.knittools.repository.OnboardingStateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
        private val analytics: UsageAnalytics = UsageAnalytics.NONE,
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
                analytics.track(
                    if (error ==
                        BackupError.WRITE
                    ) {
                        UsageEvent.BACKUP_EXPORT_FAILED
                    } else {
                        UsageEvent.BACKUP_RESTORE_FAILED
                    },
                    backupErrorDetail(error),
                )
                mutableState.value = BackupUiState(error = error)
            }
        }

        fun export(uri: Uri?) {
            if (cancelling || state.value.busy) return
            if (uri == null) {
                analytics.track(UsageEvent.BACKUP_EXPORT_CANCELLED)
                mutableState.value = BackupUiState()
                return
            }
            analytics.track(UsageEvent.BACKUP_EXPORT_STARTED)
            run(BackupPhase.EXPORTING) {
                repository.export(uri)
                BackupUiState(BackupPhase.EXPORTED)
            }
        }

        fun prepare(uri: Uri?) {
            if (cancelling || state.value.busy) return
            if (uri == null) {
                analytics.track(UsageEvent.BACKUP_RESTORE_CANCELLED)
                mutableState.value = BackupUiState()
                return
            }
            val selected = UUID.randomUUID()
            selectionId = selected
            analytics.track(UsageEvent.BACKUP_RESTORE_STARTED)
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
                BackupUiState(BackupPhase.RESTORED)
            }
        }

        private suspend fun recordOnboardingProgress(restored: Boolean) =
            withContext(NonCancellable) {
                try {
                    if (restored) {
                        onboardingStateRepository.markBasicTipsSeen()
                    } else {
                        onboardingStateRepository.markBackupCreated()
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
            if (phase != BackupPhase.SELECTING) {
                analytics.track(
                    if (phase == BackupPhase.EXPORTING) {
                        UsageEvent.BACKUP_EXPORT_CANCELLED
                    } else {
                        UsageEvent.BACKUP_RESTORE_CANCELLED
                    },
                )
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
                        when (result.phase) {
                            BackupPhase.EXPORTED -> {
                                analytics.track(UsageEvent.BACKUP_EXPORT_SUCCEEDED)
                                recordOnboardingProgress(restored = false)
                            }
                            BackupPhase.RESTORED -> {
                                analytics.track(UsageEvent.BACKUP_RESTORE_SUCCEEDED)
                                recordOnboardingProgress(restored = true)
                            }
                            else -> Unit
                        }
                    } catch (failure: CancellationException) {
                        throw failure
                    } catch (failure: BackupException) {
                        analytics.track(
                            if (phase ==
                                BackupPhase.EXPORTING
                            ) {
                                UsageEvent.BACKUP_EXPORT_FAILED
                            } else {
                                UsageEvent.BACKUP_RESTORE_FAILED
                            },
                            backupErrorDetail(failure.error),
                        )
                        currentCoroutineContext().ensureActive()
                        mutableState.value = BackupUiState(error = failure.error)
                    }
                }
        }
    }

private fun backupErrorDetail(error: BackupError): UsageDetail =
    when (error) {
        BackupError.READ -> UsageDetail.ERROR_READ
        BackupError.WRITE, BackupError.RESTORE -> UsageDetail.ERROR_WRITE
        BackupError.SPACE -> UsageDetail.ERROR_TOO_LARGE
        BackupError.UNSUPPORTED -> UsageDetail.ERROR_UNAVAILABLE
        BackupError.INVALID, BackupError.CORRUPT, BackupError.VALIDATION -> UsageDetail.ERROR_INVALID
    }
