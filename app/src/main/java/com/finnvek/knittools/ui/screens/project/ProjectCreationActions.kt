package com.finnvek.knittools.ui.screens.project

import androidx.lifecycle.SavedStateHandle
import com.finnvek.knittools.analytics.UsageAnalytics
import com.finnvek.knittools.analytics.UsageEvent
import com.finnvek.knittools.domain.model.CraftType
import com.finnvek.knittools.domain.model.MainCounterLabelType
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.repository.ProjectCreationResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.io.Serializable

internal data class PendingProjectCreation(
    val name: String,
    val craftType: CraftType,
    val mainCounterLabelType: MainCounterLabelType,
    val mainCounterCustomLabel: String?,
    val targetFolderId: Long?,
) : Serializable {
    private companion object {
        const val serialVersionUID = 1L
    }
}

internal class ProjectCreationActions(
    private val repository: CounterRepository,
    private val proManager: ProManager,
    private val savedStateHandle: SavedStateHandle,
    private val scope: CoroutineScope,
    private val analytics: UsageAnalytics = UsageAnalytics.NONE,
    private val onCreated: suspend (Long) -> Unit,
) {
    private val isPro: Boolean get() = proManager.hasFeature(ProFeature.UNLIMITED_PROJECTS)
    private val _projectCreationError = MutableStateFlow<ProjectCreationResult?>(null)
    val projectCreationError = _projectCreationError.asStateFlow()
    private val _projectCreationPromptCount = MutableStateFlow<Int?>(null)
    val projectCreationPromptCount = _projectCreationPromptCount.asStateFlow()
    val projectCreationPrompts = projectCreationPromptCount.filterNotNull()

    private val showCreateProjectDialogChannel = Channel<Unit>(Channel.BUFFERED)
    val showCreateProjectDialog = showCreateProjectDialogChannel.receiveAsFlow()

    private var pendingProjectCreation: PendingProjectCreation?
        get() = savedStateHandle[PENDING_PROJECT_CREATION_KEY]
        set(value) {
            savedStateHandle[PENDING_PROJECT_CREATION_KEY] = value
        }
    private var projectCreationInFlight = false
    private var pendingCreationOpening: Boolean
        get() = savedStateHandle["pending_creation_opening"] ?: false
        set(value) {
            savedStateHandle["pending_creation_opening"] = value
        }
    private var creationContinuationJob: Job? = null
    private var creationReconciliationRequested = false

    init {
        scope.launch { repository.getActiveProjects().collect { reconcilePendingCreation() } }
        scope.launch { proManager.proState.collect { reconcilePendingCreation() } }
    }

    fun requestProjectCreation() {
        analytics.track(UsageEvent.PROJECT_CREATION_STARTED)
        _projectCreationError.value = null
        pendingCreationOpening = true
        reconcilePendingCreation()
    }

    fun dismissPendingProjectCreation() {
        pendingCreationOpening = false
        pendingProjectCreation = null
        _projectCreationPromptCount.value = null
        creationReconciliationRequested = false
        creationContinuationJob?.cancel()
    }

    private fun reconcilePendingCreation() {
        if (creationContinuationJob?.isActive == true || projectCreationInFlight) {
            creationReconciliationRequested = true
            return
        }
        if (!pendingCreationOpening && pendingProjectCreation == null) return
        creationContinuationJob =
            scope.launch(start = CoroutineStart.LAZY) {
                try {
                    val count = repository.getActiveProjectCount()
                    if (!pendingCreationOpening && pendingProjectCreation == null) return@launch
                    if (!isPro && count >= 1) {
                        _projectCreationPromptCount.value = count
                    } else {
                        resumePendingProjectCreation(reconsiderLimit = false)
                    }
                } finally {
                    creationContinuationJob = null
                    val reconsider = creationReconciliationRequested
                    creationReconciliationRequested = false
                    if (reconsider) reconcilePendingCreation()
                }
            }
        creationContinuationJob?.start()
    }

    fun create(request: PendingProjectCreation) {
        pendingProjectCreation = request
        scope.launch { createProjectInternal(request) }
    }

    fun retryPendingProjectCreation() {
        resumePendingProjectCreation(reconsiderLimit = true)
    }

    private fun resumePendingProjectCreation(reconsiderLimit: Boolean) {
        if (pendingCreationOpening) {
            pendingCreationOpening = false
            _projectCreationPromptCount.value = null
            scope.launch { showCreateProjectDialogChannel.send(Unit) }
            return
        }
        val request = pendingProjectCreation ?: return
        scope.launch { createProjectInternal(request, reconsiderLimit) }
    }

    private suspend fun createProjectInternal(
        request: PendingProjectCreation,
        reconsiderLimit: Boolean = true,
    ) {
        if (projectCreationInFlight || pendingProjectCreation != request) return
        projectCreationInFlight = true
        analytics.track(UsageEvent.PROJECT_CREATION_SUBMITTED)
        var limited = false
        try {
            _projectCreationError.value = null
            val result =
                repository.createProject(
                    name = request.name,
                    craftType = request.craftType,
                    mainCounterLabelType = request.mainCounterLabelType,
                    mainCounterCustomLabel = request.mainCounterCustomLabel,
                    canCreateAdditionalProjects = isPro,
                    targetFolderId = request.targetFolderId,
                )
            if (pendingProjectCreation != request) return
            when (result) {
                is ProjectCreationResult.Created -> {
                    analytics.track(UsageEvent.PROJECT_CREATED)
                    pendingProjectCreation = null
                    _projectCreationPromptCount.value = null
                    onCreated(result.projectId)
                }
                ProjectCreationResult.LimitReached -> {
                    val activeCount = repository.getActiveProjectCount()
                    limited = true
                    if (pendingProjectCreation == request) _projectCreationPromptCount.value = activeCount
                }
                ProjectCreationResult.InvalidProject -> {
                    analytics.track(UsageEvent.PROJECT_CREATION_FAILED)
                    pendingProjectCreation = null
                }
                ProjectCreationResult.FolderMissing -> {
                    analytics.track(UsageEvent.PROJECT_CREATION_FAILED)
                    pendingProjectCreation = null
                    _projectCreationError.value = result
                }
            }
        } finally {
            projectCreationInFlight = false
            val reconsider = creationReconciliationRequested
            creationReconciliationRequested = false
            if (limited &&
                (reconsiderLimit || reconsider) &&
                pendingProjectCreation == request
            ) {
                reconcilePendingCreation()
            }
        }
    }

    private companion object {
        const val PENDING_PROJECT_CREATION_KEY = "pending_project_creation"
    }
}
