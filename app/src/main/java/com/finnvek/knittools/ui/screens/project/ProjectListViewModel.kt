package com.finnvek.knittools.ui.screens.project

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.knittools.R
import com.finnvek.knittools.data.datastore.PreferencesManager
import com.finnvek.knittools.domain.model.ActiveWorkSession
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.CraftType
import com.finnvek.knittools.domain.model.MainCounterLabelType
import com.finnvek.knittools.domain.model.ProjectDocument
import com.finnvek.knittools.domain.model.ProjectFolderFilter
import com.finnvek.knittools.domain.model.ProjectFolderMoveDirection
import com.finnvek.knittools.domain.model.ProjectSortOrder
import com.finnvek.knittools.domain.model.displayName
import com.finnvek.knittools.domain.model.parseYarnCardIds
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.repository.ActiveSessionCompletionChoice
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.repository.ProgressPhotoRepository
import com.finnvek.knittools.repository.ProjectCompletionResult
import com.finnvek.knittools.repository.ProjectCreationResult
import com.finnvek.knittools.repository.ProjectDeletionResult
import com.finnvek.knittools.repository.ProjectDocumentRepository
import com.finnvek.knittools.repository.ProjectFolderMutationResult
import com.finnvek.knittools.repository.ProjectFolderRepository
import com.finnvek.knittools.repository.SavedPatternRepository
import com.finnvek.knittools.repository.YarnCardRepository
import com.finnvek.knittools.repository.isSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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

private data class PendingProjectCreation(
    val name: String,
    val craftType: CraftType,
    val mainCounterLabelType: MainCounterLabelType,
    val mainCounterCustomLabel: String?,
    val targetFolderId: Long?,
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
        private val yarnCardRepository: YarnCardRepository,
        private val photoRepository: ProgressPhotoRepository,
        private val savedPatternRepository: SavedPatternRepository,
        private val projectDocumentRepository: ProjectDocumentRepository,
        private val preferencesManager: PreferencesManager,
        @param:ApplicationContext private val context: Context,
        private val folderRepository: ProjectFolderRepository,
        private val savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val _selectedFolderFilter =
            MutableStateFlow(restoreFolderFilter(savedStateHandle["project_folder_filter"]))
        val selectedFolderFilter: StateFlow<ProjectFolderFilter> = _selectedFolderFilter.asStateFlow()

        private val _folderState = MutableStateFlow(ProjectFoldersState())
        val folderState: StateFlow<ProjectFoldersState> = _folderState.asStateFlow()
        private var folderObservation: Job? = null

        private val folderEventChannel = Channel<ProjectFolderMutationResult>(Channel.BUFFERED)
        val folderEvents = folderEventChannel.receiveAsFlow()

        private val _projectCreationError = MutableStateFlow<ProjectCreationResult?>(null)
        val projectCreationError: StateFlow<ProjectCreationResult?> = _projectCreationError.asStateFlow()

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

        private val _projectYarnNames = MutableStateFlow<Map<Long, String>>(emptyMap())
        val projectYarnNames: StateFlow<Map<Long, String>> = _projectYarnNames.asStateFlow()

        private val _projectYarnCardIds = MutableStateFlow<Map<Long, Long>>(emptyMap())
        val projectYarnCardIds: StateFlow<Map<Long, Long>> = _projectYarnCardIds.asStateFlow()

        private val _projectPhotoCounts = MutableStateFlow<Map<Long, Int>>(emptyMap())
        val projectPhotoCounts: StateFlow<Map<Long, Int>> = _projectPhotoCounts.asStateFlow()

        private val _projectPatternNames = MutableStateFlow<Map<Long, String>>(emptyMap())
        val projectPatternNames: StateFlow<Map<Long, String>> = _projectPatternNames.asStateFlow()

        private val _projectIdsWithDocuments = MutableStateFlow<Set<Long>>(emptySet())
        val projectIdsWithDocuments: StateFlow<Set<Long>> = _projectIdsWithDocuments.asStateFlow()

        private val _projectIdsWithAvailablePrimary = MutableStateFlow<Set<Long>>(emptySet())
        val projectIdsWithAvailablePrimary: StateFlow<Set<Long>> = _projectIdsWithAvailablePrimary.asStateFlow()

        private val _projectHasNotes = MutableStateFlow<Set<Long>>(emptySet())
        val projectHasNotes: StateFlow<Set<Long>> = _projectHasNotes.asStateFlow()

        private val navigateToProjectChannel = Channel<Long>(Channel.BUFFERED)
        val navigateToProject = navigateToProjectChannel.receiveAsFlow()

        private val navigateToNotesEditorChannel = Channel<Long>(Channel.BUFFERED)
        val navigateToNotesEditor = navigateToNotesEditorChannel.receiveAsFlow()

        private val navigateToPhotoGalleryChannel = Channel<Long>(Channel.BUFFERED)
        val navigateToPhotoGallery = navigateToPhotoGalleryChannel.receiveAsFlow()

        private val projectCreationPromptChannel = Channel<Int>(Channel.BUFFERED)
        val projectCreationPrompts = projectCreationPromptChannel.receiveAsFlow()

        private val showCreateProjectDialogChannel = Channel<Unit>(Channel.BUFFERED)
        val showCreateProjectDialog = showCreateProjectDialogChannel.receiveAsFlow()

        private var pendingProjectCreation: PendingProjectCreation? = null

        init {
            retryFolderLoading()
            viewModelScope.launch { repository.refreshActiveSession() }
            viewModelScope.launch {
                activeProjects
                    .flatMapLatest { projects ->
                        projectDocumentRepository
                            .observeDocuments(projects.map(CounterProject::id))
                            .map { documents -> projects to documents }
                    }.collect { (projects, documentsByProject) ->
                        updateContinueKnitting(projects)
                        updateYarnNames(projects)
                        updatePhotoCounts(projects)
                        updatePatternNames(projects, documentsByProject)
                        updateHasNotes(projects)
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
            viewModelScope.launch {
                val activeIds = activeProjects.value.map { it.id }.toSet()
                val ids = _selectedProjectIds.value.intersect(activeIds)
                val active = repository.refreshActiveSession()
                if (active != null && active.projectId in ids) {
                    _pendingCompletionSessionAction.value = PendingProjectListSessionAction(active, ids)
                    return@launch
                }
                completeProjects(ids, choice = null)
                exitMultiSelectMode()
            }
        }

        fun deleteSelectedProjects() {
            viewModelScope.launch {
                val ids = _selectedProjectIds.value
                val active = repository.refreshActiveSession()
                if (active != null && active.projectId in ids) {
                    _pendingDeletionSessionAction.value = PendingProjectListSessionAction(active, ids)
                    return@launch
                }
                ids.forEach { id -> repository.deleteProjectResolvingActiveSession(id, false) }
                exitMultiSelectMode()
            }
        }

        // === Projektitoiminnot ===

        private suspend fun updateContinueKnitting(projects: List<CounterProject>) {
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

        private suspend fun updateYarnNames(projects: List<CounterProject>) {
            val yarnNameMap = mutableMapOf<Long, String>()
            val yarnCardIdMap = mutableMapOf<Long, Long>()
            val allYarnIds =
                projects
                    .flatMap { p ->
                        parseYarnCardIds(p.yarnCardIds)
                    }.distinct()
            if (allYarnIds.isNotEmpty()) {
                val cards = yarnCardRepository.getCards(allYarnIds).associateBy { it.id }
                projects.forEach { p ->
                    val ids = parseYarnCardIds(p.yarnCardIds)
                    val firstCard = ids.firstNotNullOfOrNull { cards[it] }
                    if (firstCard != null) {
                        yarnNameMap[p.id] = firstCard.displayName(::fallbackYarnCardName)
                        yarnCardIdMap[p.id] = firstCard.id
                    }
                }
            }
            _projectYarnNames.value = yarnNameMap
            _projectYarnCardIds.value = yarnCardIdMap
        }

        private fun fallbackYarnCardName(id: Long): String = context.getString(R.string.yarn_card_number_fallback, id)

        private suspend fun updatePhotoCounts(projects: List<CounterProject>) {
            _projectPhotoCounts.value =
                photoRepository
                    .getPhotoCountsByProjectIds(projects.map { it.id })
                    .filterValues { it > 0 }
        }

        private suspend fun updatePatternNames(
            projects: List<CounterProject>,
            documentsByProject: Map<Long, List<ProjectDocument>>,
        ) {
            val nameMap = mutableMapOf<Long, String>()
            _projectIdsWithDocuments.value = documentsByProject.filterValues { it.isNotEmpty() }.keys
            _projectIdsWithAvailablePrimary.value =
                documentsByProject
                    .mapNotNull { (projectId, documents) ->
                        val primary = documents.firstOrNull(ProjectDocument::isPrimary) ?: return@mapNotNull null
                        projectId.takeIf { projectDocumentRepository.isAvailable(primary) }
                    }.toSet()
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

        private fun updateHasNotes(projects: List<CounterProject>) {
            _projectHasNotes.value = projects.filter { it.notesCreated }.map { it.id }.toSet()
        }

        fun requestProjectCreation() {
            _projectCreationError.value = null
            viewModelScope.launch {
                val count = repository.getProjectCount()
                if (!isPro && count >= 1) {
                    projectCreationPromptChannel.send(count)
                } else {
                    showCreateProjectDialogChannel.send(Unit)
                }
            }
        }

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
                pendingProjectCreation = request
                createProjectInternal(request)
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
            pendingProjectCreation = request
            viewModelScope.launch {
                createProjectInternal(request)
            }
        }

        fun retryPendingProjectCreation() {
            val request = pendingProjectCreation ?: return
            viewModelScope.launch { createProjectInternal(request) }
        }

        private suspend fun createProjectInternal(request: PendingProjectCreation) {
            _projectCreationError.value = null
            when (
                val result =
                    repository.createProject(
                        name = request.name,
                        craftType = request.craftType,
                        mainCounterLabelType = request.mainCounterLabelType,
                        mainCounterCustomLabel = request.mainCounterCustomLabel,
                        canCreateAdditionalProjects = isPro,
                        targetFolderId = request.targetFolderId,
                    )
            ) {
                is ProjectCreationResult.Created -> {
                    pendingProjectCreation = null
                    navigateToProjectChannel.send(result.projectId)
                }
                ProjectCreationResult.LimitReached -> {
                    projectCreationPromptChannel.send(repository.getProjectCount())
                }
                ProjectCreationResult.InvalidProject -> pendingProjectCreation = null
                ProjectCreationResult.FolderMissing -> {
                    pendingProjectCreation = null
                    _projectCreationError.value = result
                }
            }
        }

        @Suppress("kotlin:S1871") // Molemmat aktiivisen session tarkistuspolut avaavat saman jatkotoiminnon.
        fun archiveProject(id: Long) {
            viewModelScope.launch {
                val project = repository.getProject(id) ?: return@launch
                when (
                    val result =
                        repository.completeProjectWithSessionChoice(
                            projectId = id,
                            totalRows = project.count,
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
                }
            }
        }

        fun deleteProject(id: Long) {
            viewModelScope.launch {
                when (val result = repository.deleteProjectResolvingActiveSession(id, false)) {
                    is ProjectDeletionResult.NeedsActiveSessionDiscard ->
                        _pendingDeletionSessionAction.value =
                            PendingProjectListSessionAction(result.session, setOf(id))
                    ProjectDeletionResult.Deleted,
                    ProjectDeletionResult.PersistenceFailure,
                    ProjectDeletionResult.ProjectUnavailable,
                    -> Unit
                }
            }
        }

        fun resolvePendingCompletion(saveSession: Boolean) {
            val pending = _pendingCompletionSessionAction.value ?: return
            viewModelScope.launch {
                if (saveSession && pending.session.needsRecoveryReview) {
                    _pendingCompletionSessionAction.value = null
                    exitMultiSelectMode()
                    navigateToProjectChannel.send(pending.session.projectId)
                    return@launch
                }
                completeProjects(
                    projectIds = pending.projectIds,
                    choice =
                        if (saveSession) {
                            ActiveSessionCompletionChoice.SAVE
                        } else {
                            ActiveSessionCompletionChoice.DISCARD
                        },
                )
                _pendingCompletionSessionAction.value = null
                exitMultiSelectMode()
            }
        }

        fun cancelPendingCompletion() {
            _pendingCompletionSessionAction.value = null
        }

        fun resolvePendingDeletion() {
            val pending = _pendingDeletionSessionAction.value ?: return
            viewModelScope.launch {
                pending.projectIds.forEach { id ->
                    repository.deleteProjectResolvingActiveSession(
                        id = id,
                        discardActiveSession = id == pending.session.projectId,
                    )
                }
                _pendingDeletionSessionAction.value = null
                exitMultiSelectMode()
            }
        }

        fun cancelPendingDeletion() {
            _pendingDeletionSessionAction.value = null
        }

        private suspend fun completeProjects(
            projectIds: Set<Long>,
            choice: ActiveSessionCompletionChoice?,
        ) {
            projectIds.forEach { id ->
                val project = repository.getProject(id) ?: return@forEach
                if (project.isCompleted) return@forEach
                repository.completeProjectWithSessionChoice(
                    projectId = id,
                    totalRows = project.count,
                    choice = choice.takeIf { activeSession.value?.projectId == id },
                )
            }
        }

        fun renameProject(
            id: Long,
            newName: String,
        ) {
            viewModelScope.launch {
                repository.updateProjectName(id, newName)
            }
        }

        fun reactivateProject(id: Long) {
            viewModelScope.launch {
                repository.reactivateProject(id)
            }
        }

        fun openNotesEditor(projectId: Long) {
            viewModelScope.launch {
                navigateToNotesEditorChannel.send(projectId)
            }
        }

        fun openPhotoGallery(projectId: Long) {
            viewModelScope.launch {
                navigateToPhotoGalleryChannel.send(projectId)
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
