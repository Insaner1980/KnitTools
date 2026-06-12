package com.finnvek.knittools.ui.screens.counter

import android.content.Context
import android.net.Uri
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
import com.finnvek.knittools.di.IoDispatcher
import com.finnvek.knittools.domain.calculator.CounterLogic
import com.finnvek.knittools.domain.calculator.CounterState
import com.finnvek.knittools.domain.calculator.RepeatSectionLogic
import com.finnvek.knittools.domain.calculator.RowMarker
import com.finnvek.knittools.domain.calculator.parseMapping
import com.finnvek.knittools.domain.calculator.serializeMapping
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.CraftType
import com.finnvek.knittools.domain.model.DEFAULT_READING_LINE_Y_FRACTION
import com.finnvek.knittools.domain.model.KnitSession
import com.finnvek.knittools.domain.model.MainCounterChange
import com.finnvek.knittools.domain.model.MainCounterLabelType
import com.finnvek.knittools.domain.model.ProgressPhoto
import com.finnvek.knittools.domain.model.ProjectCounter
import com.finnvek.knittools.domain.model.ProjectCounterDraft
import com.finnvek.knittools.domain.model.ProjectCounterType
import com.finnvek.knittools.domain.model.ProjectYarnNote
import com.finnvek.knittools.domain.model.RowReminder
import com.finnvek.knittools.domain.model.SavedPattern
import com.finnvek.knittools.domain.model.YarnCard
import com.finnvek.knittools.domain.model.displayName
import com.finnvek.knittools.domain.model.parseYarnCardIds
import com.finnvek.knittools.domain.model.sanitizeReadingLineYFraction
import com.finnvek.knittools.pro.InAppReviewManager
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.repository.ProgressPhotoRepository
import com.finnvek.knittools.repository.ProjectCounterRepository
import com.finnvek.knittools.repository.ProjectYarnNoteRepository
import com.finnvek.knittools.repository.ReminderRepository
import com.finnvek.knittools.repository.SavedPatternRepository
import com.finnvek.knittools.repository.YarnCardRepository
import com.finnvek.knittools.widget.CounterWidgetState
import com.finnvek.knittools.widget.WidgetData
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class CounterUiState(
    val projectName: String = "",
    val counter: CounterState = CounterState(),
    val craftType: CraftType = CraftType.KNITTING,
    val mainCounterLabelType: MainCounterLabelType = MainCounterLabelType.ROWS,
    val mainCounterCustomLabel: String? = null,
    val secondaryCount: Int = 0,
    val notes: String = "",
    val sessionSeconds: Long = 0,
    val projectId: Long? = null,
    val hapticFeedback: Boolean = true,
    val keepScreenAwake: Boolean = false,
    val isPro: Boolean = false,
    val canUseSecondaryCounter: Boolean = false,
    val canUseMultipleCounters: Boolean = false,
    val canUseRowReminders: Boolean = false,
    val canUseProgressPhotos: Boolean = false,
    val canUsePatternCameraScan: Boolean = false,
    val projects: List<CounterProject> = emptyList(),
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
    val patternUri: String? = null,
    val patternName: String? = null,
    val currentPatternPage: Int = 0,
    val readingLineEnabled: Boolean = false,
    val readingLineYFraction: Float = DEFAULT_READING_LINE_Y_FRACTION,
    val patternRowMapping: String? = null,
    val totalRows: Int? = null,
    val targetRows: Int? = null,
)

data class DismissedReminderTrigger(
    val reminderId: Long,
    val row: Int,
)

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
        private val patternDocumentStorage: PatternDocumentStorage,
        private val inAppReviewManager: InAppReviewManager,
        private val savedStateHandle: SavedStateHandle,
        @param:ApplicationContext private val context: Context,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : ViewModel() {
        private val _uiState =
            MutableStateFlow(
                CounterUiState(
                    projectName = context.getString(R.string.default_project_name),
                    sessionSeconds = savedStateHandle[KEY_SESSION_SECONDS] ?: 0L,
                ),
            )
        val uiState: StateFlow<CounterUiState> = _uiState.asStateFlow()

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

        private var timerJob: Job? = null
        private var selectedProjectJob: Job? = null
        private var reminderCollectionJob: Job? = null
        private var counterCollectionJob: Job? = null
        private var photoCollectionJob: Job? = null
        private var projectYarnNoteCollectionJob: Job? = null
        private var allPhotosJob: Job? = null
        private var linkedYarnIdsCache: String = ""
        private var pendingSavedPatternAttachment: SavedPattern? = null
        private var isForeground = true
        private var didRecoverPendingSession = false

        // Session tracking
        private var sessionStartedAt: Long = savedStateHandle[KEY_SESSION_STARTED_AT] ?: System.currentTimeMillis()
        private var sessionStartRow: Int = savedStateHandle[KEY_SESSION_START_ROW] ?: 0
        private var sessionRowsWorked: Int = savedStateHandle[KEY_SESSION_ROWS_WORKED] ?: 0

        private val lifecycleObserver =
            object : DefaultLifecycleObserver {
                override fun onResume(owner: LifecycleOwner) {
                    isForeground = true
                    val state = _uiState.value
                    val projectId = state.projectId ?: return
                    restartSessionSegment(projectId, state.counter.count)
                }

                override fun onPause(owner: LifecycleOwner) {
                    isForeground = false
                    viewModelScope.launch {
                        val state = _uiState.value
                        val projectId = state.projectId ?: return@launch
                        val didPersist =
                            persistSessionSnapshotIfNeeded(
                                projectId = projectId,
                                endRow = state.counter.count,
                                sessionSeconds = state.sessionSeconds,
                            )
                        if (didPersist) {
                            restartSessionSegment(projectId, state.counter.count)
                        }
                    }
                }
            }

        init {
            ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleObserver)
            loadOrCreateProject()
            startTimer()
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
                        it.copy(
                            isPro = proState.isPro,
                            canUseSecondaryCounter = proState.hasFeature(ProFeature.SECONDARY_COUNTER),
                            canUseMultipleCounters = proState.hasFeature(ProFeature.MULTIPLE_COUNTERS),
                            canUseRowReminders = proState.hasFeature(ProFeature.ROW_REMINDERS),
                            canUseProgressPhotos = proState.hasFeature(ProFeature.PROGRESS_PHOTOS),
                            canUsePatternCameraScan = proState.hasFeature(ProFeature.PATTERN_CAMERA_SCAN),
                        )
                    }
                    if (!proState.isPro) {
                        pruneHistoryForFree()
                    }
                }
            }
        }

        private fun loadOrCreateProject() {
            viewModelScope.launch {
                repository.getActiveProjects().collect { list ->
                    if (list.isEmpty()) {
                        repository.createProject(context.getString(R.string.default_project_name))
                    } else {
                        if (!didRecoverPendingSession) {
                            recoverPendingSessionIfNeeded(list)
                            didRecoverPendingSession = true
                        }
                        _uiState.update { it.copy(projects = list) }

                        val currentId = _uiState.value.projectId ?: savedStateHandle.get<Long>(KEY_SELECTED_PROJECT_ID)
                        val targetProject =
                            currentId?.let { id -> list.find { it.id == id } }
                                ?: list.first()

                        if (_uiState.value.projectId != targetProject.id || selectedProjectJob == null) {
                            startProjectSession(targetProject)
                        }
                    }
                }
            }
        }

        fun selectProject(project: CounterProject) {
            viewModelScope.launch {
                persistCurrentSessionIfNeeded()
                startProjectSession(project)
            }
        }

        private fun observeSelectedProject(projectId: Long) {
            selectedProjectJob?.cancel()
            selectedProjectJob =
                viewModelScope.launch {
                    repository.observeProject(projectId).collect { project ->
                        if (project == null) {
                            _uiState.update { it.copy(projectId = null) }
                            return@collect
                        }

                        val previousState = _uiState.value
                        val countChanged = previousState.counter.count != project.count
                        if (countChanged && !isForeground && previousState.projectId == project.id) {
                            persistSessionSnapshotIfNeeded(
                                projectId = project.id,
                                endRow = previousState.counter.count,
                                sessionSeconds = previousState.sessionSeconds,
                            )
                            restartSessionSegment(project.id, project.count)
                        }
                        _uiState.update { state -> state.withObservedProject(project) }
                        if (countChanged) {
                            syncRepeatSectionCounters(project.count, _uiState.value.projectCounters, persist = true)
                        }

                        if (linkedYarnIdsCache != project.yarnCardIds) {
                            linkedYarnIdsCache = project.yarnCardIds
                            loadLinkedYarnNames(project.yarnCardIds)
                        }
                    }
                }
        }

        fun linkYarnCard(cardId: Long) {
            viewModelScope.launch {
                val id = _uiState.value.projectId ?: return@launch
                yarnCardRepository.updateLinkedProjectId(cardId, id)
            }
        }

        fun unlinkYarnCard(cardId: Long) {
            viewModelScope.launch {
                yarnCardRepository.updateLinkedProjectId(cardId, null)
            }
        }

        private suspend fun loadLinkedPattern(linkedPatternId: Long?) {
            if (linkedPatternId == null) {
                _uiState.update { it.copy(linkedPattern = null) }
                return
            }
            val pattern = savedPatternRepository.getById(linkedPatternId)
            _uiState.update { it.copy(linkedPattern = pattern) }
        }

        private suspend fun loadLinkedYarnNames(yarnCardIds: String) {
            if (yarnCardIds.isBlank()) {
                _uiState.update { it.copy(linkedYarns = emptyList()) }
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
            _uiState.update { it.copy(linkedYarns = yarns) }
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

        fun saveProjectYarnNoteToMyYarn(noteId: Long) {
            viewModelScope.launch {
                projectYarnNoteRepository.saveToMyYarn(noteId)
            }
        }

        private suspend fun loadTotalSessionMinutes(projectId: Long) {
            val minutes = repository.getTotalMinutesForProject(projectId)
            _uiState.update { it.copy(totalSessionMinutes = minutes) }
        }

        private suspend fun persistCurrentSessionIfNeeded() {
            val state = _uiState.value
            val projectId = state.projectId ?: return
            persistSessionSnapshotIfNeeded(
                projectId = projectId,
                endRow = state.counter.count,
                sessionSeconds = state.sessionSeconds,
            )
            clearPendingSessionState()
        }

        private suspend fun persistSessionSnapshotIfNeeded(
            projectId: Long,
            endRow: Int,
            sessionSeconds: Long,
        ): Boolean {
            val now = System.currentTimeMillis()
            val durationSeconds =
                SessionProgress.resolveDurationSeconds(
                    recordedSeconds = sessionSeconds,
                    startedAt = sessionStartedAt,
                    nowMillis = now,
                )
            if (durationSeconds < 1L) return false
            val durationMinutes = ((durationSeconds + 59L) / 60L).toInt().coerceAtLeast(1)
            val rowsWorked = sessionRowsWorked.takeIf { it > 0 } ?: (endRow - sessionStartRow).coerceAtLeast(0)

            repository.insertSession(
                KnitSession(
                    projectId = projectId,
                    startedAt = sessionStartedAt,
                    endedAt = now,
                    startRow = sessionStartRow,
                    endRow = endRow,
                    durationMinutes = durationMinutes,
                    durationSeconds = durationSeconds,
                    rowsWorked = rowsWorked,
                ),
            )
            return true
        }

        private suspend fun recoverPendingSessionIfNeeded(projects: List<CounterProject>) {
            val projectId = savedStateHandle.get<Long>(KEY_SELECTED_PROJECT_ID) ?: return
            val pendingProject =
                projects.find { it.id == projectId } ?: run {
                    clearPendingSessionState()
                    clearSelectedProject()
                    return
                }
            val savedSeconds = savedStateHandle.get<Long>(KEY_SESSION_SECONDS) ?: return
            if (!savedStateHandle.contains(KEY_SESSION_STARTED_AT) ||
                !savedStateHandle.contains(KEY_SESSION_START_ROW)
            ) {
                clearPendingSessionState()
                return
            }

            persistSessionSnapshotIfNeeded(
                projectId = projectId,
                endRow = pendingProject.count,
                sessionSeconds = savedSeconds,
            )
            clearPendingSessionState()
        }

        private suspend fun startProjectSession(project: CounterProject) {
            sessionStartedAt = System.currentTimeMillis()
            sessionStartRow = project.count
            sessionRowsWorked = 0
            linkedYarnIdsCache = project.yarnCardIds

            saveSelectedProject(project.id)
            savePendingSessionState(project.id, 0L)

            _uiState.update { it.withStartedProject(project) }
            observeSelectedProject(project.id)
            observeReminders(project.id)
            observeProjectCounters(project.id)
            observeLatestPhotos(project.id)
            observeProjectYarnNotes(project.id)
            syncWidget()
            loadLinkedYarnNames(project.yarnCardIds)
            loadLinkedPattern(project.linkedPatternId)
            attachPendingSavedPatternIfReady()
            loadTotalSessionMinutes(project.id)
        }

        private fun saveSelectedProject(projectId: Long) {
            savedStateHandle[KEY_SELECTED_PROJECT_ID] = projectId
        }

        private fun clearSelectedProject() {
            savedStateHandle.remove<Long>(KEY_SELECTED_PROJECT_ID)
        }

        private fun savePendingSessionState(
            projectId: Long,
            sessionSeconds: Long,
        ) {
            savedStateHandle[KEY_SELECTED_PROJECT_ID] = projectId
            savedStateHandle[KEY_SESSION_STARTED_AT] = sessionStartedAt
            savedStateHandle[KEY_SESSION_START_ROW] = sessionStartRow
            savedStateHandle[KEY_SESSION_SECONDS] = sessionSeconds
            savedStateHandle[KEY_SESSION_ROWS_WORKED] = sessionRowsWorked
        }

        private fun clearPendingSessionState() {
            savedStateHandle.remove<Long>(KEY_SESSION_STARTED_AT)
            savedStateHandle.remove<Int>(KEY_SESSION_START_ROW)
            savedStateHandle.remove<Long>(KEY_SESSION_SECONDS)
            savedStateHandle.remove<Int>(KEY_SESSION_ROWS_WORKED)
        }

        private fun restartSessionSegment(
            projectId: Long,
            startRow: Int,
        ) {
            sessionStartedAt = System.currentTimeMillis()
            sessionStartRow = startRow
            sessionRowsWorked = 0
            _uiState.update { it.copy(sessionSeconds = 0L) }
            savePendingSessionState(projectId, 0L)
        }

        fun openSessionHistory(onReady: (Long) -> Unit) {
            val state = _uiState.value
            val projectId = state.projectId ?: return
            viewModelScope.launch {
                val didPersist =
                    persistSessionSnapshotIfNeeded(
                        projectId = projectId,
                        endRow = state.counter.count,
                        sessionSeconds = state.sessionSeconds,
                    )
                if (didPersist) {
                    restartSessionSegment(projectId, state.counter.count)
                    loadTotalSessionMinutes(projectId)
                }
                onReady(projectId)
            }
        }

        fun createNewProject(name: String): Boolean {
            if (name.isBlank()) return false
            if (!proManager.hasFeature(ProFeature.UNLIMITED_PROJECTS)) {
                val count = _uiState.value.projects.size
                if (count >= 1) return false
            }
            viewModelScope.launch {
                val id = repository.createProject(name) ?: return@launch
                repository.getProject(id)?.let { selectProject(it) }
            }
            return true
        }

        fun completeProject() {
            val state = _uiState.value
            val projectId = state.projectId ?: return
            viewModelScope.launch {
                persistSessionSnapshotIfNeeded(projectId, state.counter.count, state.sessionSeconds)
                repository.archiveProject(
                    id = projectId,
                    totalRows = state.counter.count,
                    completedAt = System.currentTimeMillis(),
                )
                _uiState.update { it.copy(projectId = null) }
                clearPendingSessionState()
                clearSelectedProject()
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
            viewModelScope.launch {
                repository.deleteProject(id)
                if (_uiState.value.projectId == id) {
                    _uiState.update { it.copy(projectId = null) }
                    clearPendingSessionState()
                    clearSelectedProject()
                }
            }
        }

        fun increment() {
            val state = _uiState.value
            state.projectId ?: return
            val updatedCounter = CounterLogic.increment(state.counter)
            if (updatedCounter.count == state.counter.count) return
            val resetStitch = state.stitchTrackingEnabled && updatedCounter.count != state.counter.count
            _uiState.update { it.withCounterChange(updatedCounter, resetStitch) }
            syncRepeatSectionCounters(updatedCounter.count, state.projectCounters, persist = true)
            persistCurrentStitchIfNeeded(resetStitch)
            persistCount(
                action = "increment",
                previousValue = state.counter.count,
                newValue = updatedCounter.count,
            )
        }

        fun decrement() {
            val state = _uiState.value
            state.projectId ?: return
            val updatedCounter = CounterLogic.decrement(state.counter)
            if (updatedCounter.count == state.counter.count) return
            val resetStitch = state.stitchTrackingEnabled && updatedCounter.count != state.counter.count
            _uiState.update { it.withCounterChange(updatedCounter, resetStitch) }
            syncRepeatSectionCounters(updatedCounter.count, state.projectCounters, persist = true)
            persistCurrentStitchIfNeeded(resetStitch)
            persistCount(
                action = "decrement",
                previousValue = state.counter.count,
                newValue = updatedCounter.count,
            )
        }

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
                trackSessionRows("undo", previousValue = state.counter.count, newValue = updatedCounter.count)
                _uiState.update { latestState ->
                    if (latestState.projectId == projectId) {
                        latestState.withCounterChange(updatedCounter, resetStitch)
                    } else {
                        latestState
                    }
                }
                syncRepeatSectionCounters(updatedCounter.count, _uiState.value.projectCounters, persist = true)
                inAppReviewManager.recordAction()
                savePendingSessionState(projectId, _uiState.value.sessionSeconds)
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
            persistCurrentStitchIfNeeded(resetStitch)
            persistCount(
                action = "reset",
                previousValue = state.counter.count,
                newValue = updatedCounter.count,
            )
        }

        fun incrementSecondary() {
            if (!proManager.hasFeature(ProFeature.SECONDARY_COUNTER)) return
            _uiState.update { it.copy(secondaryCount = it.secondaryCount + 1) }
            persistSecondary()
        }

        fun decrementSecondary() {
            if (!proManager.hasFeature(ProFeature.SECONDARY_COUNTER)) return
            _uiState.update { it.copy(secondaryCount = maxOf(0, it.secondaryCount - 1)) }
            persistSecondary()
        }

        private fun persistSecondary() {
            viewModelScope.launch {
                val state = _uiState.value
                val id = state.projectId ?: return@launch
                repository.updateProjectSecondaryCount(id, state.secondaryCount)
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

        fun addProjectCounter(draft: ProjectCounterDraft) {
            if (!canAddProjectCounter(draft)) return
            viewModelScope.launch {
                val projectId = _uiState.value.projectId ?: return@launch
                val counter =
                    ProjectCounter(
                        projectId = projectId,
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
                        RepeatSectionLogic.updatePosition(counter, _uiState.value.counter.count)
                    } else {
                        counter
                    }
                projectCounterRepository.addCounter(initialCounter)
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

        fun incrementProjectCounter(counter: ProjectCounter) {
            viewModelScope.launch {
                projectCounterRepository.incrementCounter(counter)
            }
        }

        fun decrementProjectCounter(counter: ProjectCounter) {
            viewModelScope.launch {
                projectCounterRepository.decrementCounter(counter)
            }
        }

        fun resetProjectCounter(counterId: Long) {
            viewModelScope.launch {
                projectCounterRepository.resetCounter(counterId)
            }
        }

        fun deleteProjectCounter(counterId: Long) {
            viewModelScope.launch {
                projectCounterRepository.deleteCounter(counterId)
            }
        }

        fun renameProjectCounter(
            counterId: Long,
            name: String,
        ) {
            viewModelScope.launch {
                projectCounterRepository.renameCounter(counterId, name)
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
        ) {
            if (!proManager.hasFeature(ProFeature.ROW_REMINDERS)) return
            viewModelScope.launch {
                val projectId = _uiState.value.projectId ?: return@launch
                reminderRepository.insert(
                    RowReminder(
                        projectId = projectId,
                        targetRow = targetRow,
                        repeatInterval = repeatInterval,
                        message = message.take(200),
                    ),
                )
            }
        }

        fun updateReminder(
            reminderId: Long,
            targetRow: Int,
            repeatInterval: Int?,
            message: String,
        ) {
            viewModelScope.launch {
                val reminder = _uiState.value.reminders.find { it.id == reminderId } ?: return@launch
                reminderRepository.update(
                    reminder.copy(
                        targetRow = targetRow,
                        repeatInterval = repeatInterval,
                        message = message.take(200),
                        isCompleted = false,
                    ),
                )
            }
        }

        fun dismissReminder(reminderId: Long) {
            viewModelScope.launch {
                val reminder = _uiState.value.reminders.find { it.id == reminderId } ?: return@launch
                if (reminder.repeatInterval == null) {
                    // Kertaluonteinen — merkitään valmiiksi
                    reminderRepository.update(reminder.copy(isCompleted = true))
                }
                // Toistuva — piilotetaan vain UI-alertista seuraavaan riviin asti
                _uiState.update { it.withDismissedReminder(reminderId) }
            }
        }

        fun deleteReminder(reminderId: Long) {
            viewModelScope.launch {
                reminderRepository.delete(reminderId)
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
            if (!proManager.hasFeature(ProFeature.PROGRESS_PHOTOS)) return
            viewModelScope.launch {
                val state = _uiState.value
                val projectId = state.projectId ?: return@launch
                photoRepository.savePhoto(projectId, sourceUri, state.counter.count)
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
            if (!proManager.hasFeature(ProFeature.NOTES)) return
            val state = _uiState.value
            val previousNotes = state.notes
            _uiState.update { it.copy(notes = notes) }
            viewModelScope.launch {
                val id = state.projectId ?: return@launch
                val savedProject =
                    repository.saveProjectNotes(
                        id = id,
                        baseNotes = previousNotes,
                        requestedNotes = notes,
                    ) ?: return@launch
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
            viewModelScope.launch {
                val id = _uiState.value.projectId ?: return@launch
                val savedName = repository.updateProjectName(id, projectName) ?: return@launch
                _uiState.update { state ->
                    if (state.projectId == id) {
                        state.copy(projectName = savedName)
                    } else {
                        state
                    }
                }
                syncWidget(projectName = savedName)
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
            viewModelScope.launch {
                val id = _uiState.value.projectId ?: return@launch
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
                syncWidget(projectName = savedProject.name)
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
        ) {
            if (uri.isBlank()) return
            val sanitizedName = name.ifBlank { context.getString(R.string.pattern_pdf_fallback_name) }
            val state = _uiState.value
            val projectId = state.projectId ?: return

            viewModelScope.launch {
                val attachment =
                    preparePatternAttachment(
                        sourceUriString = uri,
                        projectId = projectId,
                        sanitizedName = sanitizedName,
                    ) ?: return@launch

                updateAttachedPatternState(
                    patternUri = attachment.internalUri,
                    patternName = sanitizedName,
                )
                persistPatternAttachment(
                    projectId = projectId,
                    patternName = sanitizedName,
                    attachment = attachment,
                )
            }
        }

        fun attachSavedPattern(pattern: SavedPattern) {
            if (pattern.id <= 0L) return
            val projectId = _uiState.value.projectId
            if (projectId == null) {
                pendingSavedPatternAttachment = pattern
                return
            }
            pendingSavedPatternAttachment = null
            updateSavedPatternAttachmentState(pattern)
            viewModelScope.launch {
                repository.attachSavedPattern(
                    projectId = projectId,
                    savedPatternId = pattern.id,
                )
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
                    patternDocumentStorage.copyPdfToInternal(
                        context = context,
                        projectId = projectId,
                        sourceUri = sourceUri,
                        fileName = sanitizedName,
                    )
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

        private fun updateAttachedPatternState(
            patternUri: String,
            patternName: String,
        ) {
            _uiState.update {
                it.copy(
                    linkedPattern = null,
                    patternUri = patternUri,
                    patternName = patternName,
                    currentPatternPage = 0,
                    patternRowMapping = null,
                )
            }
        }

        private fun updateSavedPatternAttachmentState(pattern: SavedPattern) {
            _uiState.update {
                it.copy(
                    linkedPattern = pattern,
                    patternUri = pattern.localPdfUri,
                    patternName = pattern.name,
                    currentPatternPage = 0,
                    patternRowMapping = null,
                )
            }
        }

        private suspend fun persistPatternAttachment(
            projectId: Long,
            patternName: String,
            attachment: PatternAttachment,
        ) {
            runCatching {
                repository.attachPattern(
                    id = projectId,
                    patternUri = attachment.internalUri,
                    patternName = patternName,
                    currentPatternPage = 0,
                    patternRowMapping = null,
                )
            }.onFailure {
                attachment.copiedUri
                    ?.takeIf { attachment.reusableUri == null }
                    ?.let { failedUri ->
                        withContext(ioDispatcher) {
                            AppFileStorage.deleteIfAppOwned(context, failedUri)
                        }
                    }
            }.getOrThrow()
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

        fun updatePatternPage(page: Int) {
            val projectId = _uiState.value.projectId ?: return
            val sanitizedPage = page.coerceAtLeast(0)
            _uiState.update { it.copy(currentPatternPage = sanitizedPage) }
            viewModelScope.launch {
                repository.updateCurrentPatternPage(projectId, sanitizedPage)
            }
        }

        fun updatePatternRowMapping(mapping: String?) {
            val projectId = _uiState.value.projectId ?: return
            _uiState.update { it.copy(patternRowMapping = mapping) }
            viewModelScope.launch {
                repository.updatePatternRowMapping(projectId, mapping)
            }
        }

        fun setReadingLineEnabled(enabled: Boolean) {
            val state = _uiState.value
            val projectId = state.projectId ?: return
            _uiState.update { it.copy(readingLineEnabled = enabled) }
            viewModelScope.launch {
                repository.updateReadingLine(projectId, enabled, state.readingLineYFraction)
            }
        }

        fun updateReadingLineYFraction(yFraction: Float) {
            val state = _uiState.value
            val projectId = state.projectId ?: return
            val sanitizedYFraction = sanitizeReadingLineYFraction(yFraction)
            _uiState.update { it.copy(readingLineYFraction = sanitizedYFraction) }
            viewModelScope.launch {
                repository.updateReadingLine(projectId, state.readingLineEnabled, sanitizedYFraction)
            }
        }

        fun upsertPatternRowMarker(
            row: Int,
            page: Int,
            yPosition: Float,
        ) {
            val markers = parseMapping(_uiState.value.patternRowMapping).toMutableList()
            val sanitizedY = yPosition.coerceIn(0f, 1f)
            val index = markers.indexOfFirst { it.row == row && it.page == page }
            val marker = RowMarker(row = row, page = page, yPosition = sanitizedY)
            if (index >= 0) {
                markers[index] = marker
            } else {
                markers += marker
            }
            updatePatternRowMapping(serializeMapping(markers))
        }

        fun removePatternRowMarker(
            row: Int,
            page: Int,
        ) {
            val markers =
                parseMapping(_uiState.value.patternRowMapping)
                    .filterNot { it.row == row && it.page == page }
            updatePatternRowMapping(serializeMapping(markers))
        }

        fun removePatternRowMarkersForPage(page: Int) {
            val markers =
                parseMapping(_uiState.value.patternRowMapping)
                    .filterNot { it.page == page }
            updatePatternRowMapping(serializeMapping(markers))
        }

        fun mergePatternRowMarkers(markersToMerge: List<RowMarker>) {
            if (markersToMerge.isEmpty()) return
            val markers = parseMapping(_uiState.value.patternRowMapping).toMutableList()
            markersToMerge.forEach { marker ->
                val index = markers.indexOfFirst { it.row == marker.row && it.page == marker.page }
                if (index >= 0) {
                    markers[index] = marker
                } else {
                    markers += marker
                }
            }
            updatePatternRowMapping(serializeMapping(markers))
        }

        private fun persistCount(
            action: String,
            previousValue: Int,
            newValue: Int,
        ) {
            if (newValue == previousValue) return
            val state = _uiState.value
            val projectId = state.projectId ?: return
            trackSessionRows(action, previousValue, newValue)
            viewModelScope.launch {
                inAppReviewManager.recordAction()
                repository.applyMainCounterChange(projectId, action.toMainCounterChange())
                savePendingSessionState(projectId, state.sessionSeconds)
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

        private fun trackSessionRows(
            action: String,
            previousValue: Int,
            newValue: Int,
        ) {
            sessionRowsWorked =
                SessionProgress.adjustRowsWorked(
                    currentRowsWorked = sessionRowsWorked,
                    action = action,
                    previousValue = previousValue,
                    newValue = newValue,
                )
        }

        private fun pruneHistoryForFree() {
            viewModelScope.launch {
                val id = _uiState.value.projectId ?: return@launch
                val cutoff = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(HISTORY_LIMIT_HOURS)
                repository.deleteHistoryBefore(id, cutoff)
            }
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

        private fun startTimer() {
            timerJob =
                viewModelScope.launch {
                    while (true) {
                        delay(1000)
                        if (isForeground) {
                            var pendingSessionProjectId: Long? = null
                            var pendingSessionSeconds = 0L

                            _uiState.update {
                                val nextSeconds = it.sessionSeconds + 1
                                pendingSessionProjectId = it.projectId
                                pendingSessionSeconds = nextSeconds
                                it.copy(sessionSeconds = nextSeconds)
                            }

                            pendingSessionProjectId?.let { projectId ->
                                savePendingSessionState(projectId, pendingSessionSeconds)
                            }
                        }
                    }
                }
        }

        fun selectProjectById(id: Long) {
            selectProjectByIdForLaunch(id)
        }

        fun selectProjectByIdForLaunch(
            id: Long,
            onLoaded: (Boolean) -> Unit = {},
        ) {
            viewModelScope.launch {
                onLoaded(loadProjectForLaunch(id))
            }
        }

        private suspend fun loadProjectForLaunch(id: Long): Boolean {
            val project = repository.getProject(id) ?: return false
            persistCurrentSessionIfNeeded()
            startProjectSession(project)
            return true
        }

        fun setSectionName(name: String?) {
            _uiState.update { it.copy(sectionName = name) }
            viewModelScope.launch {
                val id = _uiState.value.projectId ?: return@launch
                repository.updateProjectSectionName(id, name)
            }
        }

        fun setStitchCount(count: Int?) {
            val sanitizedCount = count?.takeIf { it > 0 }
            val shouldDisableTracking = sanitizedCount == null
            val nextStitch = sanitizedCount?.let { total -> _uiState.value.currentStitch.coerceAtMost(total) } ?: 0
            _uiState.update {
                it.copy(
                    stitchCount = sanitizedCount,
                    stitchTrackingEnabled = !shouldDisableTracking && it.stitchTrackingEnabled,
                    currentStitch = nextStitch,
                )
            }
            viewModelScope.launch {
                val id = _uiState.value.projectId ?: return@launch
                repository.updateProjectStitchCount(id, sanitizedCount)
                repository.updateCurrentStitch(id, nextStitch)
                if (shouldDisableTracking) {
                    repository.updateStitchTrackingEnabled(id, false)
                }
            }
        }

        fun incrementStitch() {
            val state = _uiState.value
            val projectId = state.projectId ?: return
            val totalStitches = state.stitchCount ?: return
            if (!state.stitchTrackingEnabled || totalStitches <= 0) return
            val nextStitch = (state.currentStitch + 1).coerceAtMost(totalStitches)
            if (nextStitch == state.currentStitch) return
            _uiState.update { it.copy(currentStitch = nextStitch) }
            viewModelScope.launch {
                repository.updateCurrentStitch(projectId, nextStitch)
            }
        }

        fun decrementStitch() {
            val state = _uiState.value
            val projectId = state.projectId ?: return
            if (!state.stitchTrackingEnabled) return
            val nextStitch = (state.currentStitch - 1).coerceAtLeast(0)
            if (nextStitch == state.currentStitch) return
            _uiState.update { it.copy(currentStitch = nextStitch) }
            viewModelScope.launch {
                repository.updateCurrentStitch(projectId, nextStitch)
            }
        }

        fun setStitchTrackingEnabled(enabled: Boolean) {
            val state = _uiState.value
            val projectId = state.projectId ?: return
            val canEnable = enabled && (state.stitchCount ?: 0) > 0
            val shouldEnable = enabled && canEnable
            _uiState.update {
                it.copy(
                    stitchTrackingEnabled = shouldEnable,
                    currentStitch = if (shouldEnable) it.currentStitch else 0,
                )
            }
            viewModelScope.launch {
                repository.updateStitchTrackingEnabled(projectId, shouldEnable)
                if (!shouldEnable) {
                    repository.updateCurrentStitch(projectId, 0)
                }
            }
        }

        private fun persistCurrentStitchIfNeeded(shouldReset: Boolean) {
            if (!shouldReset) return
            val projectId = _uiState.value.projectId ?: return
            viewModelScope.launch {
                repository.updateCurrentStitch(projectId, 0)
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
                            id = updated.id,
                            count = updated.count,
                            currentRepeat = updated.currentRepeat,
                        )
                    }
                }
        }

        override fun onCleared() {
            val state = _uiState.value
            clearPendingSessionState()
            super.onCleared()
            @Suppress("TooGenericExceptionCaught")
            CoroutineScope(ioDispatcher + NonCancellable).launch {
                try {
                    val projectId = state.projectId ?: return@launch
                    persistSessionSnapshotIfNeeded(
                        projectId = projectId,
                        endRow = state.counter.count,
                        sessionSeconds = state.sessionSeconds,
                    )
                } catch (_: Exception) {
                    // Sessio-tallennus epäonnistui siivouksessa — ei kaadeta sovellusta
                }
            }
            selectedProjectJob?.cancel()
            ProcessLifecycleOwner.get().lifecycle.removeObserver(lifecycleObserver)
        }

        private companion object {
            const val HISTORY_LIMIT_HOURS = 24L
            const val KEY_SELECTED_PROJECT_ID = "counter.selected_project_id"
            const val KEY_SESSION_STARTED_AT = "counter.session_started_at"
            const val KEY_SESSION_START_ROW = "counter.session_start_row"
            const val KEY_SESSION_SECONDS = "counter.session_seconds"
            const val KEY_SESSION_ROWS_WORKED = "counter.session_rows_worked"
        }
    }
