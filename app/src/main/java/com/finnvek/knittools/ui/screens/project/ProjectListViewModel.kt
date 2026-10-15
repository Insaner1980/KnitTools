package com.finnvek.knittools.ui.screens.project

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.knittools.R
import com.finnvek.knittools.analytics.UsageAnalytics
import com.finnvek.knittools.analytics.UsageEvent
import com.finnvek.knittools.data.datastore.PreferencesManager
import com.finnvek.knittools.domain.model.ActiveWorkSession
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.CraftType
import com.finnvek.knittools.domain.model.MainCounterLabelType
import com.finnvek.knittools.domain.model.ProjectDocument
import com.finnvek.knittools.domain.model.ProjectFolderFilter
import com.finnvek.knittools.domain.model.ProjectFolderMoveDirection
import com.finnvek.knittools.domain.model.ProjectSortOrder
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.repository.ActiveSessionCompletionChoice
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.repository.ProgressPhotoRepository
import com.finnvek.knittools.repository.ProjectCompletionResult
import com.finnvek.knittools.repository.ProjectDeletionResult
import com.finnvek.knittools.repository.ProjectDocumentRepository
import com.finnvek.knittools.repository.ProjectFolderMutationResult
import com.finnvek.knittools.repository.ProjectFolderRepository
import com.finnvek.knittools.repository.SavedPatternRepository
import com.finnvek.knittools.repository.isSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ContinueKnittingProject(
    val projectId: Long,
    val name: String,
    val count: Int,
    val sectionName: String?,
    val targetRows: Int?,
    val craftType: CraftType,
    val mainCounterLabelType: MainCounterLabelType,
    val mainCounterCustomLabel: String?,
)

data class PendingProjectListSessionAction(
    val session: ActiveWorkSession,
    val projectIds: Set<Long>,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
@Suppress("TooManyFunctions") // Näkymämalli pitää listan käyttäjätoiminnot eksplisiittisinä.
class ProjectListViewModel
    @Inject
    constructor(
        private val repository: CounterRepository,
        private val proManager: ProManager,
        private val photoRepository: ProgressPhotoRepository,
        private val savedPatternRepository: SavedPatternRepository,
        private val projectDocumentRepository: ProjectDocumentRepository,
        private val preferencesManager: PreferencesManager,
        @param:ApplicationContext private val context: Context,
        private val folderRepository: ProjectFolderRepository,
        private val savedStateHandle: SavedStateHandle,
        private val analytics: UsageAnalytics = UsageAnalytics.NONE,
    ) : ViewModel() {
        private val _selectedFolderFilter =
            MutableStateFlow(restoreFolderFilter(savedStateHandle["project_folder_filter"]))
        val selectedFolderFilter: StateFlow<ProjectFolderFilter> = _selectedFolderFilter.asStateFlow()

        private val _folderState = MutableStateFlow(ProjectFoldersState())
        val folderState: StateFlow<ProjectFoldersState> = _folderState.asStateFlow()
        private var folderObservation: Job? = null

        private val folderEventChannel = Channel<ProjectFolderMutationResult>(Channel.BUFFERED)
        val folderEvents = folderEventChannel.receiveAsFlow()
        private val sessionErrorChannel = Channel<Int>(Channel.BUFFERED)
        val sessionErrors = sessionErrorChannel.receiveAsFlow()

        // === Preferences ===

        val showCompleted: StateFlow<Boolean> =
            preferencesManager.preferences
                .map { it.showCompletedProjects }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

        val sortOrder: StateFlow<ProjectSortOrder> =
            preferencesManager.preferences
                .map { it.projectSortOrder }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProjectSortOrder.DEFAULT)

        // === Lajittelutietoiset projektilistaukset ===

        val activeProjects: StateFlow<List<CounterProject>> =
            combine(
                sortOrder.flatMapLatest(repository::getActiveProjects),
                folderState,
                selectedFolderFilter,
            ) { projects, folders, filter ->
                filterProjects(projects, folders, filter)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        val completedProjects: StateFlow<List<CounterProject>> =
            combine(
                showCompleted.flatMapLatest { shouldShow ->
                    if (shouldShow) sortOrder.flatMapLatest(repository::getCompletedProjects) else flowOf(emptyList())
                },
                folderState,
                selectedFolderFilter,
            ) { projects, folders, filter ->
                filterProjects(projects, folders, filter)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        val hasHiddenCompletedProjects: StateFlow<Boolean> =
            combine(folderState, selectedFolderFilter, showCompleted) { folders, filter, show ->
                !show &&
                    folders.snapshot
                        ?.memberships
                        .orEmpty()
                        .any { it.isCompleted && filter.includes(it.folderId) }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

        val activeSession: StateFlow<ActiveWorkSession?> =
            repository
                .observeActiveSession()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

        val isPro: Boolean get() = proManager.hasFeature(ProFeature.UNLIMITED_PROJECTS)

        val projectLatestPhotoUris: StateFlow<Map<Long, String>> =
            photoRepository
                .observeLatestPhotoUris()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

        // === Multi-select ===

        private val _isMultiSelectMode = MutableStateFlow(false)
        val isMultiSelectMode: StateFlow<Boolean> = _isMultiSelectMode.asStateFlow()

        private val _selectedProjectIds = MutableStateFlow<Set<Long>>(emptySet())
        val selectedProjectIds: StateFlow<Set<Long>> = _selectedProjectIds.asStateFlow()

        private val _pendingCompletionSessionAction = MutableStateFlow<PendingProjectListSessionAction?>(null)
        val pendingCompletionSessionAction: StateFlow<PendingProjectListSessionAction?> =
            _pendingCompletionSessionAction.asStateFlow()

        private val _pendingDeletionSessionAction = MutableStateFlow<PendingProjectListSessionAction?>(null)
        val pendingDeletionSessionAction: StateFlow<PendingProjectListSessionAction?> =
            _pendingDeletionSessionAction.asStateFlow()

        // === Jatka neulomista ===

        private val _continueKnittingProject = MutableStateFlow<ContinueKnittingProject?>(null)
        val continueKnittingProject: StateFlow<ContinueKnittingProject?> = _continueKnittingProject.asStateFlow()

        private val _projectPatternNames = MutableStateFlow<Map<Long, String>>(emptyMap())
        val projectPatternNames: StateFlow<Map<Long, String>> = _projectPatternNames.asStateFlow()

        private val navigateToProjectChannel = Channel<Long>(Channel.BUFFERED)
        val navigateToProject = navigateToProjectChannel.receiveAsFlow()

        private val creationActions =
            ProjectCreationActions(
                repository,
                proManager,
                savedStateHandle,
                viewModelScope,
                onCreated = { navigateToProjectChannel.send(it) },
                onFailure = { sessionErrorChannel.trySend(R.string.generic_error_unknown) },
                analytics = analytics,
            )
        val projectCreationError = creationActions.projectCreationError
        val projectCreationPromptCount = creationActions.projectCreationPromptCount
        val projectCreationPrompts = creationActions.projectCreationPrompts
        val showCreateProjectDialog = creationActions.showCreateProjectDialog

        init {
            retryFolderLoading()
            viewModelScope.launch {
                try {
                    repository.refreshActiveSession()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    sessionErrorChannel.send(R.string.generic_error_unknown)
                }
            }
            viewModelScope.launch {
                combine(activeProjects, completedProjects) { active, completed ->
                    (active + completed).map { it.id }.toSet()
                }.collect { visibleIds ->
                    _selectedProjectIds.update { it.intersect(visibleIds) }
                    if (isMultiSelectMode.value) {
                        _pendingCompletionSessionAction.update {
                            it?.copy(
                                projectIds = it.projectIds.intersect(visibleIds),
                            )
                        }
                        _pendingDeletionSessionAction.update {
                            it?.copy(
                                projectIds = it.projectIds.intersect(visibleIds),
                            )
                        }
                    }
                }
            }
            viewModelScope.launch {
                activeProjects
                    .flatMapLatest { projects ->
                        projectDocumentRepository
                            .observeDocuments(projects.map(CounterProject::id))
                            .map { documents -> projects to documents }
                    }.collect { (projects, documentsByProject) ->
                        updateContinueKnitting(projects)
                        updatePatternNames(projects, documentsByProject)
                    }
            }
        }

        // === Preferences-toiminnot ===

        fun toggleShowCompleted() {
            if (showCompleted.value) {
                val completedIds =
                    folderState.value.snapshot
                        ?.memberships
                        .orEmpty()
                        .filter {
                            it.isCompleted
                        }.map { it.projectId }
                        .toSet()
                _selectedProjectIds.update { it - completedIds }
            }
            viewModelScope.launch {
                preferencesManager.toggleShowCompletedProjects()
            }
        }

        fun setSortOrder(order: ProjectSortOrder) {
            viewModelScope.launch {
                preferencesManager.setProjectSortOrder(order)
            }
        }

        // === Multi-select-toiminnot ===

        fun enterMultiSelectMode(initialProjectId: Long? = null) {
            _isMultiSelectMode.value = true
            _selectedProjectIds.value = if (initialProjectId != null) setOf(initialProjectId) else emptySet()
        }

        fun exitMultiSelectMode() {
            _isMultiSelectMode.value = false
            _selectedProjectIds.value = emptySet()
            _pendingCompletionSessionAction.value = null
            _pendingDeletionSessionAction.value = null
        }

        fun toggleProjectSelection(id: Long) {
            _selectedProjectIds.update { current ->
                if (id in current) current - id else current + id
            }
        }

        fun selectAllProjects() {
            _selectedProjectIds.value = (activeProjects.value + completedProjects.value).map { it.id }.toSet()
        }

        fun completeSelectedProjects() {
            launchSelectedProjectAction { selectedIds, active ->
                val activeIds = activeProjects.value.map { it.id }.toSet()
                val ids = selectedIds.intersect(_selectedProjectIds.value).intersect(activeIds)
                if (active != null && active.projectId in ids) {
                    _pendingCompletionSessionAction.value = PendingProjectListSessionAction(active, ids)
                    return@launchSelectedProjectAction
                }
                if (ids.isNotEmpty() && completeProjects(ids, choice = null, selectedOnly = true)) {
                    exitMultiSelectMode()
                }
            }
        }

        fun deleteSelectedProjects() {
            launchSelectedProjectAction { selectedIds, active ->
                val visibleIds = (activeProjects.value + completedProjects.value).map { it.id }.toSet()
                val ids = selectedIds.intersect(_selectedProjectIds.value).intersect(visibleIds)
                if (active != null && active.projectId in ids) {
                    _pendingDeletionSessionAction.value = PendingProjectListSessionAction(active, ids)
                    return@launchSelectedProjectAction
                }
                if (deleteProjects(ids, expectedSession = null, selectedOnly = true)) {
                    exitMultiSelectMode()
                }
            }
        }

        private fun launchSelectedProjectAction(action: suspend (Set<Long>, ActiveWorkSession?) -> Unit) {
            viewModelScope.launch {
                val selectedIds = _selectedProjectIds.value
                val active =
                    try {
                        repository.refreshActiveSession()
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        sessionErrorChannel.send(R.string.generic_error_unknown)
                        return@launch
                    }
                action(selectedIds, active)
            }
        }

        // === Projektitoiminnot ===

        private fun updateContinueKnitting(projects: List<CounterProject>) {
            val candidate = projects.firstOrNull { it.count > 0 }
            _continueKnittingProject.value =
                if (candidate != null) {
                    ContinueKnittingProject(
                        projectId = candidate.id,
                        name = candidate.name,
                        count = candidate.count,
                        sectionName = candidate.sectionName,
                        targetRows = candidate.targetRows,
                        craftType = candidate.craftType,
                        mainCounterLabelType = candidate.mainCounterLabelType,
                        mainCounterCustomLabel = candidate.mainCounterCustomLabel,
                    )
                } else {
                    null
                }
        }

        private suspend fun updatePatternNames(
            projects: List<CounterProject>,
            documentsByProject: Map<Long, List<ProjectDocument>>,
        ) {
            val nameMap = mutableMapOf<Long, String>()
            val linkedPatternIds =
                projects
                    .filter { project ->
                        documentsByProject[project.id].isNullOrEmpty() &&
                            project.patternName.isNullOrBlank()
                    }.mapNotNull { it.linkedPatternId }
                    .distinct()
            val patternsById =
                if (linkedPatternIds.isEmpty()) {
                    emptyMap()
                } else {
                    savedPatternRepository.getByIds(linkedPatternIds).associateBy { it.id }
                }
            projects.forEach { p ->
                documentsByProject[p.id]
                    ?.firstOrNull { it.isPrimary }
                    ?.let { primary ->
                        nameMap[p.id] = primary.label
                        return@forEach
                    }
                p.patternName?.takeIf { it.isNotBlank() }?.let {
                    nameMap[p.id] = it
                    return@forEach
                }
                val patternId = p.linkedPatternId ?: return@forEach
                patternsById[patternId]?.let { nameMap[p.id] = it.name }
            }
            _projectPatternNames.value = nameMap
        }

        fun requestProjectCreation() = creationActions.requestProjectCreation()

        fun projectCreationDismissed() = analytics.track(UsageEvent.PROJECT_CREATION_CANCELLED)

        fun dismissPendingProjectCreation() = creationActions.dismissPendingProjectCreation()

        fun createProject() {
            viewModelScope.launch {
                val count = repository.getProjectCount()
                val request =
                    PendingProjectCreation(
                        name = context.getString(R.string.new_project_name_format, count + 1),
                        craftType = CraftType.KNITTING,
                        mainCounterLabelType = CraftType.KNITTING.defaultMainCounterLabelType(),
                        mainCounterCustomLabel = null,
                        targetFolderId = (selectedFolderFilter.value as? ProjectFolderFilter.Folder)?.folderId,
                    )
                creationActions.create(request)
            }
        }

        fun createProject(
            name: String,
            craftType: CraftType,
            mainCounterLabelType: MainCounterLabelType,
            mainCounterCustomLabel: String?,
            targetFolderId: Long? = (selectedFolderFilter.value as? ProjectFolderFilter.Folder)?.folderId,
        ) {
            val request =
                PendingProjectCreation(
                    name = name,
                    craftType = craftType,
                    mainCounterLabelType = mainCounterLabelType,
                    mainCounterCustomLabel = mainCounterCustomLabel,
                    targetFolderId = targetFolderId,
                )
            creationActions.create(request)
        }

        fun retryPendingProjectCreation() = creationActions.retryPendingProjectCreation()

        @Suppress("kotlin:S1871") // Molemmat aktiivisen session tarkistuspolut avaavat saman jatkotoiminnon.
        fun archiveProject(id: Long) {
            viewModelScope.launch {
                when (
                    val result =
                        repository.completeProjectWithSessionChoice(
                            projectId = id,
                            choice = null,
                        )
                ) {
                    is ProjectCompletionResult.NeedsActiveSessionChoice -> {
                        _pendingCompletionSessionAction.value =
                            PendingProjectListSessionAction(result.session, setOf(id))
                    }
                    is ProjectCompletionResult.NeedsRecoveryReview -> {
                        _pendingCompletionSessionAction.value =
                            PendingProjectListSessionAction(result.session, setOf(id))
                    }
                    ProjectCompletionResult.Completed,
                    ProjectCompletionResult.PersistenceFailure,
                    ProjectCompletionResult.ProjectUnavailable,
                    -> Unit
                    ProjectCompletionResult.HistoryLimitReached ->
                        sessionErrorChannel.trySend(R.string.work_session_history_full)
                }
            }
        }

        fun deleteProject(id: Long) {
            viewModelScope.launch {
                when (val result = repository.deleteProjectResolvingActiveSession(id, false)) {
                    is ProjectDeletionResult.NeedsActiveSessionDiscard ->
                        _pendingDeletionSessionAction.value =
                            PendingProjectListSessionAction(result.session, setOf(id))
                    ProjectDeletionResult.Deleted, ProjectDeletionResult.ProjectUnavailable -> Unit
                    ProjectDeletionResult.PersistenceFailure ->
                        sessionErrorChannel.trySend(R.string.generic_error_unknown)
                }
            }
        }

        fun resolvePendingCompletion(saveSession: Boolean) {
            val pending = _pendingCompletionSessionAction.value ?: return
            val selectedOnly = isMultiSelectMode.value
            viewModelScope.launch {
                if (saveSession && pending.session.needsRecoveryReview) {
                    _pendingCompletionSessionAction.value = null
                    exitMultiSelectMode()
                    navigateToProjectChannel.send(pending.session.projectId)
                    return@launch
                }
                val completed =
                    completeProjects(
                        projectIds = pending.projectIds,
                        expectedSession = pending.session,
                        selectedOnly = selectedOnly,
                        choice =
                            if (saveSession) {
                                ActiveSessionCompletionChoice.SAVE
                            } else {
                                ActiveSessionCompletionChoice.DISCARD
                            },
                    )
                if (completed) {
                    _pendingCompletionSessionAction.value = null
                    exitMultiSelectMode()
                }
            }
        }

        fun cancelPendingCompletion() {
            _pendingCompletionSessionAction.value = null
        }

        fun resolvePendingDeletion() {
            val pending = _pendingDeletionSessionAction.value ?: return
            val selectedOnly = isMultiSelectMode.value
            viewModelScope.launch {
                if (deleteProjects(pending.projectIds, pending.session, selectedOnly)) {
                    _pendingDeletionSessionAction.value = null
                    exitMultiSelectMode()
                }
            }
        }

        fun cancelPendingDeletion() {
            _pendingDeletionSessionAction.value = null
        }

        private suspend fun completeProjects(
            projectIds: Set<Long>,
            choice: ActiveSessionCompletionChoice?,
            expectedSession: ActiveWorkSession? = null,
            selectedOnly: Boolean = false,
        ): Boolean {
            projectIds.forEach { id ->
                val project = repository.getProject(id) ?: return false
                if (selectedOnly && id !in selectedProjectIds.value) return false
                if (!project.isCompleted) {
                    val result =
                        repository.completeProjectWithSessionChoice(
                            projectId = id,
                            choice = choice.takeIf { expectedSession?.projectId == id },
                            expectedSession = expectedSession,
                        )
                    if (selectedOnly &&
                        id !in selectedProjectIds.value &&
                        result != ProjectCompletionResult.Completed
                    ) {
                        return false
                    }
                    handleProjectCompletionResult(result, projectIds, id)
                    if (result != ProjectCompletionResult.Completed) return false
                }
                _selectedProjectIds.update { it - id }
                _pendingCompletionSessionAction.update { it?.copy(projectIds = it.projectIds - id) }
            }
            return true
        }

        private fun handleProjectCompletionResult(
            result: ProjectCompletionResult,
            projectIds: Set<Long>,
            id: Long,
        ) {
            val session =
                when (result) {
                    is ProjectCompletionResult.NeedsActiveSessionChoice -> result.session
                    is ProjectCompletionResult.NeedsRecoveryReview -> result.session
                    ProjectCompletionResult.HistoryLimitReached -> {
                        sessionErrorChannel.trySend(R.string.work_session_history_full)
                        return
                    }
                    ProjectCompletionResult.PersistenceFailure -> {
                        sessionErrorChannel.trySend(R.string.generic_error_unknown)
                        return
                    }
                    else -> return
                }
            _pendingCompletionSessionAction.value =
                PendingProjectListSessionAction(
                    session,
                    projectIds.intersect(selectedProjectIds.value) + id,
                )
        }

        private suspend fun deleteProjects(
            projectIds: Set<Long>,
            expectedSession: ActiveWorkSession?,
            selectedOnly: Boolean = false,
        ): Boolean {
            projectIds.forEach { id ->
                if (selectedOnly && id !in selectedProjectIds.value) return false
                val result =
                    repository.deleteProjectResolvingActiveSession(
                        id = id,
                        discardActiveSession = id == expectedSession?.projectId,
                        expectedSession = expectedSession,
                    )
                if (selectedOnly && id !in selectedProjectIds.value && result != ProjectDeletionResult.Deleted) {
                    return false
                }
                if (result is ProjectDeletionResult.NeedsActiveSessionDiscard) {
                    _pendingDeletionSessionAction.value =
                        PendingProjectListSessionAction(
                            result.session,
                            projectIds.intersect(selectedProjectIds.value) + id,
                        )
                }
                if (result != ProjectDeletionResult.Deleted) {
                    if (result == ProjectDeletionResult.PersistenceFailure) {
                        sessionErrorChannel.trySend(R.string.generic_error_unknown)
                    }
                    return false
                }
                _selectedProjectIds.update { it - id }
                _pendingDeletionSessionAction.update { it?.copy(projectIds = it.projectIds - id) }
            }
            return true
        }

        fun reactivateProject(id: Long) {
            viewModelScope.launch {
                val project = repository.getProject(id) ?: return@launch
                repository.reactivateProject(id, project.completedAt, isPro)
            }
        }

        fun selectFolder(filter: ProjectFolderFilter) {
            if (filter == selectedFolderFilter.value) return
            _selectedFolderFilter.value = filter
            savedStateHandle["project_folder_filter"] =
                when (filter) {
                    ProjectFolderFilter.AllProjects -> "all"
                    ProjectFolderFilter.Unfiled -> "unfiled"
                    is ProjectFolderFilter.Folder -> "folder:${filter.folderId}"
                }
            exitMultiSelectMode()
        }

        fun retryFolderLoading() {
            folderObservation?.cancel()
            folderObservation =
                viewModelScope.launch {
                    folderRepository
                        .observeOrganization {
                            _folderState.update { it.copy(readFailed = true) }
                        }.collect { snapshot ->
                            _folderState.update { it.copy(snapshot = snapshot, isLoading = false, readFailed = false) }
                            val filter = selectedFolderFilter.value
                            if (filter is ProjectFolderFilter.Folder &&
                                snapshot.folders.none { it.id == filter.folderId }
                            ) {
                                selectFolder(ProjectFolderFilter.AllProjects)
                            }
                        }
                }
        }

        fun clearFolderError() {
            _folderState.update { it.copy(mutationError = null) }
        }

        fun createFolder(name: String) = mutateFolder { folderRepository.createFolder(name) }

        fun renameFolder(
            id: Long,
            name: String,
        ) = mutateFolder { folderRepository.renameFolder(id, name) }

        fun moveFolder(
            id: Long,
            direction: ProjectFolderMoveDirection,
        ) = mutateFolder {
            folderRepository.moveFolder(id, direction)
        }

        fun deleteFolder(id: Long) = mutateFolder { folderRepository.deleteFolder(id) }

        fun moveSelectedProjects(folderId: Long?) {
            val ids = selectedProjectIds.value.toSet()
            mutateFolder {
                folderRepository.moveProjects(ids, folderId).also { result ->
                    if (result.isSuccess) exitMultiSelectMode()
                }
            }
        }

        private fun mutateFolder(operation: suspend () -> ProjectFolderMutationResult) {
            if (folderState.value.isMutating) return
            _folderState.update { it.copy(isMutating = true, mutationError = null) }
            viewModelScope.launch {
                try {
                    val result = operation()
                    if (result.isSuccess) {
                        folderEventChannel.send(result)
                    } else {
                        _folderState.update { it.copy(mutationError = result) }
                    }
                } finally {
                    _folderState.update { it.copy(isMutating = false) }
                }
            }
        }
    }

private fun restoreFolderFilter(value: String?): ProjectFolderFilter =
    when {
        value == "unfiled" -> ProjectFolderFilter.Unfiled
        value?.startsWith("folder:") == true ->
            value
                .removePrefix("folder:")
                .toLongOrNull()
                ?.takeIf { it > 0 }
                ?.let(ProjectFolderFilter::Folder)
                ?: ProjectFolderFilter.AllProjects
        else -> ProjectFolderFilter.AllProjects
    }

private fun filterProjects(
    projects: List<CounterProject>,
    folders: ProjectFoldersState,
    filter: ProjectFolderFilter,
): List<CounterProject> {
    if (filter == ProjectFolderFilter.AllProjects) return projects
    val snapshot = folders.snapshot ?: return emptyList()
    val assignments = snapshot.memberships.associate { it.projectId to it.folderId }
    return projects.filter { assignments.containsKey(it.id) && filter.includes(assignments[it.id]) }
}
