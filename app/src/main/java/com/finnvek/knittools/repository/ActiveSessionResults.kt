package com.finnvek.knittools.repository

import com.finnvek.knittools.domain.model.ActiveWorkSession

sealed interface StartSessionResult {
    data class Started(
        val session: ActiveWorkSession,
    ) : StartSessionResult

    data class AlreadyActive(
        val session: ActiveWorkSession,
    ) : StartSessionResult

    data class ProjectConflict(
        val activeSession: ActiveWorkSession,
        val requestedProjectId: Long,
    ) : StartSessionResult

    data object ProjectMissing : StartSessionResult

    data object ProjectCompleted : StartSessionResult

    data object PersistenceFailure : StartSessionResult
}

sealed interface StopSessionResult {
    data class Saved(
        val completedSessionId: Long?,
    ) : StopSessionResult

    data object Discarded : StopSessionResult

    data class NeedsRecoveryReview(
        val session: ActiveWorkSession,
    ) : StopSessionResult

    data object StaleAction : StopSessionResult

    data object NoActiveSession : StopSessionResult

    data object PersistenceFailure : StopSessionResult
}

sealed interface RecoveryResolutionResult {
    data class Continued(
        val session: ActiveWorkSession,
    ) : RecoveryResolutionResult

    data class DiscardedAndStopped(
        val completedSessionId: Long?,
    ) : RecoveryResolutionResult

    data class EditedAndStopped(
        val completedSessionId: Long?,
    ) : RecoveryResolutionResult

    data object InvalidDuration : RecoveryResolutionResult

    data object StaleAction : RecoveryResolutionResult

    data object PersistenceFailure : RecoveryResolutionResult
}

enum class ActiveSessionCompletionChoice {
    SAVE,
    DISCARD,
}

sealed interface ProjectCompletionResult {
    data object Completed : ProjectCompletionResult

    data class NeedsActiveSessionChoice(
        val session: ActiveWorkSession,
    ) : ProjectCompletionResult

    data class NeedsRecoveryReview(
        val session: ActiveWorkSession,
    ) : ProjectCompletionResult

    data object ProjectUnavailable : ProjectCompletionResult

    data object PersistenceFailure : ProjectCompletionResult
}

sealed interface ProjectDeletionResult {
    data object Deleted : ProjectDeletionResult

    data class NeedsActiveSessionDiscard(
        val session: ActiveWorkSession,
    ) : ProjectDeletionResult

    data object ProjectUnavailable : ProjectDeletionResult

    data object PersistenceFailure : ProjectDeletionResult
}
