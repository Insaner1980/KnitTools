package com.finnvek.knittools.ui.screens.counter

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.annotation.MainThread
import androidx.core.net.toUri
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.knittools.R
import com.finnvek.knittools.analytics.UsageAnalytics
import com.finnvek.knittools.analytics.UsageEvent
import com.finnvek.knittools.data.datastore.PreferencesManager
import com.finnvek.knittools.data.storage.AppFileStorage
import com.finnvek.knittools.data.storage.PatternDocumentStorage
import com.finnvek.knittools.data.storage.displayNameOrNull
import com.finnvek.knittools.di.ApplicationScope
import com.finnvek.knittools.di.IoDispatcher
import com.finnvek.knittools.domain.calculator.CounterLogic
import com.finnvek.knittools.domain.calculator.CounterState
import com.finnvek.knittools.domain.calculator.CounterValueFormatter
import com.finnvek.knittools.domain.calculator.MAX_MAIN_COUNTER_STEP_SIZE
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
import com.finnvek.knittools.domain.model.PatternDisplayNames
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
import com.finnvek.knittools.domain.tips.TipSignal
import com.finnvek.knittools.pro.InAppReviewManager
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.pro.ProState
import com.finnvek.knittools.pro.ProStatus
import com.finnvek.knittools.repository.ActiveSessionCompletionChoice
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.repository.OnboardingStateRepository
import com.finnvek.knittools.repository.ProgressPhotoRepository
import com.finnvek.knittools.repository.ProjectCompletionResult
import com.finnvek.knittools.repository.ProjectCounterMutationResult
import com.finnvek.knittools.repository.ProjectCounterRepository
import com.finnvek.knittools.repository.ProjectDeletionResult
import com.finnvek.knittools.repository.ProjectDocumentMutationResult
import com.finnvek.knittools.repository.ProjectDocumentRepository
import com.finnvek.knittools.repository.ProjectNotesSaveResult
import com.finnvek.knittools.repository.ProjectReactivationResult
import com.finnvek.knittools.repository.ProjectYarnNoteEditResult
import com.finnvek.knittools.repository.ProjectYarnNoteRepository
import com.finnvek.knittools.repository.RecoveryResolutionResult
import com.finnvek.knittools.repository.ReminderMutationResult
import com.finnvek.knittools.repository.ReminderRepository
import com.finnvek.knittools.repository.SavedPatternMetadataMutationResult
import com.finnvek.knittools.repository.SavedPatternRepository
import com.finnvek.knittools.repository.StartSessionResult
import com.finnvek.knittools.repository.StopSessionResult
import com.finnvek.knittools.repository.YarnCardRepository
import com.finnvek.knittools.ui.screens.project.ProjectCompletionCelebration
import com.finnvek.knittools.ui.screens.project.ProjectSetupVisit
import com.finnvek.knittools.ui.tips.TipSession
import com.finnvek.knittools.widget.CounterWidgetState
import com.finnvek.knittools.widget.WidgetData
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject

private data class PendingProjectReactivation(
    val projectId: Long,
    val completedAt: Long?,
)

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
    val completedAt: Long? = null,
    val reactivationPromptCount: Int? = null,
    val hapticFeedback: Boolean = true,
    val keepScreenAwake: Boolean = false,
    val isPro: Boolean = false,
    val proStatus: ProStatus = ProStatus.TRIAL_NOT_STARTED,
    val proState: ProState = ProState(),
    val proAccessReady: Boolean = false,
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
    val preferencesLoaded: Boolean = false,
    val proStateLoaded: Boolean = false,
    val activeSessionLoaded: Boolean = false,
    val initialSessionRefreshFinished: Boolean = false,
    val remindersLoadedProjectId: Long? = null,
    val countersLoadedProjectId: Long? = null,
    val documentsLoadedProjectId: Long? = null,
    val isRestoringProject: Boolean = false,
    val sectionName: String? = null,
    val stitchCount: Int? = null,
    val stitchTrackingEnabled: Boolean = false,
    val currentStitch: Int = 0,
    val linkedYarns: List<Pair<Long, String>> = emptyList(),
    val projectYarnNotes: List<ProjectYarnNote> = emptyList(),
    val totalSessionMinutes: Int = 0,
    val hasSessions: Boolean = false,
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
    val startupReady: Boolean
        get() =
            projectId != null &&
                projectsLoaded &&
                !isRestoringProject &&
                preferencesLoaded &&
                proStateLoaded &&
                activeSessionLoaded &&
                initialSessionRefreshFinished &&
                remindersLoadedProjectId == projectId &&
                countersLoadedProjectId == projectId &&
                documentsLoadedProjectId == projectId

    val primaryDocument: ProjectDocument?
        get() = primaryProjectDocument(projectDocuments)
}

private fun primaryProjectDocument(documents: List<ProjectDocument>): ProjectDocument? =
    documents.firstOrNull(ProjectDocument::isPrimary)
        ?: documents.minWithOrNull(compareBy(ProjectDocument::sortOrder, ProjectDocument::id))

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

data class MainCounterStepFeedback(
    val projectId: Long,
    val delta: Int,
)

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

private class PendingNotesSave(
    val selectionVersion: Long,
    var baseNotes: String,
) {
    var latestRequest: Any? = null
}

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
        private val tipSession: TipSession,
        private val onboardingStateRepository: OnboardingStateRepository,
        private val analytics: UsageAnalytics = UsageAnalytics.NONE,
    ) : ViewModel() {
        private val _uiState =
            MutableStateFlow(
                CounterUiState(
                    projectName = context.getString(R.string.default_project_name),
                ),
            )
        val uiState: StateFlow<CounterUiState> = _uiState.asStateFlow()
        val onboardingState = onboardingStateRepository.state
        private val _projectSetupCelebration = MutableStateFlow<ProjectSetupVisit?>(null)
        val projectSetupCelebration: StateFlow<ProjectSetupVisit?> = _projectSetupCelebration.asStateFlow()
        private val _patternImportInProgress = MutableStateFlow(false)
        val patternImportInProgress: StateFlow<Boolean> = _patternImportInProgress.asStateFlow()
        private var patternImportsInFlight = 0
        private val _viewerEvents = MutableSharedFlow<CounterViewerEvent>(extraBufferCapacity = 1)
        val viewerEvents: SharedFlow<CounterViewerEvent> = _viewerEvents.asSharedFlow()
        private val _stepFeedback = MutableSharedFlow<MainCounterStepFeedback>(extraBufferCapacity = 1)
        val stepFeedback: SharedFlow<MainCounterStepFeedback> = _stepFeedback.asSharedFlow()
        private val stepSizeSaveMutex = Mutex()
        private var projectSelectionVersion = 0L
        private var pendingNotesBase: PendingNotesSave? = null
        private val notesSaveMutex = Mutex()
        private val projectClosedEventChannel = Channel<Long>(Channel.CONFLATED)
        val projectClosedEvents = projectClosedEventChannel.receiveAsFlow()
        private val _pendingCompletion = MutableStateFlow<ProjectCompletionCelebration?>(null)
        val pendingCompletion: StateFlow<ProjectCompletionCelebration?> = _pendingCompletion.asStateFlow()

        fun consumeCompletion(eventId: Long) {
            _pendingCompletion.update { if (it?.snapshot?.eventId == eventId) null else it }
        }

        @MainThread
        fun consumeProjectClosedEvent(
            selectionVersion: Long,
            onClose: () -> Unit,
        ) {
            if (selectionVersion == projectSelectionVersion) onClose()
        }

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
        private var sessionPresentationJob: Job? = null
        private var sessionStopSummaryJob: Job? = null
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
        private var projectCompletionVersion = 0L
        private var hasProjectCompletionRetry = false
        private var pendingReactivation: PendingProjectReactivation?
            get() =
                savedStateHandle.get<Long>("pending_reactivation_project_id")?.let { projectId ->
                    PendingProjectReactivation(projectId, savedStateHandle["pending_reactivation_completed_at"])
                }
            set(value) {
                savedStateHandle["pending_reactivation_completed_at"] = value?.completedAt
                savedStateHandle["pending_reactivation_project_id"] = value?.projectId
            }
        private var reactivationJob: Job? = null
        private var reactivationRetryRequested = false
        private val reactivationErrorsChannel = Channel<Unit>(Channel.BUFFERED)
        val reactivationErrors = reactivationErrorsChannel.receiveAsFlow()

        private val photoErrorsChannel = Channel<Unit>(Channel.BUFFERED)
        val photoErrors = photoErrorsChannel.receiveAsFlow()

        private val projectContentErrorsChannel = Channel<Unit>(Channel.BUFFERED)
        val projectContentErrors = projectContentErrorsChannel.receiveAsFlow()
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
            restoreSelectedProject()
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
                            preferencesLoaded = true,
                            keepScreenAwake = prefs.keepScreenAwake,
                        )
                    }
                }
            }
        }

        private fun observeProState() {
            viewModelScope.launch {
                combine(
                    proManager.proState,
                    proManager.initialControlsReady,
                    proManager.initialStateReady,
                ) { state, controlsReady, accessReady ->
                    Triple(state, controlsReady, accessReady)
                }.collect { (proState, controlsReady, accessReady) ->
                    _uiState.update {
                        val canCreateNotes = proState.hasFeature(ProFeature.NOTES)
                        val canCreateSecondaryCounter = proState.hasFeature(ProFeature.SECONDARY_COUNTER)
                        it.copy(
                            isPro = proState.isPro,
                            proStateLoaded = controlsReady,
                            proState = proState,
                            proAccessReady = accessReady,
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
                    launchPendingReactivation(checkEligibility = true)
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
                    if (projectSelectionJob == null) restorePendingReactivation()
                    if (list.isEmpty()) {
                        launchPendingReactivation(checkEligibility = true)
                    }
                    if (selectedProjectJob != null || projectSelectionJob != null) return@collect
                    if (list.isEmpty()) {
                        clearSelectedProject()
                        return@collect
                    }
                    val currentId = _uiState.value.projectId ?: savedStateHandle.get<Long>(KEY_SELECTED_PROJECT_ID)
                    val targetProject = currentId?.let { id -> list.find { it.id == id } } ?: list.first()
                    openProject(targetProject)
                }
            }
        }

        private fun restoreSelectedProject() {
            val projectId = savedStateHandle.get<Long>(KEY_SELECTED_PROJECT_ID) ?: return
            if (pendingReactivation != null) return
            val selectionVersion = projectSelectionVersion
            _uiState.update { it.copy(isRestoringProject = true) }
            projectSelectionJob =
                viewModelScope.launch {
                    val project = repository.observeProject(projectId).first()
                    currentCoroutineContext().ensureActive()
                    if (project == null) {
                        closeSelectedProject(selectionVersion)
                    } else {
                        openProject(project, restoring = true)
                    }
                }
        }

        fun selectProject(project: CounterProject) {
            projectSelectionVersion++
            if (project.id != (_uiState.value.projectId ?: pendingReactivation?.projectId)) dismissPendingReactivation()
            projectSelectionJob?.cancel()
            projectSelectionJob =
                viewModelScope.launch {
                    openProject(project)
                }
        }

        private suspend fun restorePendingReactivation() {
            if (_uiState.value.projectId != null) return
            val request = pendingReactivation ?: return
            if (savedStateHandle.get<Long>(KEY_SELECTED_PROJECT_ID) != request.projectId) {
                clearPendingReactivation()
                return
            }
            val project = repository.getProject(request.projectId)
            if (pendingReactivation != request || _uiState.value.projectId != null) return
            if (project == null || !project.isCompleted || project.completedAt != request.completedAt) {
                clearPendingReactivation()
                return
            }
            openProject(project)
            launchPendingReactivation(checkEligibility = true)
        }

        private fun observeSelectedProject(projectId: Long) {
            val selectionVersion = projectSelectionVersion
            selectedProjectJob?.cancel()
            selectedProjectJob =
                viewModelScope.launch {
                    launch {
                        repository.observeHasSessionsForProject(projectId).collect { hasSessions ->
                            _uiState.update { state ->
                                if (state.projectId == projectId && selectionVersion == projectSelectionVersion) {
                                    state.copy(hasSessions = hasSessions)
                                } else {
                                    state
                                }
                            }
                        }
                    }
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
                                selectionVersion = selectionVersion,
                                project = project,
                                linkedPattern = linkedPattern,
                                previousObservedProject = previousObservedProject,
                            )
                    }
                }
        }

        private suspend fun handleSelectedProjectObservation(
            projectId: Long,
            selectionVersion: Long,
            project: CounterProject?,
            linkedPattern: SavedPattern?,
            previousObservedProject: CounterProject?,
        ): CounterProject? {
            if (project == null) {
                handleMissingSelectedProject(projectId, selectionVersion)
                return previousObservedProject
            }

            pendingReactivation?.let { pending ->
                if (pending.projectId != project.id ||
                    !project.isCompleted ||
                    pending.completedAt != project.completedAt
                ) {
                    dismissPendingReactivation()
                }
            }
            val previousState = _uiState.value
            val countChanged = previousState.counter.count != project.count
            _uiState.update { state ->
                val observed = state.withObservedProject(project).copy(linkedPattern = linkedPattern)
                if (pendingNotesBase?.selectionVersion == selectionVersion) {
                    observed.copy(notes = state.notes)
                } else {
                    observed
                }
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

        private fun handleMissingSelectedProject(
            projectId: Long,
            selectionVersion: Long,
        ) {
            if (selectionVersion != projectSelectionVersion) return
            val wasSelected = _uiState.value.projectId == projectId
            _uiState.update { state ->
                if (state.projectId == projectId) state.copy(projectId = null) else state
            }
            if (wasSelected) {
                closeSelectedProject(selectionVersion)
            }
        }

        fun linkYarnCard(cardId: Long) {
            val projectId = _uiState.value.projectId ?: return
            updateYarnCardLink(cardId, projectId)
        }

        fun unlinkYarnCard(cardId: Long) {
            updateYarnCardLink(cardId, null)
        }

        private fun updateYarnCardLink(
            cardId: Long,
            projectId: Long?,
        ) {
            launchProjectContentMutation { yarnCardRepository.updateLinkedProjectId(cardId, projectId) }
        }

        private fun launchProjectContentMutation(operation: suspend () -> Boolean) {
            viewModelScope.launch {
                try {
                    if (!operation()) {
                        projectContentErrorsChannel.send(Unit)
                    }
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Exception) {
                    projectContentErrorsChannel.send(Unit)
                }
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
            launchProjectContentMutation {
                projectYarnNoteRepository.save(
                    ProjectYarnNote(
                        projectId = projectId,
                        name = name,
                        description = description,
                        quantity = quantity,
                        notes = notes,
                    ),
                ) > 0L
            }
        }

        fun editProjectYarnNote(
            original: ProjectYarnNote,
            draft: ProjectYarnNote,
        ): Deferred<ProjectYarnNoteEditResult> =
            viewModelScope.async {
                if (_uiState.value.projectId != original.projectId) {
                    return@async ProjectYarnNoteEditResult.Missing
                }
                try {
                    projectYarnNoteRepository.edit(original, draft)
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Exception) {
                    ProjectYarnNoteEditResult.Failed
                }
            }

        fun deleteProjectYarnNote(noteId: Long) {
            launchProjectContentMutation {
                projectYarnNoteRepository.delete(noteId)
                true
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
            launchProjectContentMutation {
                projectYarnNoteRepository.saveToMyYarn(noteId) != null
            }
        }

        private suspend fun loadTotalSessionMinutes(projectId: Long) {
            val minutes =
                try {
                    repository.getTotalMinutesForProject(projectId)
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Exception) {
                    // A failed display refresh must not interrupt an already committed session action.
                    return
                }
            _uiState.update { state ->
                if (state.projectId == projectId) {
                    state.copy(totalSessionMinutes = minutes)
                } else {
                    state
                }
            }
        }

        private suspend fun openProject(
            project: CounterProject,
            restoring: Boolean = false,
        ) {
            projectSelectionVersion++
            if (_uiState.value.projectId != project.id) invalidateProjectCompletion()
            if (pendingReactivation?.projectId != project.id) dismissPendingReactivation()
            linkedYarnIdsCache = project.yarnCardIds

            saveSelectedProject(project.id)

            _uiState.update { it.withStartedProject(project) }
            observeSelectedProject(project.id)
            observeReminders(project.id)
            observeProjectCounters(project.id, persistInitialSync = !restoring)
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
                        val primary = primaryProjectDocument(documents)
                        _uiState.update { state ->
                            state.copy(
                                projectDocuments = documents,
                                documentsLoadedProjectId = projectId,
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
            val selectionVersion = projectSelectionVersion
            viewModelScope.launch {
                val result = mutation(projectId)
                if (selectionVersion == projectSelectionVersion && _uiState.value.projectId == projectId) {
                    onResult(result)
                }
            }
        }

        private fun clearSelectedProject() {
            projectSelectionVersion++
            invalidateProjectCompletion()
            dismissPendingReactivation()
            savedStateHandle.remove<Long>(KEY_SELECTED_PROJECT_ID)
            _uiState.update { it.copy(isRestoringProject = false) }
        }

        private fun closeSelectedProject(selectionVersion: Long) {
            if (selectionVersion != projectSelectionVersion) return
            clearSelectedProject()
            projectClosedEventChannel.trySend(projectSelectionVersion)
        }

        private fun observeActiveSession() {
            viewModelScope.launch {
                repository.observeActiveSession().collectLatest { session ->
                    val sessionSeconds = session?.let { repository.activeSessionDurationSeconds(it) } ?: 0L
                    _uiState.update { state ->
                        state.copy(
                            activeSession = session,
                            activeSessionLoaded = true,
                            sessionSeconds = sessionSeconds,
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
                val expectedSession = _uiState.value.activeSession
                val session =
                    try {
                        repository.refreshActiveSession()
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        return@launch
                    } finally {
                        _uiState.update { it.copy(initialSessionRefreshFinished = true) }
                    }
                val sessionSeconds = session?.let { repository.activeSessionDurationSeconds(it) } ?: 0L
                _uiState.update {
                    if (it.activeSession != expectedSession) return@update it
                    it.copy(
                        activeSession = session,
                        sessionSeconds = sessionSeconds,
                        showSessionRecoveryPrompt =
                            session?.needsRecoveryReview == true && !session.recoveryPromptShown,
                    )
                }
            }
        }

        fun refreshSessionPresentationTime() {
            if (sessionPresentationJob?.isActive == true) return
            val session = _uiState.value.activeSession ?: return
            sessionPresentationJob =
                viewModelScope.launch {
                    if (!session.needsRecoveryReview && repository.activeSessionNeedsRecovery(session)) {
                        if (_uiState.value.activeSession == session) refreshActiveSession()
                        return@launch
                    }
                    val sessionSeconds = repository.activeSessionDurationSeconds(session)
                    _uiState.update { state ->
                        if (state.activeSession == session) {
                            state.copy(sessionSeconds = sessionSeconds)
                        } else {
                            state
                        }
                    }
                }
        }

        fun startWorkSession() {
            analytics.track(UsageEvent.WORK_SESSION_START_REQUESTED)
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
                    StartSessionResult.HistoryLimitReached -> showWorkSessionError(R.string.work_session_history_full)
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
                    result == StartSessionResult.HistoryLimitReached ->
                        showWorkSessionError(R.string.work_session_history_full)
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
            sessionStopSummaryJob?.cancel()
            val selectionVersion = projectSelectionVersion
            sessionStopSummaryJob =
                viewModelScope.launch {
                    while (isActive && selectionVersion == projectSelectionVersion) {
                        val current = _uiState.value.activeSession ?: return@launch
                        if (current.sessionToken != active.sessionToken) return@launch
                        if (current.needsRecoveryReview) {
                            _uiState.update { it.copy(showSessionRecoveryPrompt = true) }
                            return@launch
                        }
                        val durationSeconds = repository.activeSessionDurationSeconds(current)
                        if (selectionVersion != projectSelectionVersion) return@launch
                        val state = _uiState.value
                        if (state.activeSession != current) continue
                        if (
                            _uiState.compareAndSet(
                                state,
                                state.copy(
                                    sessionStopSummary =
                                        SessionStopSummary(
                                            sessionToken = current.sessionToken,
                                            projectName = state.projectName,
                                            durationSeconds = durationSeconds,
                                            rowsWorked = current.trustedRowsWorked,
                                            endRow = current.trustedLastObservedRow,
                                        ),
                                ),
                            )
                        ) {
                            return@launch
                        }
                    }
                }
        }

        fun saveStoppedWorkSession() {
            val summary = _uiState.value.sessionStopSummary ?: return
            val active = _uiState.value.activeSession ?: return
            val projectId = active.projectId
            launchWorkSessionAction {
                when (
                    val result =
                        repository.stopSession(
                            sessionToken = summary.sessionToken,
                            reviewedDurationSeconds = summary.durationSeconds,
                            reviewedRowsWorked = summary.rowsWorked,
                            reviewedEndRow = summary.endRow,
                        )
                ) {
                    is StopSessionResult.Saved -> {
                        if (result.completedSessionId != null) {
                            tipSession.onSignal(TipSignal.SessionSaved(result.wasFirstCompletedSession))
                        }
                        analytics.track(UsageEvent.WORK_SESSION_SAVED)
                        loadTotalSessionMinutes(projectId)
                        clearSessionStopSummary(summary.sessionToken)
                    }
                    is StopSessionResult.NeedsRecoveryReview ->
                        handleStopRecoveryReview(summary, active, result.session)
                    StopSessionResult.NoActiveSession -> clearSessionStopSummary(summary.sessionToken)
                    StopSessionResult.StaleAction ->
                        _uiState.update {
                            if (it.sessionStopSummary === summary) it.copy(sessionStopSummary = null) else it
                        }
                    StopSessionResult.Discarded -> Unit
                    StopSessionResult.PersistenceFailure ->
                        showWorkSessionError(R.string.work_session_could_not_save, ::saveStoppedWorkSession)
                    StopSessionResult.HistoryLimitReached -> showWorkSessionError(R.string.work_session_history_full)
                }
            }
        }

        private fun clearSessionStopSummary(sessionToken: String) {
            _uiState.update {
                if (it.sessionStopSummary?.sessionToken == sessionToken) it.copy(sessionStopSummary = null) else it
            }
        }

        fun discardStoppedWorkSession() {
            val summary = _uiState.value.sessionStopSummary ?: return
            launchWorkSessionAction {
                when (repository.discardActiveSession(summary.sessionToken)) {
                    StopSessionResult.Discarded,
                    StopSessionResult.NoActiveSession,
                    StopSessionResult.StaleAction,
                    ->
                        _uiState.update {
                            if (it.sessionStopSummary?.sessionToken == summary.sessionToken) {
                                it.copy(sessionStopSummary = null)
                            } else {
                                it
                            }
                        }
                    is StopSessionResult.NeedsRecoveryReview,
                    is StopSessionResult.Saved,
                    -> Unit
                    StopSessionResult.PersistenceFailure ->
                        showWorkSessionError(R.string.work_session_could_not_discard, ::discardStoppedWorkSession)
                    StopSessionResult.HistoryLimitReached -> Unit
                }
            }
        }

        fun cancelSessionStop() {
            sessionStopSummaryJob?.cancel()
            _uiState.update { it.copy(sessionStopSummary = null) }
        }

        private suspend fun handleStopRecoveryReview(
            summary: SessionStopSummary,
            expectedSession: ActiveWorkSession,
            recovery: ActiveWorkSession,
        ) {
            if (_uiState.value.sessionStopSummary !== summary) return
            val recoverySeconds = repository.activeSessionDurationSeconds(recovery)
            _uiState.update { state ->
                if (state.sessionStopSummary !== summary) return@update state
                val currentSession = if (state.activeSession == expectedSession) recovery else state.activeSession
                state.copy(
                    activeSession = currentSession,
                    sessionSeconds =
                        if (state.activeSession == expectedSession) recoverySeconds else state.sessionSeconds,
                    sessionStopSummary = null,
                    showSessionRecoveryPrompt =
                        state.showSessionRecoveryPrompt ||
                            currentSession?.sessionToken == recovery.sessionToken &&
                            currentSession.recoveryIntervalToken == recovery.recoveryIntervalToken,
                )
            }
        }

        fun dismissRecoveryPrompt() {
            val active = _uiState.value.activeSession ?: return
            val intervalToken = active.recoveryIntervalToken ?: return
            dismissRecoveryPrompt(active, intervalToken)
        }

        private fun dismissRecoveryPrompt(
            active: ActiveWorkSession,
            intervalToken: String,
        ) {
            viewModelScope.launch {
                try {
                    if (!repository.markRecoveryPromptShown(active.sessionToken, intervalToken)) return@launch
                    _uiState.update {
                        if (
                            it.activeSession?.sessionToken == active.sessionToken &&
                            it.activeSession.recoveryIntervalToken == intervalToken
                        ) {
                            it.copy(showSessionRecoveryPrompt = false)
                        } else {
                            it
                        }
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    val current = _uiState.value.activeSession
                    if (current?.sessionToken == active.sessionToken &&
                        current.recoveryIntervalToken == intervalToken
                    ) {
                        showWorkSessionError(R.string.work_session_could_not_save) {
                            dismissRecoveryPrompt(active, intervalToken)
                        }
                    }
                }
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
            addRecoveryInterval(active, intervalToken, durationSeconds)
        }

        private fun addRecoveryInterval(
            active: ActiveWorkSession,
            intervalToken: String,
            durationSeconds: Long,
        ) {
            launchWorkSessionAction {
                when (
                    repository.addRecoveryInterval(
                        sessionToken = active.sessionToken,
                        recoveryIntervalToken = intervalToken,
                        durationSeconds = durationSeconds,
                    )
                ) {
                    is RecoveryResolutionResult.Continued ->
                        _uiState.update {
                            if (
                                it.activeSession == null ||
                                it.activeSession.sessionToken == active.sessionToken &&
                                it.activeSession.recoveryIntervalToken == intervalToken
                            ) {
                                it.copy(showSessionRecoveryPrompt = false)
                            } else {
                                it
                            }
                        }
                    RecoveryResolutionResult.InvalidDuration ->
                        showWorkSessionError(R.string.work_session_invalid_duration)
                    RecoveryResolutionResult.StaleAction ->
                        showWorkSessionError(R.string.work_session_recovery_already_handled)
                    RecoveryResolutionResult.PersistenceFailure ->
                        showWorkSessionError(R.string.work_session_could_not_save) {
                            addRecoveryInterval(active, intervalToken, durationSeconds)
                        }
                    RecoveryResolutionResult.HistoryLimitReached -> Unit
                    is RecoveryResolutionResult.DiscardedAndStopped,
                    is RecoveryResolutionResult.EditedAndStopped,
                    -> Unit
                }
            }
        }

        fun discardRecoveryInterval() {
            val active = _uiState.value.activeSession ?: return
            val intervalToken = active.recoveryIntervalToken ?: return
            discardRecoveryInterval(active, intervalToken)
        }

        private fun discardRecoveryInterval(
            active: ActiveWorkSession,
            intervalToken: String,
        ) {
            launchWorkSessionAction {
                when (
                    repository.discardRecoveryInterval(
                        active.sessionToken,
                        intervalToken,
                    )
                ) {
                    is RecoveryResolutionResult.DiscardedAndStopped -> {
                        finishRecoveryResolution(active, intervalToken)
                    }
                    RecoveryResolutionResult.StaleAction ->
                        showWorkSessionError(R.string.work_session_recovery_already_handled)
                    RecoveryResolutionResult.PersistenceFailure ->
                        showWorkSessionError(R.string.work_session_could_not_discard) {
                            discardRecoveryInterval(active, intervalToken)
                        }
                    RecoveryResolutionResult.HistoryLimitReached ->
                        showWorkSessionError(R.string.work_session_history_full)
                    is RecoveryResolutionResult.Continued,
                    is RecoveryResolutionResult.EditedAndStopped,
                    RecoveryResolutionResult.InvalidDuration,
                    -> Unit
                }
            }
        }

        private suspend fun finishRecoveryResolution(
            active: ActiveWorkSession,
            intervalToken: String,
        ) {
            loadTotalSessionMinutes(active.projectId)
            _uiState.update {
                if (
                    it.activeSession == null ||
                    it.activeSession.sessionToken == active.sessionToken &&
                    it.activeSession.recoveryIntervalToken == intervalToken
                ) {
                    it.copy(showSessionRecoveryPrompt = false)
                } else {
                    it
                }
            }
        }

        fun editRecoveryDurationAndStop(totalDurationSeconds: Long) {
            val active = _uiState.value.activeSession ?: return
            val intervalToken = active.recoveryIntervalToken ?: return
            editRecoveryDurationAndStop(active, intervalToken, totalDurationSeconds)
        }

        private fun editRecoveryDurationAndStop(
            active: ActiveWorkSession,
            intervalToken: String,
            totalDurationSeconds: Long,
        ) {
            launchWorkSessionAction {
                when (
                    repository.editRecoveryDurationAndStop(
                        sessionToken = active.sessionToken,
                        recoveryIntervalToken = intervalToken,
                        totalDurationSeconds = totalDurationSeconds,
                    )
                ) {
                    is RecoveryResolutionResult.EditedAndStopped -> {
                        finishRecoveryResolution(active, intervalToken)
                    }
                    RecoveryResolutionResult.InvalidDuration ->
                        showWorkSessionError(R.string.work_session_invalid_duration)
                    RecoveryResolutionResult.StaleAction ->
                        showWorkSessionError(R.string.work_session_recovery_already_handled)
                    RecoveryResolutionResult.PersistenceFailure ->
                        showWorkSessionError(R.string.work_session_could_not_save) {
                            editRecoveryDurationAndStop(active, intervalToken, totalDurationSeconds)
                        }
                    RecoveryResolutionResult.HistoryLimitReached ->
                        showWorkSessionError(R.string.work_session_history_full)
                    is RecoveryResolutionResult.Continued,
                    is RecoveryResolutionResult.DiscardedAndStopped,
                    -> Unit
                }
            }
        }

        fun completeProject() {
            val projectId = _uiState.value.projectId ?: return
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
                expectedSession = state.pendingProjectCompletionSession,
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
            expectedSession: ActiveWorkSession? = null,
        ) {
            if (projectCompletionJob?.isActive == true) return
            invalidateProjectCompletion()
            val requestVersion = projectCompletionVersion
            val selectionVersion = projectSelectionVersion
            projectCompletionJob =
                viewModelScope.launch {
                    try {
                        handleProjectCompletionResult(
                            projectId,
                            requestVersion,
                            selectionVersion,
                            repository.completeProjectWithSessionChoice(
                                projectId,
                                choice,
                                expectedSession = expectedSession,
                            ),
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
            requestVersion: Long,
            selectionVersion: Long,
            result: ProjectCompletionResult,
        ) {
            if (requestVersion != projectCompletionVersion || _uiState.value.projectId != projectId) return
            when (result) {
                ProjectCompletionResult.Completed,
                is ProjectCompletionResult.CompletedNow,
                -> {
                    if (selectionVersion != projectSelectionVersion) return
                    if (result is ProjectCompletionResult.CompletedNow) {
                        _pendingCompletion.value =
                            ProjectCompletionCelebration(
                                result.snapshot,
                                _uiState.value.latestPhotos
                                    .firstOrNull()
                                    ?.photoUri,
                            )
                    }
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
                    closeSelectedProject(selectionVersion)
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
                ProjectCompletionResult.HistoryLimitReached -> showWorkSessionError(R.string.work_session_history_full)
                ProjectCompletionResult.PersistenceFailure -> {
                    showWorkSessionError(R.string.work_session_could_not_save) {
                        if (_uiState.value.projectId == projectId) {
                            launchProjectCompletion(projectId, choice = null)
                        }
                    }
                    hasProjectCompletionRetry = true
                }
            }
        }

        private fun invalidateProjectCompletion() {
            projectCompletionVersion++
            if (hasProjectCompletionRetry) dismissWorkSessionError()
            cancelProjectCompletionSessionChoice()
        }

        fun setTargetRows(target: Int?) {
            val projectId = _uiState.value.projectId ?: return
            val selectionVersion = projectSelectionVersion
            val validated = target?.takeIf { it > 0 }
            _uiState.update { it.copy(targetRows = validated) }
            launchProjectContentMutation {
                val saved =
                    tryProjectWrite {
                        repository.setTargetRows(projectId, validated)
                        true
                    }
                val persisted = repository.getProject(projectId) ?: return@launchProjectContentMutation false
                if (selectionVersion == projectSelectionVersion && _uiState.value.projectId == projectId) {
                    _uiState.update { it.copy(targetRows = persisted.targetRows) }
                    if (saved) syncWidget()
                }
                saved
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
            launchProjectDeletion(active.projectId, discardActiveSession = true, expectedSession = active)
        }

        private fun launchProjectDeletion(
            projectId: Long,
            discardActiveSession: Boolean,
            expectedSession: ActiveWorkSession? = null,
        ) {
            if (projectDeletionJob?.isActive == true) return
            val selectionVersion = projectSelectionVersion
            projectDeletionJob =
                viewModelScope.launch {
                    try {
                        handleProjectDeletionResult(
                            id = projectId,
                            selectionVersion = selectionVersion,
                            result =
                                repository.deleteProjectResolvingActiveSession(
                                    id = projectId,
                                    discardActiveSession = discardActiveSession,
                                    expectedSession = expectedSession,
                                ),
                        )
                    } finally {
                        projectDeletionJob = null
                    }
                }
        }

        fun reactivateProject(
            projectId: Long? = _uiState.value.projectId,
            expectedCompletedAt: Long? = _uiState.value.completedAt,
        ) {
            val state = _uiState.value
            if (projectId == null || projectId != state.projectId || expectedCompletedAt != state.completedAt) return
            if (!state.isCompleted || reactivationJob?.isActive == true || pendingReactivation != null) return
            pendingReactivation = PendingProjectReactivation(projectId, expectedCompletedAt)
            retryPendingReactivation()
        }

        fun dismissPendingReactivation() {
            clearPendingReactivation()
            reactivationJob?.cancel()
        }

        fun retryPendingReactivation() {
            launchPendingReactivation(checkEligibility = false)
        }

        private fun launchPendingReactivation(checkEligibility: Boolean) {
            val request = pendingReactivation ?: return
            val state = _uiState.value
            if (state.projectId == null) return
            if (state.projectId != request.projectId ||
                !state.isCompleted ||
                state.completedAt != request.completedAt
            ) {
                dismissPendingReactivation()
                return
            }
            if (reactivationJob?.isActive == true) {
                reactivationRetryRequested = true
                return
            }
            reactivationJob =
                viewModelScope.launch(start = CoroutineStart.LAZY) {
                    var limited = false
                    try {
                        if (checkEligibility) {
                            val activeCount = repository.getActiveProjectCount()
                            if (activeCount >= 1 && !proManager.hasFeature(ProFeature.UNLIMITED_PROJECTS)) {
                                _uiState.update { it.copy(reactivationPromptCount = activeCount) }
                                return@launch
                            }
                        }
                        limited = performPendingReactivation(request)
                    } finally {
                        reactivationJob = null
                        val reconsider = reactivationRetryRequested || (limited && !checkEligibility)
                        reactivationRetryRequested = false
                        if (pendingReactivation == request && reconsider) {
                            launchPendingReactivation(checkEligibility = true)
                        }
                    }
                }
            reactivationJob?.start()
        }

        private suspend fun performPendingReactivation(request: PendingProjectReactivation): Boolean {
            val result = repository.reactivateProject(request.projectId, request.completedAt)
            if (pendingReactivation != request) return false
            when (result) {
                ProjectReactivationResult.LimitReached -> {
                    val activeCount = repository.getActiveProjectCount()
                    _uiState.update { it.copy(reactivationPromptCount = activeCount) }
                    return true
                }
                ProjectReactivationResult.Reactivated,
                ProjectReactivationResult.ProjectUnavailable,
                -> clearPendingReactivation()
                ProjectReactivationResult.PersistenceFailure -> {
                    clearPendingReactivation()
                    reactivationErrorsChannel.send(Unit)
                }
            }
            return false
        }

        private fun clearPendingReactivation() {
            pendingReactivation = null
            reactivationRetryRequested = false
            _uiState.update { it.copy(reactivationPromptCount = null) }
        }

        fun cancelProjectDeletionSessionChoice() {
            _uiState.update { it.copy(pendingProjectDeletionSession = null) }
        }

        private fun handleProjectDeletionResult(
            id: Long,
            selectionVersion: Long,
            result: ProjectDeletionResult,
        ) {
            if (selectionVersion != projectSelectionVersion) return
            when (result) {
                ProjectDeletionResult.Deleted -> {
                    if (_uiState.value.projectId == id) {
                        _uiState.update {
                            it.copy(
                                projectId = null,
                                pendingProjectDeletionSession = null,
                            )
                        }
                        closeSelectedProject(selectionVersion)
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
            invalidateProjectCompletion()
            pendingWorkSessionRetry = retry
            _uiState.update {
                it.copy(
                    workSessionErrorRes = messageRes,
                    workSessionErrorCanRetry = retry != null,
                )
            }
        }

        fun dismissWorkSessionError() {
            hasProjectCompletionRetry = false
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
            invalidateProjectCompletion()
            workSessionActionJob = viewModelScope.launch { block() }
        }

        fun increment() {
            val state = _uiState.value
            state.projectId ?: return
            // CPD-OFF: Ruudun paikallinen Compose-rakenne pidetaan vastuun yhteydessa.
            val updatedCounter = CounterLogic.increment(state.counter)
            analytics.track(UsageEvent.COUNTER_INCREMENTED)
            if (updatedCounter.count == state.counter.count) return
            val incrementTimestampMillis = System.nanoTime() / 1_000_000
            if (state.counter.stepSize > 1) {
                _stepFeedback.tryEmit(
                    MainCounterStepFeedback(
                        state.projectId,
                        updatedCounter.count - state.counter.count,
                    ),
                )
            }
            val resetStitch = state.stitchTrackingEnabled && updatedCounter.count != state.counter.count
            _uiState.update { it.withCounterChange(updatedCounter, resetStitch) }
            syncRepeatSectionCounters(updatedCounter.count, state.projectCounters, persist = true)
            persistCount(
                change = MainCounterChange.Increment,
                // CPD-ON
                previousValue = state.counter.count,
                newValue = updatedCounter.count,
                incrementTimestampMillis = incrementTimestampMillis,
            )
        }

        fun enableKeepScreenAwake() {
            viewModelScope.launch { preferencesManager.setKeepScreenAwake(true) }
        }

        fun hideProjectSetup() {
            persistProjectSetup { onboardingStateRepository.hideProjectSetup() }
        }

        fun celebrateProjectSetup(
            projectId: Long,
            routeEntryGeneration: Long,
        ) {
            val visit = ProjectSetupVisit(projectId, routeEntryGeneration)
            if (_uiState.value.projectId != projectId || _projectSetupCelebration.value == visit) return
            // Muistijälki säilyy kierrossa, mutta ei siirry seuraavalle käynnille tai uuteen prosessiin.
            _projectSetupCelebration.value = visit
            persistProjectSetup { onboardingStateRepository.markProjectSetupCelebrated() }
        }

        private fun persistProjectSetup(action: suspend () -> Unit) {
            applicationScope.launch {
                try {
                    action()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    Log.w("CounterViewModel", "Unable to persist setup guidance state")
                }
            }
        }

        // CPD-OFF: Laskuritoiminnon paikallinen tila- ja tallennusketju pidetaan yhtenaisena.
        fun decrement() {
            val state = _uiState.value
            state.projectId ?: return
            val updatedCounter = CounterLogic.decrement(state.counter)
            analytics.track(UsageEvent.COUNTER_DECREMENTED)
            if (updatedCounter.count == state.counter.count) return
            if (state.counter.stepSize > 1) {
                _stepFeedback.tryEmit(
                    MainCounterStepFeedback(
                        state.projectId,
                        updatedCounter.count - state.counter.count,
                    ),
                )
            }
            val resetStitch = state.stitchTrackingEnabled && updatedCounter.count != state.counter.count
            _uiState.update { it.withCounterChange(updatedCounter, resetStitch) }
            syncRepeatSectionCounters(updatedCounter.count, state.projectCounters, persist = true)
            persistCount(
                change = MainCounterChange.Decrement,
                previousValue = state.counter.count,
                newValue = updatedCounter.count,
            )
        }
        // CPD-ON

        fun undo() {
            val state = _uiState.value
            val projectId = state.projectId ?: return
            val selectionVersion = projectSelectionVersion
            launchProjectContentMutation {
                val changed = repository.applyMainCounterChange(projectId, MainCounterChange.Undo)
                if (!changed) return@launchProjectContentMutation true
                val updatedProject = repository.getProject(projectId) ?: return@launchProjectContentMutation false
                val currentState = _uiState.value
                if (selectionVersion != projectSelectionVersion || currentState.projectId != projectId) {
                    return@launchProjectContentMutation true
                }

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
                syncRepeatSectionCounters(updatedCounter.count, _uiState.value.projectCounters, persist = false)
                inAppReviewManager.recordAction()
                if (selectionVersion == projectSelectionVersion && _uiState.value.projectId == projectId) {
                    syncWidget(projectId, updatedProject.name, updatedCounter.count)
                }
                true
            }
        }

        fun reset() {
            val state = _uiState.value
            state.projectId ?: return
            val updatedCounter = CounterLogic.reset(state.counter)
            if (updatedCounter.count == state.counter.count) return
            if (state.counter.stepSize > 1) {
                _stepFeedback.tryEmit(
                    MainCounterStepFeedback(
                        state.projectId,
                        updatedCounter.count - state.counter.count,
                    ),
                )
            }
            val resetStitch = state.stitchTrackingEnabled && updatedCounter.count != state.counter.count
            _uiState.update { it.withCounterChange(updatedCounter, resetStitch) }
            syncRepeatSectionCounters(updatedCounter.count, state.projectCounters, persist = true)
            persistCount(
                change = MainCounterChange.Reset,
                previousValue = state.counter.count,
                newValue = updatedCounter.count,
            )
        }

        fun incrementSecondary() {
            if (!_uiState.value.canUseSecondaryCounter) return
            _uiState.update {
                it.copy(secondaryCount = (it.secondaryCount.toLong() + 1L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
            }
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
            val selectionVersion = projectSelectionVersion
            launchProjectContentMutation {
                val saved = tryProjectWrite { repository.updateProjectSecondaryCount(projectId, secondaryCount) }
                if (!saved) {
                    val persisted = repository.getProject(projectId) ?: return@launchProjectContentMutation false
                    _uiState.update { current ->
                        if (current.projectId == projectId && selectionVersion == projectSelectionVersion) {
                            current.copy(
                                secondaryCount = persisted.secondaryCount,
                                secondaryCounterUsed = persisted.secondaryCounterUsed,
                            )
                        } else {
                            current
                        }
                    }
                }
                saved
            }
        }

        // — Multiple Counters —

        private fun observeProjectCounters(
            projectId: Long,
            persistInitialSync: Boolean = true,
        ) {
            counterCollectionJob?.cancel()
            counterCollectionJob =
                viewModelScope.launch {
                    var persistSync = persistInitialSync
                    projectCounterRepository.getCountersForProject(projectId).collect { counters ->
                        val visibleCounters = withoutLegacySecondaryBackfillCopies(counters)
                        syncRepeatSectionCounters(
                            mainRowCount = _uiState.value.counter.count,
                            counters = visibleCounters,
                            persist = persistSync,
                        )
                        persistSync = true
                        _uiState.update { it.copy(countersLoadedProjectId = projectId) }
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
            launchProjectContentMutation {
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
                    projectCounterRepository.addCounter(initialCounter) is ProjectCounterMutationResult.Success
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

        private fun canUseProjectCounter(counter: ProjectCounter): Boolean = canUseProjectCounter(counter.id)

        private fun canUseProjectCounter(counterId: Long): Boolean =
            _uiState.value.projectCounters.any { it.id == counterId }

        fun incrementProjectCounter(counter: ProjectCounter) {
            if (!canUseProjectCounter(counter)) return
            launchProjectContentMutation {
                projectCounterRepository.incrementCounter(counter) is ProjectCounterMutationResult.Success
            }
        }

        fun decrementProjectCounter(counter: ProjectCounter) {
            if (!canUseProjectCounter(counter)) return
            launchProjectContentMutation {
                projectCounterRepository.decrementCounter(counter) is ProjectCounterMutationResult.Success
            }
        }

        fun resetProjectCounter(counterId: Long) {
            if (!canUseProjectCounter(counterId)) return
            val projectId = _uiState.value.projectId ?: return
            launchProjectContentMutation {
                projectCounterRepository.resetCounter(projectId, counterId) is ProjectCounterMutationResult.Success
            }
        }

        fun deleteProjectCounter(counterId: Long) {
            if (!canUseProjectCounter(counterId)) return
            val projectId = _uiState.value.projectId ?: return
            launchProjectContentMutation {
                projectCounterRepository.deleteCounter(projectId, counterId) is ProjectCounterMutationResult.Success
            }
        }

        fun renameProjectCounter(
            counterId: Long,
            name: String,
        ) {
            if (!canUseProjectCounter(counterId)) return
            val projectId = _uiState.value.projectId ?: return
            launchProjectContentMutation {
                projectCounterRepository.renameCounter(
                    projectId,
                    counterId,
                    name,
                ) is ProjectCounterMutationResult.Success
            }
        }

        // — Row Reminders —

        private fun observeReminders(projectId: Long) {
            reminderCollectionJob?.cancel()
            reminderCollectionJob =
                viewModelScope.launch {
                    reminderRepository.getRemindersForProject(projectId).collect { reminders ->
                        _uiState.update { it.withReminderList(reminders).copy(remindersLoadedProjectId = projectId) }
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
            launchProjectContentMutation {
                try {
                    reminderRepository.insert(
                        RowReminder(
                            projectId = draft.projectId,
                            targetRow = draft.targetRow,
                            repeatInterval = draft.repeatInterval,
                            message = draft.message,
                        ),
                    ) is ReminderMutationResult.Success
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
            launchProjectContentMutation {
                reminderRepository.update(
                    projectId,
                    reminder.copy(
                        targetRow = targetRow,
                        repeatInterval = repeatInterval,
                        message = message,
                        isCompleted = false,
                    ),
                    expectedReminder = reminder,
                ) is ReminderMutationResult.Success
            }
        }

        fun dismissReminder(reminderId: Long) {
            val state = _uiState.value
            val projectId = state.projectId ?: return
            val reminder = state.reminders.find { it.id == reminderId } ?: return
            launchProjectContentMutation {
                if (reminder.repeatInterval == null) {
                    // Kertaluonteinen — merkitään valmiiksi
                    val result =
                        reminderRepository.update(
                            projectId,
                            reminder.copy(isCompleted = true),
                            expectedReminder = reminder,
                        )
                    if (result !is ReminderMutationResult.Success) return@launchProjectContentMutation false
                }
                // Toistuva — piilotetaan vain UI-alertista seuraavaan riviin asti
                _uiState.update {
                    if (it.projectId == projectId) it.withDismissedReminder(reminderId) else it
                }
                true
            }
        }

        fun deleteReminder(reminderId: Long) {
            val projectId = _uiState.value.projectId ?: return
            launchProjectContentMutation {
                reminderRepository.delete(projectId, reminderId) is ReminderMutationResult.Success
            }
        }

        // — Progress Photos —

        private fun observeLatestPhotos(projectId: Long) {
            _allPhotos.value = emptyList()
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

        fun savePhoto(
            sourceUri: Uri,
            expectedProjectId: Long? = savedStateHandle[KEY_PROGRESS_PHOTO_CREATION_PROJECT_ID],
        ): Boolean {
            if (!canFinishProgressPhotoCreation()) return false
            val projectId = expectedProjectId ?: return false
            if (!hasExternalResultOwner(projectId)) return false
            launchPhotoOperation {
                var handedToRepository = false
                try {
                    val state = restoredExternalResultState(projectId) ?: return@launchPhotoOperation
                    handedToRepository = true
                    if (photoRepository.savePhoto(projectId, sourceUri, state.counter.count) <= 0L) {
                        photoErrorsChannel.send(Unit)
                        return@launchPhotoOperation
                    }
                    if (savedStateHandle.get<Long>(KEY_PROGRESS_PHOTO_CREATION_PROJECT_ID) == projectId) {
                        cancelProgressPhotoCreation()
                    }
                } finally {
                    if (!handedToRepository) {
                        withContext(NonCancellable + ioDispatcher) {
                            photoRepository.deletePendingPhotoFile(
                                AppFileStorage.resolveAppOwnedFile(context, sourceUri)?.absolutePath,
                            )
                        }
                    }
                }
            }
            return true
        }

        fun authorizeProgressPhotoCreation() {
            savedStateHandle[KEY_PROGRESS_PHOTO_CREATION_AUTHORIZED] = true
            savedStateHandle[KEY_PROGRESS_PHOTO_CREATION_PROJECT_ID] = _uiState.value.projectId
        }

        fun cancelProgressPhotoCreation() {
            savedStateHandle.remove<Boolean>(KEY_PROGRESS_PHOTO_CREATION_AUTHORIZED)
            savedStateHandle.remove<Long>(KEY_PROGRESS_PHOTO_CREATION_PROJECT_ID)
        }

        private fun canFinishProgressPhotoCreation(): Boolean =
            proManager.hasFeature(ProFeature.PROGRESS_PHOTOS) ||
                savedStateHandle.get<Boolean>(KEY_PROGRESS_PHOTO_CREATION_AUTHORIZED) == true

        private fun hasExternalResultOwner(projectId: Long): Boolean =
            _uiState.value.projectId == projectId ||
                (_uiState.value.isRestoringProject && savedStateHandle.get<Long>(KEY_SELECTED_PROJECT_ID) == projectId)

        private suspend fun restoredExternalResultState(projectId: Long): CounterUiState? =
            _uiState.first { !it.isRestoringProject }.takeIf { it.projectId == projectId }

        fun createPhotoCaptureTarget(
            projectId: Long,
            onCreated: (PhotoCaptureTarget?) -> Unit,
        ) {
            if (!canFinishProgressPhotoCreation() || !hasExternalResultOwner(projectId)) {
                onCreated(null)
                return
            }
            viewModelScope.launch {
                if (restoredExternalResultState(projectId) == null) {
                    onCreated(null)
                    return@launch
                }
                val selectionVersion = projectSelectionVersion
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
                if (selectionVersion != projectSelectionVersion || _uiState.value.projectId != projectId) {
                    discardUnclaimedPhotoTarget(target)
                    return@launch
                }
                if (target == null) photoErrorsChannel.send(Unit)
                onCreated(target)
            }
        }

        private suspend fun discardUnclaimedPhotoTarget(target: PhotoCaptureTarget?) {
            if (target == null) return
            runCatching { photoRepository.deletePendingPhotoFile(target.filePath) }
                .onFailure { failure -> if (failure is CancellationException) throw failure }
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
            launchPhotoOperation {
                photoRepository.updatePhotoNote(photoId, note)
            }
        }

        fun deletePhoto(photo: ProgressPhoto) {
            launchPhotoOperation {
                photoRepository.deletePhoto(photo)
            }
        }

        private fun launchPhotoOperation(operation: suspend () -> Unit) {
            viewModelScope.launch {
                try {
                    operation()
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Exception) {
                    photoErrorsChannel.send(Unit)
                }
            }
        }

        fun setNotes(notes: String) {
            val state = _uiState.value
            if (!state.canUseNotes) return
            val id = state.projectId ?: return
            val selectionVersion = projectSelectionVersion
            val pendingSave =
                pendingNotesBase?.takeIf { it.selectionVersion == selectionVersion }
                    ?: PendingNotesSave(selectionVersion, state.notes)
            val request = Any()
            pendingSave.latestRequest = request
            pendingNotesBase = pendingSave
            _uiState.update { it.copy(notes = notes) }
            launchProjectContentMutation {
                notesSaveMutex.withLock {
                    val savedProject =
                        repository
                            .saveProjectNotes(
                                id = id,
                                baseNotes = pendingSave.baseNotes,
                                requestedNotes = notes,
                            ).let { result ->
                                (result as? ProjectNotesSaveResult.Saved)?.project ?: return@withLock false
                            }
                    pendingSave.baseNotes = notes
                    _uiState.update { currentState ->
                        if (currentState.projectId != id || selectionVersion != projectSelectionVersion) {
                            return@update currentState
                        }
                        val isLatestDraft =
                            pendingNotesBase === pendingSave &&
                                pendingSave.latestRequest === request &&
                                currentState.notes == notes
                        if (!isLatestDraft) return@update currentState
                        pendingNotesBase = null
                        currentState.copy(notes = savedProject.notes)
                    }
                    true
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
            val selectionVersion = projectSelectionVersion
            launchProjectContentMutation {
                val savedProject =
                    repository.updateProjectDetails(
                        id = id,
                        name = projectName,
                        craftType = craftType,
                        mainCounterLabelType = mainCounterLabelType,
                        mainCounterCustomLabel = mainCounterCustomLabel,
                    ) ?: return@launchProjectContentMutation false
                _uiState.update { state ->
                    if (state.projectId == id && selectionVersion == projectSelectionVersion) {
                        val observed = state.withObservedProject(savedProject)
                        if (pendingNotesBase?.selectionVersion == selectionVersion) {
                            observed.copy(notes = state.notes)
                        } else {
                            observed
                        }
                    } else {
                        state
                    }
                }
                if (_uiState.value.projectId == id && selectionVersion == projectSelectionVersion) {
                    syncWidget(projectId = id, projectName = savedProject.name)
                }
                true
            }
        }

        fun setStepSize(
            size: Int,
            expectedProjectId: Long? = _uiState.value.projectId,
            onResult: (Boolean) -> Unit,
        ): Job {
            val selectionVersion = projectSelectionVersion
            return viewModelScope.launch {
                onResult(saveStepSize(size, expectedProjectId, selectionVersion))
            }
        }

        private suspend fun saveStepSize(
            size: Int,
            expectedProjectId: Long?,
            selectionVersion: Long,
        ): Boolean {
            val id = expectedProjectId ?: return false
            return stepSizeSaveMutex.withLock {
                if (_uiState.value.projectId != id ||
                    selectionVersion != projectSelectionVersion ||
                    size !in 1..MAX_MAIN_COUNTER_STEP_SIZE
                ) {
                    return@withLock false
                }
                val saved = tryProjectWrite { repository.updateProjectStepSize(id, size) }
                if (_uiState.value.projectId != id || selectionVersion != projectSelectionVersion) {
                    return@withLock false
                }
                if (saved) {
                    _uiState.update { current ->
                        current.copy(counter = CounterLogic.setStepSize(current.counter, size))
                    }
                }
                saved
            }
        }

        fun attachPattern(
            uri: String,
            name: String,
            expectedProjectId: Long? = _uiState.value.projectId,
            onResult: (ProjectDocumentMutationResult) -> Unit = {},
        ) {
            if (uri.isBlank()) {
                onResult(ProjectDocumentMutationResult.PersistenceFailure)
                return
            }
            val projectId = expectedProjectId
            if (projectId == null || !hasExternalResultOwner(projectId)) {
                onResult(ProjectDocumentMutationResult.StaleAction)
                return
            }

            viewModelScope.launch {
                patternImportsInFlight++
                _patternImportInProgress.value = true
                try {
                    onResult(importPattern(projectId, uri, name))
                } finally {
                    patternImportsInFlight--
                    _patternImportInProgress.value = patternImportsInFlight > 0
                }
            }
        }

        private suspend fun importPattern(
            projectId: Long,
            uri: String,
            name: String,
        ): ProjectDocumentMutationResult {
            if (restoredExternalResultState(projectId) == null) return ProjectDocumentMutationResult.StaleAction
            analytics.track(UsageEvent.PDF_IMPORT_STARTED)
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
                analytics.track(UsageEvent.PDF_IMPORT_FAILED)
                return ProjectDocumentMutationResult.PersistenceFailure
            }
            val result =
                persistPatternAttachment(
                    projectId = projectId,
                    // Tiedosto säilyttää alkuperäisen nimensä, nimikkeeksi luettava ohjeen nimi.
                    patternName = PatternDisplayNames.documentLabel(sanitizedName),
                    attachment = attachment,
                )
            analytics.track(
                if (result is ProjectDocumentMutationResult.Added ||
                    result == ProjectDocumentMutationResult.AlreadyAttached
                ) {
                    UsageEvent.PDF_IMPORT_SUCCEEDED
                } else {
                    UsageEvent.PDF_IMPORT_FAILED
                },
            )
            return result
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
            return context.contentResolver.displayNameOrNull(uri)
                ?: uri.lastPathSegment
                ?: context.getString(R.string.pattern_pdf_fallback_name)
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

        /** Kirjaston Liitä projektiin: käyttäjä valitsi projektin, joten laskurin valintaa ei käytetä. */
        fun attachSavedPatternToProject(
            projectId: Long,
            savedPatternId: Long,
            onResult: (attached: Boolean) -> Unit,
        ) {
            if (projectId <= 0L || savedPatternId <= 0L) {
                onResult(false)
                return
            }
            viewModelScope.launch {
                val attached =
                    tryProjectWrite {
                        repository.attachSavedPattern(projectId = projectId, savedPatternId = savedPatternId) != null
                    }
                onResult(attached)
            }
        }

        fun attachSavedPatternMetadata(
            savedPatternId: Long,
            expectedExistingSavedPatternId: Long? = null,
            projectId: Long? = _uiState.value.projectId,
            onResult: (SavedPatternMetadataMutationResult) -> Unit = {},
        ) {
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
                        .getOrNull()
                }
            if (committed == false) {
                attachment.copiedUri
                    ?.takeIf { attachment.reusableUri == null }
                    ?.let { failedUri ->
                        withContext(NonCancellable + ioDispatcher) {
                            savedPatternRepository.deleteLocalPatternFileIfUnused(failedUri)
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
            launchProjectContentMutation {
                when (repository.detachPattern(projectId)) {
                    is ProjectDocumentMutationResult.Removed,
                    ProjectDocumentMutationResult.MissingDocument,
                    -> true
                    else -> false
                }
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
            launchProjectContentMutation {
                repository.updateCurrentPatternPage(projectId, sanitizedPage, documentId)
                true
            }
        }

        fun updatePatternRowMapping(
            mapping: String?,
            documentId: Long? = null,
            documentRowMapping: String? = null,
        ) {
            val projectId = _uiState.value.projectId ?: return
            _uiState.update { it.copy(patternRowMapping = mapping) }
            launchProjectContentMutation {
                if (documentId == null) {
                    repository.updatePatternRowMapping(projectId, mapping)
                    true
                } else {
                    repository.updatePatternRowMappingIfUnchanged(projectId, mapping, documentId, documentRowMapping)
                }
            }
        }

        fun setReadingLineEnabled(
            enabled: Boolean,
            documentId: Long? = null,
        ) {
            val projectId = _uiState.value.projectId ?: return
            _uiState.update { it.copy(readingLineEnabled = enabled) }
            launchProjectContentMutation {
                repository.updateReadingLineVisibility(projectId, enabled, documentId)
                true
            }
        }

        fun commitManualReadingLinePosition(
            yFraction: Float,
            documentId: Long? = null,
        ) {
            val projectId = _uiState.value.projectId ?: return
            val sanitizedYFraction = sanitizeReadingLineYFraction(yFraction)
            _uiState.update {
                it.copy(
                    readingLineYFraction = sanitizedYFraction,
                    readingLineFollowCurrentRow = false,
                )
            }
            launchProjectContentMutation {
                repository.commitManualReadingLinePosition(projectId, sanitizedYFraction, documentId)
                true
            }
        }

        fun setReadingLineFollowCurrentRow(
            enabled: Boolean,
            documentId: Long? = null,
        ) {
            val projectId = _uiState.value.projectId ?: return
            _uiState.update { it.copy(readingLineFollowCurrentRow = enabled) }
            launchProjectContentMutation {
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
                true
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
            launchProjectContentMutation {
                repository.updateVerticalReadingGuide(
                    id = projectId,
                    enabled = enabled,
                    xFraction = state.verticalReadingGuideXFraction.takeIf { documentId == null },
                    documentId = documentId,
                )
                true
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
            launchProjectContentMutation {
                repository.updateVerticalReadingGuide(
                    id = projectId,
                    enabled = state.verticalReadingGuideEnabled.takeIf { documentId == null },
                    xFraction = sanitizedXFraction,
                    documentId = documentId,
                )
                true
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
            updatePatternRowMapping(serializeMapping(markers), documentId, documentRowMapping)
        }

        fun removePatternRowMarker(
            row: Int,
            page: Int,
            documentId: Long? = null,
            documentRowMapping: String? = null,
        ) {
            val currentMarkers = currentPatternRowMarkers(documentId, documentRowMapping) ?: return
            val markers = currentMarkers.filterNot { it.row == row && it.page == page }
            if (markers.size == currentMarkers.size) return
            updatePatternRowMapping(serializeMapping(markers), documentId, documentRowMapping)
        }

        fun removePatternRowMarkersForPage(
            page: Int,
            documentId: Long? = null,
            documentRowMapping: String? = null,
        ) {
            val currentMarkers = currentPatternRowMarkers(documentId, documentRowMapping) ?: return
            val markers = currentMarkers.filterNot { it.page == page }
            if (markers.size == currentMarkers.size) return
            updatePatternRowMapping(serializeMapping(markers), documentId, documentRowMapping)
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
            updatePatternRowMapping(serializeMapping(markers), documentId, documentRowMapping)
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
            change: MainCounterChange,
            previousValue: Int,
            newValue: Int,
            incrementTimestampMillis: Long? = null,
        ) {
            if (newValue == previousValue) return
            val state = _uiState.value
            val projectId = state.projectId ?: return
            val selectionVersion = projectSelectionVersion
            viewModelScope.launch {
                val changed = applyCountChangeAndRecordSignals(projectId, change, incrementTimestampMillis)
                val persisted = loadProjectAfterCountChange(projectId, changed) ?: return@launch
                if (selectionVersion != projectSelectionVersion || _uiState.value.projectId != projectId) return@launch
                _uiState.update {
                    it.copy(
                        counter = CounterState(count = persisted.count, stepSize = persisted.stepSize),
                        currentStitch = persisted.currentStitch,
                    )
                }
                syncRepeatSectionCounters(persisted.count, _uiState.value.projectCounters, persist = false)
                if (changed) inAppReviewManager.recordAction()
                if (selectionVersion == projectSelectionVersion && _uiState.value.projectId == projectId) {
                    syncWidget(projectId, persisted.name, persisted.count)
                }
            }
        }

        private suspend fun applyCountChangeAndRecordSignals(
            projectId: Long,
            change: MainCounterChange,
            incrementTimestampMillis: Long?,
        ): Boolean {
            val committed =
                try {
                    repository.applyMainCounterChangeWithResult(projectId, change)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    null
                }
            val changed = committed != null
            if (!changed) projectContentErrorsChannel.send(Unit)
            if (changed && incrementTimestampMillis != null) {
                tipSession.onSignal(TipSignal.Increment(projectId, incrementTimestampMillis))
            }
            if (committed?.crossedTarget == true) {
                tipSession.onSignal(TipSignal.TargetReached(projectId))
            }
            return changed
        }

        private suspend fun loadProjectAfterCountChange(
            projectId: Long,
            changed: Boolean,
        ): CounterProject? =
            try {
                repository.getProject(projectId)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                if (changed) projectContentErrorsChannel.send(Unit)
                null
            }

        private suspend fun syncWidget(
            projectId: Long? = _uiState.value.projectId,
            projectName: String = _uiState.value.projectName,
            count: Int = _uiState.value.counter.count,
        ) {
            val resolvedProjectId = projectId ?: return
            val state = _uiState.value
            val label =
                CounterValueFormatter.mainCounterLabel(
                    craftType = state.craftType,
                    labelType = state.mainCounterLabelType,
                    customLabel = state.mainCounterCustomLabel,
                )
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
                    labelType = label.labelType,
                    customLabel = label.customLabel,
                )
            try {
                CounterWidgetState.syncAll(context, widgetData)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                // The persisted project remains authoritative if widget refresh fails.
            }
        }

        fun selectProjectById(id: Long) {
            selectProjectByIdForLaunch(id)
        }

        fun selectProjectByIdForLaunch(
            id: Long,
            onLoaded: (Boolean) -> Unit = {},
        ) {
            projectSelectionVersion++
            if (id != (_uiState.value.projectId ?: pendingReactivation?.projectId)) dismissPendingReactivation()
            projectSelectionJob?.cancel()
            projectSelectionJob =
                viewModelScope.launch {
                    try {
                        onLoaded(loadProjectForLaunch(id))
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (_: Exception) {
                        onLoaded(false)
                    } finally {
                        if (currentCoroutineContext().isActive) {
                            _uiState.update { it.copy(isRestoringProject = false) }
                        }
                    }
                }
        }

        private suspend fun loadProjectForLaunch(id: Long): Boolean {
            val project = repository.getProject(id) ?: return false
            openProject(project)
            return true
        }

        fun setStitchCount(
            count: Int?,
            enableTracking: Boolean = false,
        ) {
            if (count != null && count <= 0) return
            val state = _uiState.value
            val projectId = state.projectId ?: return
            val shouldDisableTracking = count == null
            val nextStitch = count?.let { total -> state.currentStitch.coerceIn(0, total) } ?: 0
            _uiState.update {
                it.copy(
                    stitchCount = count,
                    stitchTrackingEnabled = !shouldDisableTracking && (enableTracking || it.stitchTrackingEnabled),
                    currentStitch = nextStitch,
                )
            }
            persistStitchChange(projectId) {
                repository.updateProjectStitchCount(projectId, count, enableTracking)
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
            persistStitchChange(projectId) {
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
            persistStitchChange(projectId) {
                repository.updateStitchTrackingEnabled(projectId, enabled)
            }
        }

        private suspend fun tryProjectWrite(operation: suspend () -> Boolean): Boolean =
            try {
                operation()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                false
            }

        private fun persistStitchChange(
            projectId: Long,
            operation: suspend () -> Boolean,
        ) {
            val selectionVersion = projectSelectionVersion
            launchProjectContentMutation {
                val saved = tryProjectWrite(operation)
                val persisted = repository.getProject(projectId) ?: return@launchProjectContentMutation false
                if (selectionVersion == projectSelectionVersion && _uiState.value.projectId == projectId) {
                    _uiState.update {
                        it.copy(
                            stitchCount = persisted.stitchCount,
                            currentStitch = persisted.currentStitch,
                            stitchTrackingEnabled = persisted.stitchTrackingEnabled,
                        )
                    }
                }
                saved
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
            const val KEY_PROGRESS_PHOTO_CREATION_PROJECT_ID = "counter.progress_photo_creation_project_id"
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
