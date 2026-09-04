package com.finnvek.knittools.ui.screens.counter

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.net.toUri
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.knittools.R
import com.finnvek.knittools.data.datastore.PreferencesManager
import com.finnvek.knittools.data.storage.AppFileStorage
import com.finnvek.knittools.data.storage.PatternDocumentStorage
import com.finnvek.knittools.di.ApplicationScope
import com.finnvek.knittools.di.IoDispatcher
import com.finnvek.knittools.domain.calculator.CounterLogic
import com.finnvek.knittools.domain.calculator.CounterState
import com.finnvek.knittools.domain.calculator.ReadingLineResolutionKind
import com.finnvek.knittools.domain.calculator.RepeatSectionLogic
import com.finnvek.knittools.domain.calculator.RowMarker
import com.finnvek.knittools.domain.calculator.parseMapping
import com.finnvek.knittools.domain.calculator.serializeMapping
import com.finnvek.knittools.domain.model.ActiveWorkSession
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.CraftType
import com.finnvek.knittools.domain.model.DEFAULT_READING_GUIDE_FRACTION
import com.finnvek.knittools.domain.model.DEFAULT_READING_LINE_Y_FRACTION
import com.finnvek.knittools.domain.model.MainCounterChange
import com.finnvek.knittools.domain.model.MainCounterLabelType
import com.finnvek.knittools.domain.model.ProgressPhoto
import com.finnvek.knittools.domain.model.ProjectCounter
import com.finnvek.knittools.domain.model.ProjectCounterDraft
import com.finnvek.knittools.domain.model.ProjectCounterType
import com.finnvek.knittools.domain.model.ProjectDocument
import com.finnvek.knittools.domain.model.ProjectYarnNote
import com.finnvek.knittools.domain.model.RowReminder
import com.finnvek.knittools.domain.model.SavedPattern
import com.finnvek.knittools.domain.model.YarnCard
import com.finnvek.knittools.domain.model.displayName
import com.finnvek.knittools.domain.model.isWebPatternCompatible
import com.finnvek.knittools.domain.model.parseYarnCardIds
import com.finnvek.knittools.domain.model.sanitizeReadingGuideFraction
import com.finnvek.knittools.domain.model.sanitizeReadingLineYFraction
import com.finnvek.knittools.pro.InAppReviewManager
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.pro.ProStatus
import com.finnvek.knittools.repository.ActiveSessionCompletionChoice
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.repository.ProgressPhotoRepository
import com.finnvek.knittools.repository.ProjectCompletionResult
import com.finnvek.knittools.repository.ProjectCounterRepository
import com.finnvek.knittools.repository.ProjectCreationResult
import com.finnvek.knittools.repository.ProjectDeletionResult
import com.finnvek.knittools.repository.ProjectDocumentMutationResult
import com.finnvek.knittools.repository.ProjectDocumentRepository
import com.finnvek.knittools.repository.ProjectNotesSaveResult
import com.finnvek.knittools.repository.ProjectYarnNoteRepository
import com.finnvek.knittools.repository.RecoveryResolutionResult
import com.finnvek.knittools.repository.ReminderMutationResult
import com.finnvek.knittools.repository.ReminderRepository
import com.finnvek.knittools.repository.SavedPatternMetadataMutationResult
import com.finnvek.knittools.repository.SavedPatternRepository
import com.finnvek.knittools.repository.StartSessionResult
import com.finnvek.knittools.repository.StopSessionResult
import com.finnvek.knittools.repository.YarnCardRepository
import com.finnvek.knittools.widget.CounterWidgetState
import com.finnvek.knittools.widget.WidgetData
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class CounterUiState(
    val projectName: String = "",
    val counter: CounterState = CounterState(),
    val craftType: CraftType = CraftType.KNITTING,
    val mainCounterLabelType: MainCounterLabelType = MainCounterLabelType.ROWS,
    val mainCounterCustomLabel: String? = null,
    val secondaryCount: Int = 0,
    val secondaryCounterUsed: Boolean = false,
    val notes: String = "",
    val notesCreated: Boolean = false,
    val sessionSeconds: Long = 0,
    val activeSession: ActiveWorkSession? = null,
    val sessionStartConflict: SessionStartConflict? = null,
    val pendingProjectCompletionSession: ActiveWorkSession? = null,
    val pendingProjectDeletionSession: ActiveWorkSession? = null,
    val showSessionRecoveryPrompt: Boolean = false,
    val sessionStopSummary: SessionStopSummary? = null,
    val workSessionErrorRes: Int? = null,
    val workSessionErrorCanRetry: Boolean = false,
    val projectId: Long? = null,
    val isCompleted: Boolean = false,
    val hapticFeedback: Boolean = true,
    val keepScreenAwake: Boolean = false,
    val isPro: Boolean = false,
    val proStatus: ProStatus = ProStatus.TRIAL_NOT_STARTED,
    val canCreateNotes: Boolean = false,
    val canUseNotes: Boolean = false,
    val canCreateSecondaryCounter: Boolean = false,
    val canUseSecondaryCounter: Boolean = false,
    val canUseMultipleCounters: Boolean = false,
    val canUseRowReminders: Boolean = false,
    val canUseProgressPhotos: Boolean = false,
    val canUsePatternCameraScan: Boolean = false,
    val canUseYarnCards: Boolean = false,
    val projects: List<CounterProject> = emptyList(),
    val projectsLoaded: Boolean = false,
    val sectionName: String? = null,
    val stitchCount: Int? = null,
    val stitchTrackingEnabled: Boolean = false,
    val currentStitch: Int = 0,
    val linkedYarns: List<Pair<Long, String>> = emptyList(),
    val projectYarnNotes: List<ProjectYarnNote> = emptyList(),
    val totalSessionMinutes: Int = 0,
    val reminders: List<RowReminder> = emptyList(),
    val activeAlert: RowReminder? = null,
    val dismissedReminderTrigger: DismissedReminderTrigger? = null,
    val projectCounters: List<ProjectCounter> = emptyList(),
    val latestPhotos: List<ProgressPhoto> = emptyList(),
    val linkedPattern: SavedPattern? = null,
    val projectDocuments: List<ProjectDocument> = emptyList(),
    val projectDocumentAvailability: Map<Long, Boolean> = emptyMap(),
    val patternUri: String? = null,
    val patternName: String? = null,
    val currentPatternPage: Int = 0,
    val readingLineEnabled: Boolean = false,
    val readingLineYFraction: Float = DEFAULT_READING_LINE_Y_FRACTION,
    val readingLineFollowCurrentRow: Boolean = true,
    val verticalReadingGuideEnabled: Boolean = false,
    val verticalReadingGuideXFraction: Float = DEFAULT_READING_GUIDE_FRACTION,
    val patternRowMapping: String? = null,
    val totalRows: Int? = null,
    val targetRows: Int? = null,
) {
    val primaryDocument: ProjectDocument?
        get() =
            projectDocuments.firstOrNull(ProjectDocument::isPrimary)
                ?: projectDocuments.minWithOrNull(compareBy(ProjectDocument::sortOrder, ProjectDocument::id))
}

data class SessionStartConflict(
    val activeSession: ActiveWorkSession,
    val requestedProjectId: Long,
)

data class SessionStopSummary(
    val sessionToken: String,
    val projectName: String,
    val durationSeconds: Long,
    val rowsWorked: Int,
    val endRow: Int,
)

data class DismissedReminderTrigger(
    val reminderId: Long,
    val row: Int,
)

sealed interface CounterViewerEvent {
    data class AutomaticReadingLinePageChanged(
        val row: Int,
        val page: Int,
    ) : CounterViewerEvent

    data class ReadingLineFollowingResumed(
        val calibrated: Boolean,
    ) : CounterViewerEvent
}

private data class PendingReminderDraft(
    val projectId: Long,
    val targetRow: Int,
    val repeatInterval: Int?,
    val message: String,
)

private data class PendingProjectCounterCreation(
    val projectId: Long,
    val mainRowCount: Int,
    val draft: ProjectCounterDraft,
)

private fun shouldAnnounceAutomaticPageChange(
    previousProject: CounterProject?,
    project: CounterProject,
): Boolean {
    if (previousProject == null) return false
    if (previousProject.id != project.id) return false
    if (previousProject.count == project.count) return false
    if (previousProject.currentPatternPage == project.currentPatternPage) return false
    return project.readingLineFollowCurrentRow
}

@HiltViewModel
class CounterViewModel
    @Inject
    constructor(
        private val repository: CounterRepository,
        private val reminderRepository: ReminderRepository,
        private val projectCounterRepository: ProjectCounterRepository,
        private val photoRepository: ProgressPhotoRepository,
        private val projectYarnNoteRepository: ProjectYarnNoteRepository,
        private val preferencesManager: PreferencesManager,
        private val proManager: ProManager,
        private val yarnCardRepository: YarnCardRepository,
        private val savedPatternRepository: SavedPatternRepository,
        private val projectDocumentRepository: ProjectDocumentRepository,
        private val patternDocumentStorage: PatternDocumentStorage,
        private val inAppReviewManager: InAppReviewManager,
        private val savedStateHandle: SavedStateHandle,
        @param:ApplicationContext private val context: Context,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
        @param:ApplicationScope private val applicationScope: CoroutineScope,
    ) : ViewModel() {
        private val _uiState =
            MutableStateFlow(
                CounterUiState(
                    projectName = context.getString(R.string.default_project_name),
                ),
            )
        val uiState: StateFlow<CounterUiState> = _uiState.asStateFlow()
        private val _viewerEvents = MutableSharedFlow<CounterViewerEvent>(extraBufferCapacity = 1)
        val viewerEvents: SharedFlow<CounterViewerEvent> = _viewerEvents.asSharedFlow()
        private val projectClosedEventChannel = Channel<Unit>(Channel.CONFLATED)
        val projectClosedEvents = projectClosedEventChannel.receiveAsFlow()

        val savedYarnCards: StateFlow<List<YarnCard>> =
            yarnCardRepository.getAllCards().stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList(),
            )

        val savedPatterns: StateFlow<List<SavedPattern>> =
            savedPatternRepository.getAll().stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList(),
            )

        private val _allPhotos = MutableStateFlow<List<ProgressPhoto>>(emptyList())
        val allPhotos: StateFlow<List<ProgressPhoto>> = _allPhotos.asStateFlow()

        private var selectedProjectJob: Job? = null
        private var projectSelectionJob: Job? = null
        private var workSessionActionJob: Job? = null
        private var pendingWorkSessionRetry: (() -> Unit)? = null
        private var reminderCollectionJob: Job? = null
        private var counterCollectionJob: Job? = null
        private var photoCollectionJob: Job? = null
        private var projectYarnNoteCollectionJob: Job? = null
        private var projectDocumentCollectionJob: Job? = null
        private var allPhotosJob: Job? = null
        private var linkedYarnIdsCache: String = ""
        private var pendingSavedPatternAttachment: SavedPattern? = null
        private var pendingProjectCounterCreation: PendingProjectCounterCreation? = null
        private var reminderCreationInFlight = false
        private var projectCounterCreationInFlight = false
        private var projectCompletionJob: Job? = null
        private var projectDeletionJob: Job? = null
        private var pendingReminderDraft: PendingReminderDraft? = null
        private var pendingProjectYarnNoteId: Long? = null
        private val lifecycleObserver =
            object : DefaultLifecycleObserver {
                override fun onResume(owner: LifecycleOwner) {
                    refreshActiveSession()
                }

                override fun onPause(owner: LifecycleOwner) {
                    applicationScope.launch {
                        runCatching { repository.checkpointActiveSession() }
                    }
                }
            }

        init {
            ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleObserver)
            observeProjects()
            observeActiveSession()
            observePreferences()
            observeProState()
        }

        private fun observePreferences() {
            viewModelScope.launch {
                preferencesManager.preferences.collect { prefs ->
                    _uiState.update {
                        it.copy(
                            hapticFeedback = prefs.hapticFeedback,
                            keepScreenAwake = prefs.keepScreenAwake,
                        )
                    }
                }
            }
        }

        private fun observeProState() {
            viewModelScope.launch {
                proManager.proState.collect { proState ->
                    _uiState.update {
                        val canCreateNotes = proState.hasFeature(ProFeature.NOTES)
                        val canCreateSecondaryCounter = proState.hasFeature(ProFeature.SECONDARY_COUNTER)
                        it.copy(
                            isPro = proState.isPro,
                            proStatus = proState.status,
                            canCreateNotes = canCreateNotes,
                            canUseNotes = it.notesCreated || canCreateNotes,
                            canCreateSecondaryCounter = canCreateSecondaryCounter,
                            canUseSecondaryCounter =
                                it.secondaryCounterUsed || canCreateSecondaryCounter,
                            canUseMultipleCounters = proState.hasFeature(ProFeature.MULTIPLE_COUNTERS),
                            canUseRowReminders = proState.hasFeature(ProFeature.ROW_REMINDERS),
                            canUseProgressPhotos = proState.hasFeature(ProFeature.PROGRESS_PHOTOS),
                            canUsePatternCameraScan = proState.hasFeature(ProFeature.PATTERN_CAMERA_SCAN),
                            canUseYarnCards = proState.hasFeature(ProFeature.UNLIMITED_YARN),
                        )
                    }
                }
            }
        }

        private fun observeProjects() {
            viewModelScope.launch {
                repository.getActiveProjects().collect { list ->
                    _uiState.update {
                        it.copy(
                            projects = list,
                            projectsLoaded = true,
                        )
                    }
                    if (selectedProjectJob != null) return@collect
                    if (list.isEmpty()) {
                        clearSelectedProject()
                    } else {
                        val currentId = _uiState.value.projectId ?: savedStateHandle.get<Long>(KEY_SELECTED_PROJECT_ID)
                        val targetProject =
                            currentId?.let { id -> list.find { it.id == id } }
                                ?: list.first()

                        if (_uiState.value.projectId != targetProject.id || selectedProjectJob == null) {
                            openProject(targetProject)
                        }
                    }
                }
            }
        }

        fun selectProject(project: CounterProject) {
            projectSelectionJob?.cancel()
            projectSelectionJob =
                viewModelScope.launch {
                    openProject(project)
                }
        }

        private fun observeSelectedProject(projectId: Long) {
            selectedProjectJob?.cancel()
            selectedProjectJob =
                viewModelScope.launch {
                    var previousObservedProject: CounterProject? = null
                    combine(
                        repository.observeProject(projectId),
                        savedPatternRepository.getAll(),
                    ) { project, patterns ->
                        project to project?.linkedPatternId?.let { id -> patterns.firstOrNull { it.id == id } }
                    }.collect { (project, linkedPattern) ->
                        previousObservedProject =
                            handleSelectedProjectObservation(
                                projectId = projectId,
                                project = project,
                                linkedPattern = linkedPattern,
                                previousObservedProject = previousObservedProject,
                            )
                    }
                }
        }

        private suspend fun handleSelectedProjectObservation(
            projectId: Long,
            project: CounterProject?,
            linkedPattern: SavedPattern?,
            previousObservedProject: CounterProject?,
        ): CounterProject? {
            if (project == null) {
                handleMissingSelectedProject(projectId)
                return previousObservedProject
            }

            val previousState = _uiState.value
            val countChanged = previousState.counter.count != project.count
            _uiState.update { state ->
                state.withObservedProject(project).copy(linkedPattern = linkedPattern)
            }
            if (shouldAnnounceAutomaticPageChange(previousObservedProject, project)) {
                _viewerEvents.emit(
                    CounterViewerEvent.AutomaticReadingLinePageChanged(
                        row = project.count,
                        page = project.currentPatternPage,
                    ),
                )
            }
            if (countChanged) {
                syncRepeatSectionCounters(project.count, _uiState.value.projectCounters, persist = true)
            }
            if (linkedYarnIdsCache != project.yarnCardIds) {
                linkedYarnIdsCache = project.yarnCardIds
                loadLinkedYarnNames(project.id, project.yarnCardIds)
            }
            return project
        }

        private fun handleMissingSelectedProject(projectId: Long) {
            val wasSelected = _uiState.value.projectId == projectId
            _uiState.update { state ->
                if (state.projectId == projectId) state.copy(projectId = null) else state
            }
            if (wasSelected) {
                clearSelectedProject()
                projectClosedEventChannel.trySend(Unit)
            }
        }

        fun linkYarnCard(cardId: Long) {
            val projectId = _uiState.value.projectId ?: return
            viewModelScope.launch {
                yarnCardRepository.updateLinkedProjectId(cardId, projectId)
            }
        }

        fun unlinkYarnCard(cardId: Long) {
            viewModelScope.launch {
                yarnCardRepository.updateLinkedProjectId(cardId, null)
            }
        }

        private suspend fun loadLinkedYarnNames(
            projectId: Long,
            yarnCardIds: String,
        ) {
            if (yarnCardIds.isBlank()) {
                _uiState.update { state ->
                    if (state.projectId == projectId) state.copy(linkedYarns = emptyList()) else state
                }
                return
            }
            val ids = parseYarnCardIds(yarnCardIds)
            val cardsById = yarnCardRepository.getCards(ids).associateBy { it.id }
            val yarns =
                ids.mapNotNull { id ->
                    cardsById[id]?.let { card ->
                        id to card.displayName(::fallbackYarnCardName)
                    }
                }
            _uiState.update { state ->
                if (state.projectId == projectId) state.copy(linkedYarns = yarns) else state
            }
        }

        private fun fallbackYarnCardName(id: Long): String = context.getString(R.string.yarn_card_number_fallback, id)

        private fun observeProjectYarnNotes(projectId: Long) {
            projectYarnNoteCollectionJob?.cancel()
            projectYarnNoteCollectionJob =
                viewModelScope.launch {
                    projectYarnNoteRepository.observeForProject(projectId).collect { notes ->
                        _uiState.update { it.copy(projectYarnNotes = notes) }
                    }
                }
        }

        fun saveProjectYarnNote(
            name: String,
            description: String,
            quantity: Int,
            notes: String,
        ) {
            val projectId = _uiState.value.projectId ?: return
            if (name.isBlank()) return
            viewModelScope.launch {
                projectYarnNoteRepository.save(
                    ProjectYarnNote(
                        projectId = projectId,
                        name = name,
                        description = description,
                        quantity = quantity,
                        notes = notes,
                    ),
                )
            }
        }

        fun deleteProjectYarnNote(noteId: Long) {
            viewModelScope.launch {
                projectYarnNoteRepository.delete(noteId)
            }
        }

        fun saveProjectYarnNoteToMyYarn(noteId: Long): Boolean {
            pendingProjectYarnNoteId = noteId
            return runProjectYarnNoteSaveIfAllowed(
                noteId = noteId,
                canUseYarnCards = proManager.hasFeature(ProFeature.UNLIMITED_YARN),
                save = { savePendingProjectYarnNoteToMyYarn() },
            )
        }

        fun retrySaveProjectYarnNoteToMyYarn(): Boolean {
            val noteId = pendingProjectYarnNoteId ?: return false
            return runProjectYarnNoteSaveIfAllowed(
                noteId = noteId,
                canUseYarnCards = proManager.hasFeature(ProFeature.UNLIMITED_YARN),
                save = { savePendingProjectYarnNoteToMyYarn() },
            )
        }

        private fun savePendingProjectYarnNoteToMyYarn() {
            val noteId = pendingProjectYarnNoteId ?: return
            pendingProjectYarnNoteId = null
            viewModelScope.launch {
                projectYarnNoteRepository.saveToMyYarn(noteId)
            }
        }

        private suspend fun loadTotalSessionMinutes(projectId: Long) {
            val minutes = repository.getTotalMinutesForProject(projectId)
            _uiState.update { state ->
                if (state.projectId == projectId) state.copy(totalSessionMinutes = minutes) else state
            }
        }

        private suspend fun openProject(project: CounterProject) {
            pruneHistory(project.id)
            linkedYarnIdsCache = project.yarnCardIds

            saveSelectedProject(project.id)

            _uiState.update { it.withStartedProject(project) }
            observeSelectedProject(project.id)
            observeReminders(project.id)
            observeProjectCounters(project.id)
            observeLatestPhotos(project.id)
            observeProjectYarnNotes(project.id)
            observeProjectDocuments(project.id)
            syncWidget()
            loadLinkedYarnNames(project.id, project.yarnCardIds)
            attachPendingSavedPatternIfReady()
            loadTotalSessionMinutes(project.id)
        }

        private fun saveSelectedProject(projectId: Long) {
            savedStateHandle[KEY_SELECTED_PROJECT_ID] = projectId
        }

        private fun observeProjectDocuments(projectId: Long) {
            projectDocumentCollectionJob?.cancel()
            projectDocumentCollectionJob =
                viewModelScope.launch {
                    combine(
                        projectDocumentRepository.observeDocuments(projectId),
                        projectDocumentRepository.observeActiveDocument(projectId),
                    ) { documents, activeDocument ->
                        documents to activeDocument
                    }.collect { (documents, activeDocument) ->
                        val availability =
                            documents.associate { document ->
                                document.id to
                                    try {
                                        projectDocumentRepository.isAvailable(document)
                                    } catch (cancellation: CancellationException) {
                                        throw cancellation
                                    } catch (_: Exception) {
                                        false
                                    }
                            }
                        val primary =
                            documents.firstOrNull(ProjectDocument::isPrimary)
                                ?: documents.minWithOrNull(compareBy(ProjectDocument::sortOrder, ProjectDocument::id))
                        _uiState.update { state ->
                            state.copy(
                                projectDocuments = documents,
                                projectDocumentAvailability = availability,
                                patternUri = primary?.localPdfUri,
                                patternName = primary?.label,
                                currentPatternPage = activeDocument?.currentPage ?: 0,
                                readingLineEnabled = activeDocument?.readingLineEnabled ?: false,
                                readingLineYFraction =
                                    activeDocument?.readingLineYFraction ?: DEFAULT_READING_LINE_Y_FRACTION,
                                readingLineFollowCurrentRow = activeDocument?.readingLineFollowCurrentRow ?: true,
                                verticalReadingGuideEnabled = activeDocument?.verticalReadingGuideEnabled ?: false,
                                verticalReadingGuideXFraction =
                                    activeDocument?.verticalReadingGuideXFraction ?: DEFAULT_READING_GUIDE_FRACTION,
                                patternRowMapping = activeDocument?.rowMapping,
                            )
                        }
                    }
                }
        }

        fun selectProjectDocument(
            documentId: Long,
            onResult: (ProjectDocumentMutationResult) -> Unit,
        ) = mutateProjectDocument(onResult) { projectId ->
            projectDocumentRepository.select(projectId, documentId)
        }

        fun renameProjectDocument(
            documentId: Long,
            label: String,
            onResult: (ProjectDocumentMutationResult) -> Unit,
        ) = mutateProjectDocument(onResult) { projectId ->
            projectDocumentRepository.rename(projectId, documentId, label)
        }

        fun moveProjectDocumentEarlier(
            documentId: Long,
            onResult: (ProjectDocumentMutationResult) -> Unit,
        ) = mutateProjectDocument(onResult) { projectId ->
            projectDocumentRepository.moveEarlier(projectId, documentId)
        }

        fun moveProjectDocumentLater(
            documentId: Long,
            onResult: (ProjectDocumentMutationResult) -> Unit,
        ) = mutateProjectDocument(onResult) { projectId ->
            projectDocumentRepository.moveLater(projectId, documentId)
        }

        fun setPrimaryProjectDocument(
            documentId: Long,
            onResult: (ProjectDocumentMutationResult) -> Unit,
        ) = mutateProjectDocument(onResult) { projectId ->
            projectDocumentRepository.setPrimary(projectId, documentId)
        }

        fun removeProjectDocument(
            documentId: Long,
            onResult: (ProjectDocumentMutationResult) -> Unit,
        ) = mutateProjectDocument(onResult) { projectId ->
            projectDocumentRepository.remove(projectId, documentId)
        }

        private fun mutateProjectDocument(
            onResult: (ProjectDocumentMutationResult) -> Unit,
            mutation: suspend (Long) -> ProjectDocumentMutationResult,
        ) {
            val projectId = _uiState.value.projectId ?: return
            viewModelScope.launch { onResult(mutation(projectId)) }
        }

        private fun clearSelectedProject() {
            savedStateHandle.remove<Long>(KEY_SELECTED_PROJECT_ID)
        }

        fun openSessionHistory(onReady: (Long) -> Unit) {
            _uiState.value.projectId?.let(onReady)
        }

        private fun observeActiveSession() {
            viewModelScope.launch {
                repository.observeActiveSession().collect { session ->
                    _uiState.update { state ->
                        state.copy(
                            activeSession = session,
                            sessionSeconds = session?.let(repository::activeSessionDurationSeconds) ?: 0L,
                            showSessionRecoveryPrompt =
                                session?.let { it.needsRecoveryReview && !it.recoveryPromptShown } == true,
                        )
                    }
                }
            }
            refreshActiveSession()
        }

        private fun refreshActiveSession() {
            viewModelScope.launch {
                val session = repository.refreshActiveSession()
                _uiState.update {
                    it.copy(
                        activeSession = session,
                        sessionSeconds = session?.let(repository::activeSessionDurationSeconds) ?: 0L,
                        showSessionRecoveryPrompt =
                            session?.needsRecoveryReview == true && !session.recoveryPromptShown,
                    )
                }
            }
        }

        fun refreshSessionPresentationTime() {
            val session = _uiState.value.activeSession ?: return
            if (!session.needsRecoveryReview && repository.activeSessionNeedsRecovery(session)) {
                refreshActiveSession()
                return
            }
            _uiState.update { state ->
                if (state.activeSession?.sessionToken == session.sessionToken) {
                    state.copy(sessionSeconds = repository.activeSessionDurationSeconds(session))
                } else {
                    state
                }
            }
        }

        fun startWorkSession() {
            val projectId = _uiState.value.projectId ?: return
            launchWorkSessionAction {
                when (val result = repository.startSession(projectId)) {
                    is StartSessionResult.ProjectConflict ->
                        _uiState.update {
                            it.copy(
                                sessionStartConflict =
                                    SessionStartConflict(result.activeSession, result.requestedProjectId),
                            )
                        }
                    is StartSessionResult.AlreadyActive,
                    is StartSessionResult.Started,
                    StartSessionResult.ProjectCompleted,
                    StartSessionResult.ProjectMissing,
                    -> Unit
                    StartSessionResult.PersistenceFailure ->
                        showWorkSessionError(R.string.work_session_could_not_start, ::startWorkSession)
                }
            }
        }

        fun cancelSessionStartConflict() {
            _uiState.update { it.copy(sessionStartConflict = null) }
        }

        fun resolveSessionStartConflict(saveCurrent: Boolean) {
            val conflict = _uiState.value.sessionStartConflict ?: return
            if (saveCurrent && conflict.activeSession.needsRecoveryReview) {
                _uiState.update {
                    it.copy(
                        sessionStartConflict = null,
                        showSessionRecoveryPrompt = true,
                    )
                }
                selectProjectById(conflict.activeSession.projectId)
                return
            }
            launchWorkSessionAction {
                val result =
                    repository.replaceActiveSession(
                        requestedProjectId = conflict.requestedProjectId,
                        expectedSessionToken = conflict.activeSession.sessionToken,
                        saveCurrent = saveCurrent,
                    )
                when {
                    result == StartSessionResult.PersistenceFailure ->
                        showWorkSessionError(R.string.work_session_could_not_start) {
                            resolveSessionStartConflict(saveCurrent)
                        }
                    result !is StartSessionResult.ProjectConflict ->
                        _uiState.update { it.copy(sessionStartConflict = null) }
                }
            }
        }

        fun returnToActiveSessionProject() {
            val activeProjectId =
                _uiState.value.sessionStartConflict
                    ?.activeSession
                    ?.projectId
                    ?: return
            _uiState.update { it.copy(sessionStartConflict = null) }
            selectProjectById(activeProjectId)
        }

        fun stopWorkSession() {
            val active = _uiState.value.activeSession ?: return
            if (active.needsRecoveryReview) {
                _uiState.update { it.copy(showSessionRecoveryPrompt = true) }
                return
            }
            _uiState.update { state ->
                state.copy(
                    sessionStopSummary =
                        SessionStopSummary(
                            sessionToken = active.sessionToken,
                            projectName = state.projectName,
                            durationSeconds = repository.activeSessionDurationSeconds(active),
                            rowsWorked = active.trustedRowsWorked,
                            endRow = active.trustedLastObservedRow,
                        ),
                )
            }
        }

        fun saveStoppedWorkSession() {
            val summary = _uiState.value.sessionStopSummary ?: return
            val projectId = _uiState.value.activeSession?.projectId ?: return
            launchWorkSessionAction {
                when (
                    repository.stopSession(
                        sessionToken = summary.sessionToken,
                        reviewedDurationSeconds = summary.durationSeconds,
                        reviewedRowsWorked = summary.rowsWorked,
                        reviewedEndRow = summary.endRow,
                    )
                ) {
                    is StopSessionResult.Saved -> {
                        loadTotalSessionMinutes(projectId)
                        _uiState.update { it.copy(sessionStopSummary = null) }
                    }
                    is StopSessionResult.NeedsRecoveryReview ->
                        _uiState.update {
                            it.copy(
                                sessionStopSummary = null,
                                showSessionRecoveryPrompt = true,
                            )
                        }
                    StopSessionResult.NoActiveSession,
                    StopSessionResult.StaleAction,
                    -> _uiState.update { it.copy(sessionStopSummary = null) }
                    StopSessionResult.Discarded -> Unit
                    StopSessionResult.PersistenceFailure ->
                        showWorkSessionError(R.string.work_session_could_not_save, ::saveStoppedWorkSession)
                }
            }
        }

        fun discardStoppedWorkSession() {
            val summary = _uiState.value.sessionStopSummary ?: return
            launchWorkSessionAction {
                when (repository.discardActiveSession(summary.sessionToken)) {
                    StopSessionResult.Discarded,
                    StopSessionResult.NoActiveSession,
                    StopSessionResult.StaleAction,
                    -> _uiState.update { it.copy(sessionStopSummary = null) }
                    is StopSessionResult.NeedsRecoveryReview,
                    is StopSessionResult.Saved,
                    -> Unit
                    StopSessionResult.PersistenceFailure ->
                        showWorkSessionError(R.string.work_session_could_not_discard, ::discardStoppedWorkSession)
                }
            }
        }

        fun cancelSessionStop() {
            _uiState.update { it.copy(sessionStopSummary = null) }
        }

        fun dismissRecoveryPrompt() {
            val active = _uiState.value.activeSession ?: return
            val intervalToken = active.recoveryIntervalToken ?: return
            _uiState.update { it.copy(showSessionRecoveryPrompt = false) }
            viewModelScope.launch {
                repository.markRecoveryPromptShown(active.sessionToken, intervalToken)
            }
        }

        fun showRecoveryPrompt() {
            if (_uiState.value.activeSession?.needsRecoveryReview == true) {
                _uiState.update { it.copy(showSessionRecoveryPrompt = true) }
            }
        }

        fun addRecoveryInterval(durationSeconds: Long) {
            val active = _uiState.value.activeSession ?: return
            val intervalToken = active.recoveryIntervalToken ?: return
            launchWorkSessionAction {
                when (
                    repository.addRecoveryInterval(
                        sessionToken = active.sessionToken,
                        recoveryIntervalToken = intervalToken,
                        durationSeconds = durationSeconds,
                    )
                ) {
                    is RecoveryResolutionResult.Continued ->
                        _uiState.update { it.copy(showSessionRecoveryPrompt = false) }
                    RecoveryResolutionResult.InvalidDuration ->
                        showWorkSessionError(R.string.work_session_invalid_duration)
                    RecoveryResolutionResult.StaleAction ->
                        showWorkSessionError(R.string.work_session_recovery_already_handled)
                    RecoveryResolutionResult.PersistenceFailure ->
                        showWorkSessionError(R.string.work_session_could_not_save) {
                            addRecoveryInterval(durationSeconds)
                        }
                    is RecoveryResolutionResult.DiscardedAndStopped,
                    is RecoveryResolutionResult.EditedAndStopped,
                    -> Unit
                }
            }
        }

        fun discardRecoveryInterval() {
            val active = _uiState.value.activeSession ?: return
            val intervalToken = active.recoveryIntervalToken ?: return
            launchWorkSessionAction {
                when (
                    repository.discardRecoveryInterval(
                        active.sessionToken,
                        intervalToken,
                    )
                ) {
                    is RecoveryResolutionResult.DiscardedAndStopped -> {
                        loadTotalSessionMinutes(active.projectId)
                        _uiState.update { it.copy(showSessionRecoveryPrompt = false) }
                    }
                    RecoveryResolutionResult.StaleAction ->
                        showWorkSessionError(R.string.work_session_recovery_already_handled)
                    RecoveryResolutionResult.PersistenceFailure ->
                        showWorkSessionError(R.string.work_session_could_not_discard, ::discardRecoveryInterval)
                    is RecoveryResolutionResult.Continued,
                    is RecoveryResolutionResult.EditedAndStopped,
                    RecoveryResolutionResult.InvalidDuration,
                    -> Unit
                }
            }
        }

        fun editRecoveryDurationAndStop(totalDurationSeconds: Long) {
            val active = _uiState.value.activeSession ?: return
            val intervalToken = active.recoveryIntervalToken ?: return
            launchWorkSessionAction {
                when (
                    repository.editRecoveryDurationAndStop(
                        sessionToken = active.sessionToken,
                        recoveryIntervalToken = intervalToken,
                        totalDurationSeconds = totalDurationSeconds,
                    )
                ) {
                    is RecoveryResolutionResult.EditedAndStopped -> {
                        loadTotalSessionMinutes(active.projectId)
                        _uiState.update { it.copy(showSessionRecoveryPrompt = false) }
                    }
                    RecoveryResolutionResult.InvalidDuration ->
                        showWorkSessionError(R.string.work_session_invalid_duration)
                    RecoveryResolutionResult.StaleAction ->
                        showWorkSessionError(R.string.work_session_recovery_already_handled)
                    RecoveryResolutionResult.PersistenceFailure ->
                        showWorkSessionError(R.string.work_session_could_not_save) {
                            editRecoveryDurationAndStop(totalDurationSeconds)
                        }
                    is RecoveryResolutionResult.Continued,
                    is RecoveryResolutionResult.DiscardedAndStopped,
                    -> Unit
                }
            }
        }

        fun createNewProject(name: String): Boolean {
            if (name.isBlank()) return false
            viewModelScope.launch {
                when (
                    val result =
                        repository.createProject(
                            name = name,
                            canCreateAdditionalProjects =
                                proManager.hasFeature(ProFeature.UNLIMITED_PROJECTS),
                        )
                ) {
                    is ProjectCreationResult.Created ->
                        repository.getProject(result.projectId)?.let { selectProject(it) }
                    ProjectCreationResult.InvalidProject,
                    ProjectCreationResult.LimitReached,
                    ProjectCreationResult.FolderMissing,
                    -> Unit
                }
            }
            return true
        }

        fun completeProject() {
            val state = _uiState.value
            val projectId = state.projectId ?: return
            launchProjectCompletion(projectId, choice = null)
        }

        fun completeProjectWithActiveSession(saveSession: Boolean) {
            val state = _uiState.value
            val projectId = state.pendingProjectCompletionSession?.projectId ?: return
            if (state.projectId != projectId) {
                cancelProjectCompletionSessionChoice()
                return
            }
            launchProjectCompletion(
                projectId,
                choice =
                    if (saveSession) {
                        ActiveSessionCompletionChoice.SAVE
                    } else {
                        ActiveSessionCompletionChoice.DISCARD
                    },
            )
        }

        private fun launchProjectCompletion(
            projectId: Long,
            choice: ActiveSessionCompletionChoice?,
        ) {
            if (projectCompletionJob?.isActive == true) return
            projectCompletionJob =
                viewModelScope.launch {
                    try {
                        handleProjectCompletionResult(
                            projectId,
                            repository.completeProjectWithSessionChoice(projectId, choice),
                        )
                    } finally {
                        projectCompletionJob = null
                    }
                }
        }

        fun cancelProjectCompletionSessionChoice() {
            _uiState.update { it.copy(pendingProjectCompletionSession = null) }
        }

        private fun handleProjectCompletionResult(
            projectId: Long,
            result: ProjectCompletionResult,
        ) {
            when (result) {
                ProjectCompletionResult.Completed -> {
                    val closedCurrentProject = _uiState.value.projectId == projectId
                    _uiState.update { state ->
                        when {
                            state.projectId == projectId ->
                                state.copy(
                                    projectId = null,
                                    pendingProjectCompletionSession = null,
                                )
                            state.pendingProjectCompletionSession?.projectId == projectId ->
                                state.copy(pendingProjectCompletionSession = null)
                            else -> state
                        }
                    }
                    if (closedCurrentProject) {
                        clearSelectedProject()
                        projectClosedEventChannel.trySend(Unit)
                    }
                }
                is ProjectCompletionResult.NeedsActiveSessionChoice ->
                    _uiState.update { it.copy(pendingProjectCompletionSession = result.session) }
                is ProjectCompletionResult.NeedsRecoveryReview ->
                    _uiState.update {
                        it.copy(
                            pendingProjectCompletionSession = null,
                            showSessionRecoveryPrompt = true,
                        )
                    }
                ProjectCompletionResult.ProjectUnavailable -> Unit
                ProjectCompletionResult.PersistenceFailure ->
                    showWorkSessionError(R.string.work_session_could_not_save, ::completeProject)
            }
        }

        fun setTargetRows(target: Int?) {
            val projectId = _uiState.value.projectId ?: return
            val validated = target?.takeIf { it > 0 }
            _uiState.update { it.copy(targetRows = validated) }
            viewModelScope.launch {
                repository.setTargetRows(projectId, validated)
                syncWidget()
            }
        }

        fun clearTarget() {
            setTargetRows(null)
        }

        fun deleteProject(id: Long) {
            launchProjectDeletion(id, discardActiveSession = false)
        }

        fun deleteProjectDiscardingActiveSession() {
            val active = _uiState.value.pendingProjectDeletionSession ?: return
            launchProjectDeletion(active.projectId, discardActiveSession = true)
        }

        private fun launchProjectDeletion(
            projectId: Long,
            discardActiveSession: Boolean,
        ) {
            if (projectDeletionJob?.isActive == true) return
            projectDeletionJob =
                viewModelScope.launch {
                    try {
                        handleProjectDeletionResult(
                            id = projectId,
                            result =
                                repository.deleteProjectResolvingActiveSession(
                                    id = projectId,
                                    discardActiveSession = discardActiveSession,
                                ),
                        )
                    } finally {
                        projectDeletionJob = null
                    }
                }
        }

        fun reactivateProject() {
            val state = _uiState.value
            val projectId = state.projectId ?: return
            if (!state.isCompleted) return
            viewModelScope.launch { repository.reactivateProject(projectId) }
        }

        fun cancelProjectDeletionSessionChoice() {
            _uiState.update { it.copy(pendingProjectDeletionSession = null) }
        }

        private fun handleProjectDeletionResult(
            id: Long,
            result: ProjectDeletionResult,
        ) {
            when (result) {
                ProjectDeletionResult.Deleted -> {
                    if (_uiState.value.projectId == id) {
                        _uiState.update {
                            it.copy(
                                projectId = null,
                                pendingProjectDeletionSession = null,
                            )
                        }
                        clearSelectedProject()
                        projectClosedEventChannel.trySend(Unit)
                    }
                }
                is ProjectDeletionResult.NeedsActiveSessionDiscard ->
                    _uiState.update { it.copy(pendingProjectDeletionSession = result.session) }
                ProjectDeletionResult.ProjectUnavailable -> Unit
                ProjectDeletionResult.PersistenceFailure ->
                    showWorkSessionError(R.string.work_session_could_not_discard) { deleteProject(id) }
            }
        }

        private fun showWorkSessionError(
            messageRes: Int,
            retry: (() -> Unit)? = null,
        ) {
            pendingWorkSessionRetry = retry
            _uiState.update {
                it.copy(
                    workSessionErrorRes = messageRes,
                    workSessionErrorCanRetry = retry != null,
                )
            }
        }

        fun dismissWorkSessionError() {
            pendingWorkSessionRetry = null
            _uiState.update { it.copy(workSessionErrorRes = null, workSessionErrorCanRetry = false) }
        }

        fun retryWorkSessionAction() {
            val retry = pendingWorkSessionRetry
            dismissWorkSessionError()
            retry?.invoke()
        }

        private fun launchWorkSessionAction(block: suspend () -> Unit) {
            if (workSessionActionJob?.isActive == true) return
            workSessionActionJob = viewModelScope.launch { block() }
        }

        fun increment() {
            val state = _uiState.value
            state.projectId ?: return
            // CPD-OFF: Ruudun paikallinen Compose-rakenne pidetaan vastuun yhteydessa.
            val updatedCounter = CounterLogic.increment(state.counter)
            if (updatedCounter.count == state.counter.count) return
            val resetStitch = state.stitchTrackingEnabled && updatedCounter.count != state.counter.count
            _uiState.update { it.withCounterChange(updatedCounter, resetStitch) }
            syncRepeatSectionCounters(updatedCounter.count, state.projectCounters, persist = true)
            persistCount(
                action = "increment",
                // CPD-ON
                previousValue = state.counter.count,
                newValue = updatedCounter.count,
            )
        }

        // CPD-OFF: Laskuritoiminnon paikallinen tila- ja tallennusketju pidetaan yhtenaisena.
        fun decrement() {
            val state = _uiState.value
            state.projectId ?: return
            val updatedCounter = CounterLogic.decrement(state.counter)
            if (updatedCounter.count == state.counter.count) return
            val resetStitch = state.stitchTrackingEnabled && updatedCounter.count != state.counter.count
            _uiState.update { it.withCounterChange(updatedCounter, resetStitch) }
            syncRepeatSectionCounters(updatedCounter.count, state.projectCounters, persist = true)
            persistCount(
                action = "decrement",
                previousValue = state.counter.count,
                newValue = updatedCounter.count,
            )
        }
        // CPD-ON

        fun undo() {
            val state = _uiState.value
            val projectId = state.projectId ?: return
            viewModelScope.launch {
                val changed = repository.applyMainCounterChange(projectId, MainCounterChange.Undo)
                if (!changed) return@launch
                val updatedProject = repository.getProject(projectId) ?: return@launch
                val currentState = _uiState.value
                if (currentState.projectId != projectId) return@launch

                val updatedCounter =
                    CounterState(
                        count = updatedProject.count,
                        stepSize = updatedProject.stepSize,
                    )
                val resetStitch =
                    currentState.stitchTrackingEnabled &&
                        updatedCounter.count != currentState.counter.count
                _uiState.update { latestState ->
                    if (latestState.projectId == projectId) {
                        latestState.withCounterChange(updatedCounter, resetStitch)
                    } else {
                        latestState
                    }
                }
                syncRepeatSectionCounters(updatedCounter.count, _uiState.value.projectCounters, persist = true)
                inAppReviewManager.recordAction()
                syncWidget(projectId, _uiState.value.projectName, updatedCounter.count)
            }
        }

        fun reset() {
            val state = _uiState.value
            state.projectId ?: return
            val updatedCounter = CounterLogic.reset(state.counter)
            if (updatedCounter.count == state.counter.count) return
            val resetStitch = state.stitchTrackingEnabled && updatedCounter.count != state.counter.count
            _uiState.update { it.withCounterChange(updatedCounter, resetStitch) }
            syncRepeatSectionCounters(updatedCounter.count, state.projectCounters, persist = true)
            persistCount(
                action = "reset",
                previousValue = state.counter.count,
                newValue = updatedCounter.count,
            )
        }

        fun incrementSecondary() {
            if (!_uiState.value.canUseSecondaryCounter) return
            _uiState.update { it.copy(secondaryCount = it.secondaryCount + 1) }
            persistSecondary()
        }

        fun decrementSecondary() {
            if (!_uiState.value.canUseSecondaryCounter) return
            _uiState.update { it.copy(secondaryCount = maxOf(0, it.secondaryCount - 1)) }
            persistSecondary()
        }

        private fun persistSecondary() {
            val state = _uiState.value
            val projectId = state.projectId ?: return
            val secondaryCount = state.secondaryCount
            viewModelScope.launch {
                if (!repository.updateProjectSecondaryCount(projectId, secondaryCount)) {
                    val persisted = repository.getProject(projectId) ?: return@launch
                    _uiState.update { current ->
                        if (current.projectId == projectId) {
                            current.copy(
                                secondaryCount = persisted.secondaryCount,
                                secondaryCounterUsed = persisted.secondaryCounterUsed,
                            )
                        } else {
                            current
                        }
                    }
                }
            }
        }

        // — Multiple Counters —

        private fun observeProjectCounters(projectId: Long) {
            counterCollectionJob?.cancel()
            counterCollectionJob =
                viewModelScope.launch {
                    projectCounterRepository.getCountersForProject(projectId).collect { counters ->
                        val visibleCounters = withoutLegacySecondaryBackfillCopies(counters)
                        syncRepeatSectionCounters(
                            mainRowCount = _uiState.value.counter.count,
                            counters = visibleCounters,
                            persist = true,
                        )
                    }
                }
        }

        fun addProjectCounter(draft: ProjectCounterDraft): Boolean {
            if (projectCounterCreationInFlight) return false
            val state = _uiState.value
            val projectId = state.projectId ?: return false
            pendingProjectCounterCreation = PendingProjectCounterCreation(projectId, state.counter.count, draft)
            if (!canAddProjectCounter(draft)) return false
            addPendingProjectCounter()
            return true
        }

        fun retryAddProjectCounter(): Boolean {
            val pending = pendingProjectCounterCreation ?: return false
            if (!canAddProjectCounter(pending.draft)) return false
            addPendingProjectCounter()
            return true
        }

        private fun addPendingProjectCounter() {
            val pending = pendingProjectCounterCreation ?: return
            pendingProjectCounterCreation = null
            projectCounterCreationInFlight = true
            viewModelScope.launch {
                try {
                    val draft = pending.draft
                    val counter =
                        ProjectCounter(
                            projectId = pending.projectId,
                            name = draft.name,
                            repeatAt = draft.repeatAt,
                            stepSize = draft.stepSize,
                            counterType = draft.counterType,
                            startingStitches = draft.startingStitches,
                            stitchChange = draft.stitchChange,
                            shapeEveryN = draft.shapeEveryN,
                            repeatStartRow = draft.repeatStartRow,
                            repeatEndRow = draft.repeatEndRow,
                            totalRepeats = draft.totalRepeats,
                            currentRepeat = draft.currentRepeat,
                            linkedToMainCounter = draft.linkedToMainCounter,
                        )
                    val initialCounter =
                        if (draft.counterType == ProjectCounterType.REPEAT_SECTION) {
                            RepeatSectionLogic.updatePosition(counter, pending.mainRowCount)
                        } else {
                            counter
                        }
                    projectCounterRepository.addCounter(initialCounter)
                } finally {
                    projectCounterCreationInFlight = false
                }
            }
        }

        private fun canAddProjectCounter(draft: ProjectCounterDraft): Boolean {
            if (!proManager.hasFeature(ProFeature.MULTIPLE_COUNTERS)) return false
            return when (draft.counterType) {
                ProjectCounterType.SHAPING -> proManager.hasFeature(ProFeature.SHAPING_COUNTER)
                ProjectCounterType.REPEAT_SECTION -> proManager.hasFeature(ProFeature.REPEAT_SECTION)
                else -> true
            }
        }

        private fun canUseProjectCounter(counter: ProjectCounter): Boolean =
            _uiState.value.projectCounters.any {
                it.id == counter.id
            }

        private fun canUseProjectCounter(counterId: Long): Boolean =
            _uiState.value.projectCounters
                .firstOrNull { it.id == counterId }
                ?.let(::canUseProjectCounter) == true

        fun incrementProjectCounter(counter: ProjectCounter) {
            if (!canUseProjectCounter(counter)) return
            viewModelScope.launch {
                projectCounterRepository.incrementCounter(counter)
            }
        }

        fun decrementProjectCounter(counter: ProjectCounter) {
            if (!canUseProjectCounter(counter)) return
            viewModelScope.launch {
                projectCounterRepository.decrementCounter(counter)
            }
        }

        fun resetProjectCounter(counterId: Long) {
            if (!canUseProjectCounter(counterId)) return
            val projectId = _uiState.value.projectId ?: return
            viewModelScope.launch {
                projectCounterRepository.resetCounter(projectId, counterId)
            }
        }

        fun deleteProjectCounter(counterId: Long) {
            if (!canUseProjectCounter(counterId)) return
            val projectId = _uiState.value.projectId ?: return
            viewModelScope.launch {
                projectCounterRepository.deleteCounter(projectId, counterId)
            }
        }

        fun renameProjectCounter(
            counterId: Long,
            name: String,
        ) {
            if (!canUseProjectCounter(counterId)) return
            val projectId = _uiState.value.projectId ?: return
            viewModelScope.launch {
                projectCounterRepository.renameCounter(projectId, counterId, name)
            }
        }

        // — Row Reminders —

        private fun observeReminders(projectId: Long) {
            reminderCollectionJob?.cancel()
            reminderCollectionJob =
                viewModelScope.launch {
                    reminderRepository.getRemindersForProject(projectId).collect { reminders ->
                        _uiState.update { it.withReminderList(reminders) }
                    }
                }
        }

        fun addReminder(
            targetRow: Int,
            repeatInterval: Int?,
            message: String,
        ): Boolean {
            val projectId = _uiState.value.projectId ?: return false
            pendingReminderDraft = PendingReminderDraft(projectId, targetRow, repeatInterval, message)
            if (!proManager.hasFeature(ProFeature.ROW_REMINDERS)) return false
            return addPendingReminder()
        }

        fun retryAddReminder(): Boolean {
            if (!proManager.hasFeature(ProFeature.ROW_REMINDERS)) return false
            return addPendingReminder()
        }

        private fun addPendingReminder(): Boolean {
            val draft = pendingReminderDraft ?: return false
            if (reminderCreationInFlight) return false
            reminderCreationInFlight = true
            pendingReminderDraft = null
            viewModelScope.launch {
                try {
                    reminderRepository.insert(
                        RowReminder(
                            projectId = draft.projectId,
                            targetRow = draft.targetRow,
                            repeatInterval = draft.repeatInterval,
                            message = draft.message,
                        ),
                    )
                } finally {
                    reminderCreationInFlight = false
                }
            }
            return true
        }

        fun updateReminder(
            reminderId: Long,
            targetRow: Int,
            repeatInterval: Int?,
            message: String,
        ) {
            val state = _uiState.value
            val projectId = state.projectId ?: return
            val reminder = state.reminders.find { it.id == reminderId } ?: return
            viewModelScope.launch {
                reminderRepository.update(
                    projectId,
                    reminder.copy(
                        targetRow = targetRow,
                        repeatInterval = repeatInterval,
                        message = message,
                        isCompleted = false,
                    ),
                )
            }
        }

        fun dismissReminder(reminderId: Long) {
            val state = _uiState.value
            val projectId = state.projectId ?: return
            val reminder = state.reminders.find { it.id == reminderId } ?: return
            viewModelScope.launch {
                if (reminder.repeatInterval == null) {
                    // Kertaluonteinen — merkitään valmiiksi
                    val result = reminderRepository.update(projectId, reminder.copy(isCompleted = true))
                    if (result !is ReminderMutationResult.Success) return@launch
                }
                // Toistuva — piilotetaan vain UI-alertista seuraavaan riviin asti
                _uiState.update {
                    if (it.projectId == projectId) it.withDismissedReminder(reminderId) else it
                }
            }
        }

        fun deleteReminder(reminderId: Long) {
            val projectId = _uiState.value.projectId ?: return
            viewModelScope.launch {
                reminderRepository.delete(projectId, reminderId)
            }
        }

        // — Progress Photos —

        private fun observeLatestPhotos(projectId: Long) {
            photoCollectionJob?.cancel()
            photoCollectionJob =
                viewModelScope.launch {
                    photoRepository.getLatestPhotos(projectId).collect { photos ->
                        _uiState.update { it.copy(latestPhotos = photos) }
                    }
                }
            allPhotosJob?.cancel()
            allPhotosJob =
                viewModelScope.launch {
                    photoRepository.getPhotosForProject(projectId).collect { photos ->
                        _allPhotos.value = photos
                    }
                }
        }

        fun savePhoto(sourceUri: Uri) {
            if (!canFinishProgressPhotoCreation()) return
            val state = _uiState.value
            val projectId = state.projectId ?: return
            val row = state.counter.count
            viewModelScope.launch {
                photoRepository.savePhoto(projectId, sourceUri, row)
                savedStateHandle.remove<Boolean>(KEY_PROGRESS_PHOTO_CREATION_AUTHORIZED)
            }
        }

        fun authorizeProgressPhotoCreation() {
            savedStateHandle[KEY_PROGRESS_PHOTO_CREATION_AUTHORIZED] = true
        }

        fun cancelProgressPhotoCreation() {
            savedStateHandle.remove<Boolean>(KEY_PROGRESS_PHOTO_CREATION_AUTHORIZED)
        }

        private fun canFinishProgressPhotoCreation(): Boolean =
            proManager.hasFeature(ProFeature.PROGRESS_PHOTOS) ||
                savedStateHandle.get<Boolean>(KEY_PROGRESS_PHOTO_CREATION_AUTHORIZED) == true

        fun createPhotoCaptureTarget(
            projectId: Long,
            onCreated: (PhotoCaptureTarget?) -> Unit,
        ) {
            if (!canFinishProgressPhotoCreation()) {
                onCreated(null)
                return
            }
            viewModelScope.launch {
                val target =
                    runCatching {
                        val (file, uri) = photoRepository.createPhotoCaptureTarget(projectId)
                        PhotoCaptureTarget(
                            uri = uri,
                            filePath = file.absolutePath,
                        )
                    }.onFailure { failure ->
                        if (failure is CancellationException) throw failure
                    }.getOrNull()
                onCreated(target)
            }
        }

        fun deletePendingPhotoFile(filePath: String?) {
            viewModelScope.launch {
                photoRepository.deletePendingPhotoFile(filePath)
            }
        }

        fun updatePhotoNote(
            photoId: Long,
            note: String?,
        ) {
            viewModelScope.launch {
                photoRepository.updatePhotoNote(photoId, note)
            }
        }

        fun deletePhoto(photo: ProgressPhoto) {
            viewModelScope.launch {
                photoRepository.deletePhoto(photo)
            }
        }

        fun setNotes(notes: String) {
            val state = _uiState.value
            if (!state.canUseNotes) return
            val previousNotes = state.notes
            _uiState.update { it.copy(notes = notes) }
            viewModelScope.launch {
                val id = state.projectId ?: return@launch
                val savedProject =
                    repository
                        .saveProjectNotes(
                            id = id,
                            baseNotes = previousNotes,
                            requestedNotes = notes,
                        ).let { result ->
                            (result as? ProjectNotesSaveResult.Saved)?.project ?: return@launch
                        }
                _uiState.update { currentState ->
                    if (currentState.projectId == id && currentState.notes == notes) {
                        currentState.copy(notes = savedProject.notes)
                    } else {
                        currentState
                    }
                }
            }
        }

        fun setProjectName(name: String) {
            val projectName = name.trim()
            if (projectName.isEmpty()) return
            val id = _uiState.value.projectId ?: return
            viewModelScope.launch {
                val savedName = repository.updateProjectName(id, projectName) ?: return@launch
                _uiState.update { state ->
                    if (state.projectId == id) {
                        state.copy(projectName = savedName)
                    } else {
                        state
                    }
                }
                if (_uiState.value.projectId == id) {
                    syncWidget(projectId = id, projectName = savedName)
                }
            }
        }

        fun setProjectDetails(
            name: String,
            craftType: CraftType,
            mainCounterLabelType: MainCounterLabelType,
            mainCounterCustomLabel: String?,
        ) {
            val projectName = name.trim()
            if (projectName.isEmpty()) return
            val id = _uiState.value.projectId ?: return
            viewModelScope.launch {
                val savedProject =
                    repository.updateProjectDetails(
                        id = id,
                        name = projectName,
                        craftType = craftType,
                        mainCounterLabelType = mainCounterLabelType,
                        mainCounterCustomLabel = mainCounterCustomLabel,
                    ) ?: return@launch
                _uiState.update { state ->
                    if (state.projectId == id) {
                        state.withObservedProject(savedProject)
                    } else {
                        state
                    }
                }
                if (_uiState.value.projectId == id) {
                    syncWidget(projectId = id, projectName = savedProject.name)
                }
            }
        }

        fun setStepSize(size: Int) {
            val state = _uiState.value
            val updatedCounter = CounterLogic.setStepSize(state.counter, size)
            _uiState.update { it.copy(counter = updatedCounter) }
            viewModelScope.launch {
                val id = state.projectId ?: return@launch
                repository.updateProjectStepSize(id, updatedCounter.stepSize)
            }
        }

        fun attachPattern(
            uri: String,
            name: String,
            onResult: (ProjectDocumentMutationResult) -> Unit = {},
        ) {
            if (uri.isBlank()) {
                onResult(ProjectDocumentMutationResult.PersistenceFailure)
                return
            }
            val state = _uiState.value
            val projectId = state.projectId ?: return

            viewModelScope.launch {
                val sourceUri = uri.toUri()
                val sanitizedName =
                    withContext(ioDispatcher) {
                        takePersistableReadPermission(sourceUri)
                        resolvePatternName(sourceUri, name)
                    }
                val attachment =
                    preparePatternAttachment(
                        sourceUriString = uri,
                        projectId = projectId,
                        sanitizedName = sanitizedName,
                    )
                if (attachment == null) {
                    onResult(ProjectDocumentMutationResult.PersistenceFailure)
                    return@launch
                }

                onResult(
                    persistPatternAttachment(
                        projectId = projectId,
                        patternName = sanitizedName,
                        attachment = attachment,
                    ),
                )
            }
        }

        private fun takePersistableReadPermission(uri: Uri) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }

        private fun resolvePatternName(
            uri: Uri,
            providedName: String,
        ): String {
            if (providedName.isNotBlank()) return providedName
            context.contentResolver
                .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    val columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (columnIndex >= 0 && cursor.moveToFirst()) {
                        cursor.getString(columnIndex)?.takeIf(String::isNotBlank)?.let { return it }
                    }
                }
            return uri.lastPathSegment ?: context.getString(R.string.pattern_pdf_fallback_name)
        }

        fun attachSavedPattern(pattern: SavedPattern) {
            if (pattern.id <= 0L) return
            val projectId = _uiState.value.projectId
            if (projectId == null) {
                pendingSavedPatternAttachment = pattern
                return
            }
            pendingSavedPatternAttachment = null
            if (pattern.isWebPatternCompatible) {
                attachSavedPatternMetadata(pattern.id)
                return
            }
            viewModelScope.launch {
                repository.attachSavedPattern(
                    projectId = projectId,
                    savedPatternId = pattern.id,
                )
            }
        }

        fun attachSavedPatternMetadata(
            savedPatternId: Long,
            expectedExistingSavedPatternId: Long? = null,
            onResult: (SavedPatternMetadataMutationResult) -> Unit = {},
        ) {
            val projectId = _uiState.value.projectId
            if (projectId == null || savedPatternId <= 0L) {
                onResult(SavedPatternMetadataMutationResult.ProjectMissing)
                return
            }
            viewModelScope.launch {
                val result =
                    try {
                        repository.attachSavedPatternMetadata(
                            projectId = projectId,
                            savedPatternId = savedPatternId,
                            expectedExistingSavedPatternId = expectedExistingSavedPatternId,
                        )
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (_: Exception) {
                        SavedPatternMetadataMutationResult.PersistenceFailure
                    }
                onResult(result)
            }
        }

        fun unlinkSavedPatternMetadata(
            expectedSavedPatternId: Long,
            onResult: (SavedPatternMetadataMutationResult) -> Unit = {},
        ) {
            val projectId = _uiState.value.projectId
            if (projectId == null || expectedSavedPatternId <= 0L) {
                onResult(SavedPatternMetadataMutationResult.ProjectMissing)
                return
            }
            viewModelScope.launch {
                val result =
                    try {
                        repository.unlinkSavedPatternMetadata(projectId, expectedSavedPatternId)
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (_: Exception) {
                        SavedPatternMetadataMutationResult.PersistenceFailure
                    }
                onResult(result)
            }
        }

        private fun attachPendingSavedPatternIfReady() {
            val pattern = pendingSavedPatternAttachment ?: return
            if (_uiState.value.projectId == null) return
            attachSavedPattern(pattern)
        }

        private suspend fun preparePatternAttachment(
            sourceUriString: String,
            projectId: Long,
            sanitizedName: String,
        ): PatternAttachment? {
            val sourceUri = sourceUriString.toUri()
            val sourceIsAppOwned =
                withContext(ioDispatcher) {
                    AppFileStorage.isAppOwnedUri(context, sourceUri)
                }
            if (sourceIsAppOwned && savedPatternRepository.pruneMissingLocalPattern(sourceUriString)) {
                return null
            }

            val copiedUri =
                copyPatternToInternalIfNeeded(
                    sourceIsAppOwned = sourceIsAppOwned,
                    projectId = projectId,
                    sourceUri = sourceUri,
                    sanitizedName = sanitizedName,
                )
            val reusableUri = findReusablePatternUri(copiedUri, sanitizedName)
            if (reusableUri != null && copiedUri != null) {
                withContext(ioDispatcher) {
                    AppFileStorage.deleteIfAppOwned(context, copiedUri)
                }
            }
            val internalUri =
                resolvePatternAttachmentUri(
                    sourceUriString = sourceUriString,
                    copiedUriString = reusableUri ?: copiedUri,
                    isSourceAppOwned = sourceIsAppOwned,
                ) ?: return null

            return PatternAttachment(
                internalUri = internalUri,
                copiedUri = copiedUri,
                reusableUri = reusableUri,
            )
        }

        private suspend fun copyPatternToInternalIfNeeded(
            sourceIsAppOwned: Boolean,
            projectId: Long,
            sourceUri: Uri,
            sanitizedName: String,
        ): String? =
            if (sourceIsAppOwned) {
                null
            } else {
                // Kopioi PDF sisäiseen tallennustilaan — estää permission-ongelmat
                withContext(ioDispatcher) {
                    try {
                        patternDocumentStorage.copyPdfToInternal(
                            context = context,
                            projectId = projectId,
                            sourceUri = sourceUri,
                            fileName = sanitizedName,
                        )
                    } finally {
                        releasePersistedReadPermissionIfHeld(context.contentResolver, sourceUri)
                    }
                }
            }

        private fun releasePersistedReadPermissionIfHeld(
            contentResolver: ContentResolver,
            uri: Uri,
        ) {
            val hasPersistedReadPermission =
                contentResolver.persistedUriPermissions.any { permission ->
                    permission.uri == uri && permission.isReadPermission
                }
            if (!hasPersistedReadPermission) return

            runCatching {
                contentResolver.releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }

        private suspend fun findReusablePatternUri(
            copiedUri: String?,
            sanitizedName: String,
        ): String? =
            copiedUri?.let { copiedPatternUri ->
                savedPatternRepository.findReusableImportedPatternUrl(
                    candidatePatternUrl = copiedPatternUri,
                    name = sanitizedName,
                )
            }

        private suspend fun persistPatternAttachment(
            projectId: Long,
            patternName: String,
            attachment: PatternAttachment,
        ): ProjectDocumentMutationResult {
            val result =
                runCatching {
                    repository.attachPattern(
                        id = projectId,
                        patternUri = attachment.internalUri,
                        patternName = patternName,
                        currentPatternPage = 0,
                        patternRowMapping = null,
                    )
                }
            result.getOrNull()?.let { return it }
            val committed =
                withContext(NonCancellable) {
                    runCatching { repository.isPatternDocumentAttached(projectId, attachment.internalUri) }
                        .getOrDefault(false)
                }
            if (!committed) {
                attachment.copiedUri
                    ?.takeIf { attachment.reusableUri == null }
                    ?.let { failedUri ->
                        withContext(NonCancellable + ioDispatcher) {
                            AppFileStorage.deleteIfAppOwned(context, failedUri)
                        }
                    }
            }
            val failure = result.exceptionOrNull()
            if (failure is CancellationException) throw failure
            return ProjectDocumentMutationResult.PersistenceFailure
        }

        private data class PatternAttachment(
            val internalUri: String,
            val copiedUri: String?,
            val reusableUri: String?,
        )

        fun detachPattern() {
            val projectId = _uiState.value.projectId ?: return
            _uiState.update {
                it.copy(
                    linkedPattern = null,
                    patternUri = null,
                    patternName = null,
                    currentPatternPage = 0,
                    patternRowMapping = null,
                )
            }
            viewModelScope.launch {
                repository.detachPattern(projectId)
            }
        }

        fun updatePatternPage(
            page: Int,
            documentId: Long? = null,
        ) {
            val projectId = _uiState.value.projectId ?: return
            val sanitizedPage = page.coerceAtLeast(0)
            _uiState.update {
                it.copy(
                    currentPatternPage = sanitizedPage,
                    readingLineFollowCurrentRow = false,
                )
            }
            viewModelScope.launch {
                repository.updateCurrentPatternPage(projectId, sanitizedPage, documentId)
            }
        }

        fun updatePatternRowMapping(
            mapping: String?,
            documentId: Long? = null,
        ) {
            val projectId = _uiState.value.projectId ?: return
            _uiState.update { it.copy(patternRowMapping = mapping) }
            viewModelScope.launch {
                repository.updatePatternRowMapping(projectId, mapping, documentId)
            }
        }

        fun setReadingLineEnabled(
            enabled: Boolean,
            documentId: Long? = null,
        ) {
            val state = _uiState.value
            val projectId = state.projectId ?: return
            _uiState.update { it.copy(readingLineEnabled = enabled) }
            viewModelScope.launch {
                repository.updateReadingLineVisibility(projectId, enabled, documentId)
            }
        }

        fun updateReadingLineYFraction(yFraction: Float) {
            commitManualReadingLinePosition(yFraction)
        }

        fun commitManualReadingLinePosition(
            yFraction: Float,
            documentId: Long? = null,
        ) {
            val state = _uiState.value
            val projectId = state.projectId ?: return
            val sanitizedYFraction = sanitizeReadingLineYFraction(yFraction)
            _uiState.update {
                it.copy(
                    readingLineYFraction = sanitizedYFraction,
                    readingLineFollowCurrentRow = false,
                )
            }
            viewModelScope.launch {
                repository.commitManualReadingLinePosition(projectId, sanitizedYFraction, documentId)
            }
        }

        fun setReadingLineFollowCurrentRow(
            enabled: Boolean,
            documentId: Long? = null,
        ) {
            val projectId = _uiState.value.projectId ?: return
            _uiState.update { it.copy(readingLineFollowCurrentRow = enabled) }
            viewModelScope.launch {
                val resolution = repository.setReadingLineFollowCurrentRow(projectId, enabled, documentId)
                if (enabled && resolution != null) {
                    _viewerEvents.emit(
                        CounterViewerEvent.ReadingLineFollowingResumed(
                            calibrated =
                                resolution.kind == ReadingLineResolutionKind.EXACT_MARKER ||
                                    resolution.kind == ReadingLineResolutionKind.SAME_PAGE_INTERPOLATION,
                        ),
                    )
                }
            }
        }

        fun returnReadingLineToCurrentRow(documentId: Long? = null) {
            setReadingLineFollowCurrentRow(true, documentId)
        }

        fun setVerticalReadingGuideEnabled(
            enabled: Boolean,
            documentId: Long? = null,
        ) {
            val state = _uiState.value
            val projectId = state.projectId ?: return
            _uiState.update { it.copy(verticalReadingGuideEnabled = enabled) }
            viewModelScope.launch {
                repository.updateVerticalReadingGuide(
                    id = projectId,
                    enabled = enabled,
                    xFraction = state.verticalReadingGuideXFraction.takeIf { documentId == null },
                    documentId = documentId,
                )
            }
        }

        fun updateVerticalReadingGuideXFraction(
            xFraction: Float,
            documentId: Long? = null,
        ) {
            val state = _uiState.value
            val projectId = state.projectId ?: return
            val sanitizedXFraction = sanitizeReadingGuideFraction(xFraction)
            _uiState.update { it.copy(verticalReadingGuideXFraction = sanitizedXFraction) }
            viewModelScope.launch {
                repository.updateVerticalReadingGuide(
                    id = projectId,
                    enabled = state.verticalReadingGuideEnabled.takeIf { documentId == null },
                    xFraction = sanitizedXFraction,
                    documentId = documentId,
                )
            }
        }

        fun centerVerticalReadingGuide(documentId: Long? = null) {
            updateVerticalReadingGuideXFraction(DEFAULT_READING_GUIDE_FRACTION, documentId)
        }

        fun upsertPatternRowMarker(
            row: Int,
            page: Int,
            yPosition: Float,
            documentId: Long? = null,
            documentRowMapping: String? = null,
        ) {
            val markers = currentPatternRowMarkers(documentId, documentRowMapping)?.toMutableList() ?: return
            val sanitizedY = yPosition.coerceIn(0f, 1f)
            val index = markers.indexOfFirst { it.row == row && it.page == page }
            val marker = RowMarker(row = row, page = page, yPosition = sanitizedY)
            if (index >= 0) {
                markers[index] = marker
            } else {
                markers += marker
            }
            updatePatternRowMapping(serializeMapping(markers), documentId)
        }

        fun removePatternRowMarker(
            row: Int,
            page: Int,
            documentId: Long? = null,
            documentRowMapping: String? = null,
        ) {
            val currentMarkers = currentPatternRowMarkers(documentId, documentRowMapping) ?: return
            val markers =
                currentMarkers.filterNot { it.row == row && it.page == page }
            if (markers.size == currentMarkers.size) return
            updatePatternRowMapping(serializeMapping(markers), documentId)
        }

        fun removePatternRowMarkersForPage(
            page: Int,
            documentId: Long? = null,
            documentRowMapping: String? = null,
        ) {
            val currentMarkers = currentPatternRowMarkers(documentId, documentRowMapping) ?: return
            val markers =
                currentMarkers.filterNot { it.page == page }
            if (markers.size == currentMarkers.size) return
            updatePatternRowMapping(serializeMapping(markers), documentId)
        }

        fun mergePatternRowMarkers(
            markersToMerge: List<RowMarker>,
            documentId: Long? = null,
            documentRowMapping: String? = null,
        ) {
            if (markersToMerge.isEmpty()) return
            val markers = currentPatternRowMarkers(documentId, documentRowMapping)?.toMutableList() ?: return
            markersToMerge.forEach { marker ->
                val index = markers.indexOfFirst { it.row == marker.row && it.page == marker.page }
                if (index >= 0) {
                    markers[index] = marker
                } else {
                    markers += marker
                }
            }
            updatePatternRowMapping(serializeMapping(markers), documentId)
        }

        private fun currentPatternRowMarkers(
            documentId: Long?,
            documentRowMapping: String?,
        ): List<RowMarker>? {
            val state = _uiState.value
            if (documentId == null && state.patternUri == null) return null
            val mapping = if (documentId == null) state.patternRowMapping else documentRowMapping
            return parseMapping(mapping)
        }

        private fun persistCount(
            action: String,
            previousValue: Int,
            newValue: Int,
        ) {
            if (newValue == previousValue) return
            val state = _uiState.value
            val projectId = state.projectId ?: return
            viewModelScope.launch {
                inAppReviewManager.recordAction()
                repository.applyMainCounterChange(projectId, action.toMainCounterChange())
                syncWidget(projectId, state.projectName, newValue)
            }
        }

        private fun String.toMainCounterChange(): MainCounterChange =
            when (this) {
                "increment" -> MainCounterChange.Increment
                "decrement" -> MainCounterChange.Decrement
                "reset" -> MainCounterChange.Reset
                "undo" -> MainCounterChange.Undo
                else -> MainCounterChange.Increment
            }

        private suspend fun pruneHistory(projectId: Long) {
            val cutoff = System.currentTimeMillis() - 24L * 60L * 60L * 1_000L
            repository.deleteHistoryBefore(projectId, cutoff)
        }

        private suspend fun syncWidget(
            projectId: Long? = _uiState.value.projectId,
            projectName: String = _uiState.value.projectName,
            count: Int = _uiState.value.counter.count,
        ) {
            val resolvedProjectId = projectId ?: return
            val state = _uiState.value
            val widgetData =
                WidgetData(
                    projectName = projectName,
                    count = count,
                    projectId = resolvedProjectId,
                    targetRows = state.targetRows?.takeIf { it > 0 },
                    sectionName = state.sectionName?.takeIf { it.isNotBlank() },
                    currentStitch = state.currentStitch,
                    totalStitches = state.stitchCount?.takeIf { it > 0 },
                    stitchTrackingEnabled = state.stitchTrackingEnabled,
                )
            CounterWidgetState.syncAll(context, widgetData)
        }

        fun selectProjectById(id: Long) {
            selectProjectByIdForLaunch(id)
        }

        fun selectProjectByIdForLaunch(
            id: Long,
            onLoaded: (Boolean) -> Unit = {},
        ) {
            projectSelectionJob?.cancel()
            projectSelectionJob =
                viewModelScope.launch {
                    onLoaded(loadProjectForLaunch(id))
                }
        }

        private suspend fun loadProjectForLaunch(id: Long): Boolean {
            val project = repository.getProject(id) ?: return false
            openProject(project)
            return true
        }

        fun setSectionName(name: String?) {
            val projectId = _uiState.value.projectId ?: return
            _uiState.update { state ->
                if (state.projectId == projectId) state.copy(sectionName = name) else state
            }
            viewModelScope.launch {
                repository.updateProjectSectionName(projectId, name)
            }
        }

        fun setStitchCount(count: Int?) {
            if (count != null && count <= 0) return
            val state = _uiState.value
            val projectId = state.projectId ?: return
            val shouldDisableTracking = count == null
            val nextStitch = count?.let { total -> state.currentStitch.coerceIn(0, total) } ?: 0
            _uiState.update {
                it.copy(
                    stitchCount = count,
                    stitchTrackingEnabled = !shouldDisableTracking && it.stitchTrackingEnabled,
                    currentStitch = nextStitch,
                )
            }
            viewModelScope.launch {
                repository.updateProjectStitchCount(projectId, count)
            }
        }

        fun incrementStitch() {
            changeStitch(increment = true)
        }

        fun decrementStitch() {
            changeStitch(increment = false)
        }

        private fun changeStitch(increment: Boolean) {
            val state = _uiState.value
            val projectId = state.projectId ?: return
            val totalStitches = state.stitchCount ?: return
            if (!state.stitchTrackingEnabled || totalStitches <= 0 || state.currentStitch !in 0..totalStitches) return
            val delta = if (increment) 1L else -1L
            val nextStitch =
                (state.currentStitch.toLong() + delta)
                    .coerceIn(0L, totalStitches.toLong())
                    .toInt()
            if (nextStitch == state.currentStitch) return
            _uiState.update { it.copy(currentStitch = nextStitch) }
            viewModelScope.launch {
                repository.applyStitchChange(projectId, increment)
            }
        }

        fun setStitchTrackingEnabled(enabled: Boolean) {
            val state = _uiState.value
            val projectId = state.projectId ?: return
            val stitchCount = state.stitchCount
            if (enabled && (stitchCount == null || stitchCount <= 0)) return
            _uiState.update {
                it.copy(
                    stitchTrackingEnabled = enabled,
                    currentStitch = if (enabled) it.currentStitch.coerceIn(0, requireNotNull(stitchCount)) else 0,
                )
            }
            viewModelScope.launch {
                repository.updateStitchTrackingEnabled(projectId, enabled)
            }
        }

        private fun syncRepeatSectionCounters(
            mainRowCount: Int,
            counters: List<ProjectCounter>,
            persist: Boolean,
        ) {
            val syncedCounters =
                counters.map { counter ->
                    if (counter.counterType == ProjectCounterType.REPEAT_SECTION) {
                        RepeatSectionLogic.updatePosition(counter, mainRowCount)
                    } else {
                        counter
                    }
                }

            _uiState.update { it.copy(projectCounters = syncedCounters) }

            if (!persist) return

            syncedCounters
                .zip(counters)
                .filter { (updated, original) ->
                    updated.id == original.id &&
                        (updated.count != original.count || updated.currentRepeat != original.currentRepeat)
                }.forEach { (updated, _) ->
                    viewModelScope.launch {
                        projectCounterRepository.updateRepeatSectionState(
                            projectId = updated.projectId,
                            id = updated.id,
                        )
                    }
                }
        }

        override fun onCleared() {
            selectedProjectJob?.cancel()
            ProcessLifecycleOwner.get().lifecycle.removeObserver(lifecycleObserver)
        }

        private companion object {
            const val KEY_SELECTED_PROJECT_ID = "counter.selected_project_id"
            const val KEY_PROGRESS_PHOTO_CREATION_AUTHORIZED = "counter.progress_photo_creation_authorized"
        }
    }

internal inline fun runProjectYarnNoteSaveIfAllowed(
    noteId: Long,
    canUseYarnCards: Boolean,
    save: (Long) -> Unit,
): Boolean {
    if (!canUseYarnCards) return false
    save(noteId)
    return true
}
