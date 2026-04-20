package com.finnvek.knittools.ui.screens.counter

import android.content.Context
import android.net.Uri
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.knittools.BuildConfig
import com.finnvek.knittools.R
import com.finnvek.knittools.ai.AiQuotaManager
import com.finnvek.knittools.ai.GeminiAiService
import com.finnvek.knittools.ai.ProjectSummarizer
import com.finnvek.knittools.ai.VoiceCommandInterpreter
import com.finnvek.knittools.ai.live.LiveVoiceState
import com.finnvek.knittools.ai.live.ProjectVoiceContext
import com.finnvek.knittools.ai.live.VoiceLiveQuotaManager
import com.finnvek.knittools.ai.live.VoiceLiveSession
import com.finnvek.knittools.ai.nano.NanoAvailability
import com.finnvek.knittools.data.datastore.PreferencesManager
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.ProgressPhotoEntity
import com.finnvek.knittools.data.local.ProjectCounterEntity
import com.finnvek.knittools.data.local.RowReminderEntity
import com.finnvek.knittools.data.local.SavedPatternEntity
import com.finnvek.knittools.data.local.SessionEntity
import com.finnvek.knittools.data.local.YarnCardEntity
import com.finnvek.knittools.data.storage.PatternDocumentStorage
import com.finnvek.knittools.domain.calculator.CounterLogic
import com.finnvek.knittools.domain.calculator.CounterState
import com.finnvek.knittools.domain.calculator.ReminderLogic
import com.finnvek.knittools.domain.calculator.RepeatSectionLogic
import com.finnvek.knittools.domain.calculator.RowMarker
import com.finnvek.knittools.domain.calculator.parseMapping
import com.finnvek.knittools.domain.calculator.serializeMapping
import com.finnvek.knittools.domain.model.ProjectCounterDraft
import com.finnvek.knittools.pro.InAppReviewManager
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.repository.PatternAnnotationRepository
import com.finnvek.knittools.repository.ProgressPhotoRepository
import com.finnvek.knittools.repository.ProjectCounterRepository
import com.finnvek.knittools.repository.ReminderRepository
import com.finnvek.knittools.repository.SavedPatternRepository
import com.finnvek.knittools.repository.YarnCardRepository
import com.finnvek.knittools.widget.CounterWidget
import com.finnvek.knittools.widget.CounterWidgetState
import com.finnvek.knittools.widget.WidgetData
import com.google.firebase.ai.type.FunctionCallPart
import com.google.firebase.ai.type.FunctionResponsePart
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class CounterUiState(
    val projectName: String = "",
    val counter: CounterState = CounterState(),
    val secondaryCount: Int = 0,
    val notes: String = "",
    val sessionSeconds: Long = 0,
    val projectId: Long? = null,
    val hapticFeedback: Boolean = true,
    val keepScreenAwake: Boolean = false,
    val isPro: Boolean = false,
    val isNanoAvailable: Boolean = false,
    val projects: List<CounterProjectEntity> = emptyList(),
    val sectionName: String? = null,
    val stitchCount: Int? = null,
    val stitchTrackingEnabled: Boolean = false,
    val currentStitch: Int = 0,
    val linkedYarns: List<Pair<Long, String>> = emptyList(),
    val totalSessionMinutes: Int = 0,
    val isAiAvailable: Boolean = false,
    val projectSummary: String? = null,
    val summaryError: String? = null,
    val isSummaryLoading: Boolean = false,
    val reminders: List<RowReminderEntity> = emptyList(),
    val activeAlert: RowReminderEntity? = null,
    val projectCounters: List<ProjectCounterEntity> = emptyList(),
    val latestPhotos: List<ProgressPhotoEntity> = emptyList(),
    val linkedPattern: SavedPatternEntity? = null,
    val patternUri: String? = null,
    val patternName: String? = null,
    val currentPatternPage: Int = 0,
    val patternRowMapping: String? = null,
    val totalRows: Int? = null,
    val isLiveSessionActive: Boolean = false,
    val voiceLiveEnabled: Boolean = true,
)

@HiltViewModel
class CounterViewModel
    @Inject
    constructor(
        private val repository: CounterRepository,
        private val reminderRepository: ReminderRepository,
        private val projectCounterRepository: ProjectCounterRepository,
        private val photoRepository: ProgressPhotoRepository,
        private val patternAnnotationRepository: PatternAnnotationRepository,
        private val preferencesManager: PreferencesManager,
        private val proManager: ProManager,
        private val yarnCardRepository: YarnCardRepository,
        private val savedPatternRepository: SavedPatternRepository,
        private val patternDocumentStorage: PatternDocumentStorage,
        private val geminiAiService: GeminiAiService,
        private val aiQuotaManager: AiQuotaManager,
        private val voiceLiveSession: VoiceLiveSession,
        private val voiceLiveQuotaManager: VoiceLiveQuotaManager,
        private val inAppReviewManager: InAppReviewManager,
        private val savedStateHandle: SavedStateHandle,
        @param:ApplicationContext private val context: Context,
    ) : ViewModel() {
        private val _uiState =
            MutableStateFlow(
                CounterUiState(
                    projectName = context.getString(R.string.default_project_name),
                    sessionSeconds = savedStateHandle[KEY_SESSION_SECONDS] ?: 0L,
                ),
            )
        val uiState: StateFlow<CounterUiState> = _uiState.asStateFlow()

        val savedYarnCards: StateFlow<List<YarnCardEntity>> =
            yarnCardRepository.getAllCards().stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList(),
            )

        val savedPatterns: StateFlow<List<SavedPatternEntity>> =
            savedPatternRepository.getAll().stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList(),
            )

        private val _allPhotos = MutableStateFlow<List<ProgressPhotoEntity>>(emptyList())
        val allPhotos: StateFlow<List<ProgressPhotoEntity>> = _allPhotos.asStateFlow()

        private var timerJob: Job? = null
        private var selectedProjectJob: Job? = null
        private var reminderCollectionJob: Job? = null
        private var counterCollectionJob: Job? = null
        private var photoCollectionJob: Job? = null
        private var allPhotosJob: Job? = null
        private var linkedYarnIdsCache: String = ""
        private var isForeground = true
        private var didRecoverPendingSession = false

        // Session tracking
        private var sessionStartedAt: Long = savedStateHandle[KEY_SESSION_STARTED_AT] ?: System.currentTimeMillis()
        private var sessionStartRow: Int = savedStateHandle[KEY_SESSION_START_ROW] ?: 0

        private val lifecycleObserver =
            object : DefaultLifecycleObserver {
                override fun onResume(owner: LifecycleOwner) {
                    isForeground = true
                }

                override fun onPause(owner: LifecycleOwner) {
                    isForeground = false
                }
            }

        init {
            ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleObserver)
            loadOrCreateProject()
            startTimer()
            observePreferences()
            observeProState()
            refreshNanoAvailability()
        }

        private fun observePreferences() {
            viewModelScope.launch {
                preferencesManager.preferences.collect { prefs ->
                    _uiState.update {
                        it.copy(
                            hapticFeedback = prefs.hapticFeedback,
                            keepScreenAwake = prefs.keepScreenAwake,
                            voiceLiveEnabled = prefs.voiceLiveEnabled,
                        )
                    }
                }
            }
        }

        private fun observeProState() {
            viewModelScope.launch {
                proManager.proState.collect { proState ->
                    _uiState.update { it.copy(isPro = proState.isPro) }
                    if (!proState.isPro) {
                        _uiState.update { it.copy(isNanoAvailable = false, isAiAvailable = false) }
                        pruneHistoryForFree()
                    } else {
                        refreshNanoAvailability()
                        refreshAiAvailability()
                    }
                }
            }
        }

        fun refreshNanoAvailability() {
            viewModelScope.launch {
                val isNanoAvailable =
                    proManager.hasFeature(ProFeature.GEMINI_NANO) &&
                        NanoAvailability.isUsable()
                _uiState.update { it.copy(isNanoAvailable = isNanoAvailable) }
            }
        }

        fun refreshAiAvailability() {
            viewModelScope.launch {
                val available =
                    proManager.hasFeature(ProFeature.AI_FEATURES) &&
                        aiQuotaManager.hasQuota()
                _uiState.update { it.copy(isAiAvailable = available) }
            }
        }

        private fun loadOrCreateProject() {
            viewModelScope.launch {
                repository.getAllProjects().collect { list ->
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

        fun selectProject(project: CounterProjectEntity) {
            viewModelScope.launch {
                // Pysäytä live-sessio ennen projektin vaihtoa — konteksti muuttuu
                if (voiceLiveSession.isActive()) voiceLiveSession.stop()
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

                        _uiState.update { state ->
                            state.copy(
                                projectName = project.name,
                                counter = state.counter.copy(count = project.count),
                                secondaryCount = project.secondaryCount,
                                notes = project.notes,
                                sectionName = project.sectionName,
                                stitchCount = project.stitchCount,
                                stitchTrackingEnabled = project.stitchTrackingEnabled,
                                currentStitch = project.currentStitch,
                                patternUri = project.patternUri,
                                patternName = project.patternName,
                                currentPatternPage = project.currentPatternPage,
                                patternRowMapping = project.patternRowMapping,
                                totalRows = project.totalRows,
                            )
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
                val project = repository.getProject(id) ?: return@launch
                val currentIds = project.yarnCardIds.split(",").mapNotNull { it.trim().toLongOrNull() }
                if (cardId in currentIds) return@launch
                yarnCardRepository.getCard(cardId)?.linkedProjectId?.takeIf { it != id }?.let { previousProjectId ->
                    repository.getProject(previousProjectId)?.let { previousProject ->
                        val previousIds =
                            previousProject.yarnCardIds
                                .split(",")
                                .mapNotNull { it.trim().toLongOrNull() }
                                .filter { it != cardId }
                                .joinToString(",")
                        repository.updateProjectYarnCardIds(previousProjectId, previousIds)
                    }
                }
                val newIds = (currentIds + cardId).joinToString(",")
                repository.updateProjectYarnCardIds(id, newIds)
                yarnCardRepository.updateLinkedProjectId(cardId, id)
            }
        }

        fun unlinkYarnCard(cardId: Long) {
            viewModelScope.launch {
                val id = _uiState.value.projectId ?: return@launch
                val project = repository.getProject(id) ?: return@launch
                val currentIds = project.yarnCardIds.split(",").mapNotNull { it.trim().toLongOrNull() }
                val newIds = currentIds.filter { it != cardId }.joinToString(",")
                repository.updateProjectYarnCardIds(id, newIds)
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
            val ids = yarnCardIds.split(",").mapNotNull { it.trim().toLongOrNull() }
            val cardsById = yarnCardRepository.getCards(ids).associateBy { it.id }
            val yarns =
                ids.mapNotNull { id ->
                    cardsById[id]?.let { card ->
                        val name =
                            listOfNotNull(
                                card.brand.takeIf { it.isNotBlank() },
                                card.yarnName.takeIf { it.isNotBlank() },
                            ).joinToString(" ")
                                .ifEmpty { "Yarn #$id" }
                        id to name
                    }
                }
            _uiState.update { it.copy(linkedYarns = yarns) }
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
        ) {
            val durationMinutes = (sessionSeconds / 60).toInt()
            if (durationMinutes < 1 && endRow == sessionStartRow) return

            repository.insertSession(
                SessionEntity(
                    projectId = projectId,
                    startedAt = sessionStartedAt,
                    endedAt = System.currentTimeMillis(),
                    startRow = sessionStartRow,
                    endRow = endRow,
                    durationMinutes = maxOf(1, durationMinutes),
                ),
            )
        }

        private suspend fun recoverPendingSessionIfNeeded(projects: List<CounterProjectEntity>) {
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

        private suspend fun startProjectSession(project: CounterProjectEntity) {
            sessionStartedAt = System.currentTimeMillis()
            sessionStartRow = project.count
            linkedYarnIdsCache = project.yarnCardIds

            saveSelectedProject(project.id)
            savePendingSessionState(project.id, 0L)

            _uiState.update {
                it.copy(
                    projectId = project.id,
                    projectName = project.name,
                    counter = CounterState(count = project.count, stepSize = project.stepSize),
                    secondaryCount = project.secondaryCount,
                    notes = project.notes,
                    sectionName = project.sectionName,
                    stitchCount = project.stitchCount,
                    stitchTrackingEnabled = project.stitchTrackingEnabled,
                    currentStitch = project.currentStitch,
                    patternUri = project.patternUri,
                    patternName = project.patternName,
                    currentPatternPage = project.currentPatternPage,
                    patternRowMapping = project.patternRowMapping,
                    sessionSeconds = 0,
                )
            }
            observeSelectedProject(project.id)
            observeReminders(project.id)
            observeProjectCounters(project.id)
            observeLatestPhotos(project.id)
            syncWidget()
            loadLinkedYarnNames(project.yarnCardIds)
            loadLinkedPattern(project.linkedPatternId)
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
        }

        private fun clearPendingSessionState() {
            savedStateHandle.remove<Long>(KEY_SESSION_STARTED_AT)
            savedStateHandle.remove<Int>(KEY_SESSION_START_ROW)
            savedStateHandle.remove<Long>(KEY_SESSION_SECONDS)
        }

        fun createNewProject(name: String): Boolean {
            if (!proManager.hasFeature(ProFeature.UNLIMITED_PROJECTS)) {
                val count = _uiState.value.projects.size
                if (count >= 1) return false
            }
            viewModelScope.launch {
                val id = repository.createProject(name)
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
            val resetStitch = state.stitchTrackingEnabled && updatedCounter.count != state.counter.count
            _uiState.update {
                it.copy(
                    counter = updatedCounter,
                    currentStitch = if (resetStitch) 0 else it.currentStitch,
                )
            }
            recomputeActiveAlert(updatedCounter.count)
            syncRepeatSectionCounters(updatedCounter.count, state.projectCounters, persist = true)
            persistCurrentStitchIfNeeded(resetStitch)
            persistCount(
                action = "increment",
                previousValue = state.counter.count,
                newValue = updatedCounter.count,
                stepSize = updatedCounter.stepSize,
                delta = updatedCounter.stepSize,
            )
        }

        fun decrement() {
            val state = _uiState.value
            state.projectId ?: return
            val updatedCounter = CounterLogic.decrement(state.counter)
            val resetStitch = state.stitchTrackingEnabled && updatedCounter.count != state.counter.count
            _uiState.update {
                it.copy(
                    counter = updatedCounter,
                    currentStitch = if (resetStitch) 0 else it.currentStitch,
                )
            }
            recomputeActiveAlert(updatedCounter.count)
            syncRepeatSectionCounters(updatedCounter.count, state.projectCounters, persist = true)
            persistCurrentStitchIfNeeded(resetStitch)
            persistCount(
                action = "decrement",
                previousValue = state.counter.count,
                newValue = updatedCounter.count,
                stepSize = updatedCounter.stepSize,
                delta = -updatedCounter.stepSize,
            )
        }

        fun undo() {
            val state = _uiState.value
            state.projectId ?: return
            val previousValue = state.counter.previousCount ?: return
            val updatedCounter = CounterLogic.undo(state.counter)
            val resetStitch = state.stitchTrackingEnabled && updatedCounter.count != state.counter.count
            _uiState.update {
                it.copy(
                    counter = updatedCounter,
                    currentStitch = if (resetStitch) 0 else it.currentStitch,
                )
            }
            recomputeActiveAlert(updatedCounter.count)
            syncRepeatSectionCounters(updatedCounter.count, state.projectCounters, persist = true)
            persistCurrentStitchIfNeeded(resetStitch)
            persistCount(
                action = "undo",
                previousValue = state.counter.count,
                newValue = previousValue,
                stepSize = updatedCounter.stepSize,
            )
        }

        fun reset() {
            val state = _uiState.value
            state.projectId ?: return
            val updatedCounter = CounterLogic.reset(state.counter)
            val resetStitch = state.stitchTrackingEnabled && updatedCounter.count != state.counter.count
            _uiState.update {
                it.copy(
                    counter = updatedCounter,
                    currentStitch = if (resetStitch) 0 else it.currentStitch,
                )
            }
            syncRepeatSectionCounters(updatedCounter.count, state.projectCounters, persist = true)
            persistCurrentStitchIfNeeded(resetStitch)
            persistCount(
                action = "reset",
                previousValue = state.counter.count,
                newValue = updatedCounter.count,
                stepSize = updatedCounter.stepSize,
            )
        }

        fun incrementSecondary() {
            _uiState.update { it.copy(secondaryCount = it.secondaryCount + 1) }
            persistSecondary()
        }

        fun decrementSecondary() {
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
                        syncRepeatSectionCounters(
                            mainRowCount = _uiState.value.counter.count,
                            counters = counters,
                            persist = true,
                        )
                    }
                }
        }

        fun addProjectCounter(draft: ProjectCounterDraft) {
            if (!proManager.hasFeature(ProFeature.MULTIPLE_COUNTERS)) return
            viewModelScope.launch {
                val projectId = _uiState.value.projectId ?: return@launch
                val counter =
                    ProjectCounterEntity(
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
                    )
                val initialCounter =
                    if (draft.counterType == "REPEAT_SECTION") {
                        RepeatSectionLogic.updatePosition(counter, _uiState.value.counter.count)
                    } else {
                        counter
                    }
                projectCounterRepository.addCounter(initialCounter)
            }
        }

        fun incrementProjectCounter(counter: ProjectCounterEntity) {
            viewModelScope.launch {
                projectCounterRepository.incrementCounter(counter)
            }
        }

        fun decrementProjectCounter(counter: ProjectCounterEntity) {
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
                        val currentRow = _uiState.value.counter.count
                        val active = ReminderLogic.activeReminders(reminders, currentRow).firstOrNull()
                        _uiState.update {
                            it.copy(reminders = reminders, activeAlert = active)
                        }
                    }
                }
        }

        private fun recomputeActiveAlert(currentRow: Int) {
            val active = ReminderLogic.activeReminders(_uiState.value.reminders, currentRow).firstOrNull()
            _uiState.update { it.copy(activeAlert = active) }
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
                    RowReminderEntity(
                        projectId = projectId,
                        targetRow = targetRow,
                        repeatInterval = repeatInterval,
                        message = message.take(200),
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
                _uiState.update { it.copy(activeAlert = null) }
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

        fun deletePhoto(photo: ProgressPhotoEntity) {
            viewModelScope.launch {
                photoRepository.deletePhoto(photo)
            }
        }

        fun setNotes(notes: String) {
            if (!proManager.hasFeature(ProFeature.NOTES)) return
            _uiState.update { it.copy(notes = notes) }
            viewModelScope.launch {
                val id = _uiState.value.projectId ?: return@launch
                repository.updateProjectNotes(id, notes)
            }
        }

        fun setProjectName(name: String) {
            _uiState.update { it.copy(projectName = name) }
            viewModelScope.launch {
                val id = _uiState.value.projectId ?: return@launch
                repository.updateProjectName(id, name)
                syncWidget()
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
                // Kopioi PDF sisäiseen tallennustilaan — estää permission-ongelmat
                val internalUri =
                    patternDocumentStorage.copyPdfToInternal(
                        context = context,
                        projectId = projectId,
                        sourceUri = Uri.parse(uri),
                        fileName = sanitizedName,
                    ) ?: uri

                _uiState.update {
                    it.copy(
                        patternUri = internalUri,
                        patternName = sanitizedName,
                        currentPatternPage = 0,
                        patternRowMapping = null,
                    )
                }
                persistImportedPatternIfNeeded(internalUri, sanitizedName)
                patternAnnotationRepository.clearProject(projectId)
                repository.updatePattern(
                    id = projectId,
                    patternUri = internalUri,
                    patternName = sanitizedName,
                    currentPatternPage = 0,
                    patternRowMapping = null,
                )
            }
        }

        private suspend fun persistImportedPatternIfNeeded(
            uri: String,
            name: String,
        ) {
            savedPatternRepository.saveImportedPatternIfMissing(uri, name)
        }

        fun detachPattern() {
            val projectId = _uiState.value.projectId ?: return
            _uiState.update {
                it.copy(
                    patternUri = null,
                    patternName = null,
                    currentPatternPage = 0,
                    patternRowMapping = null,
                )
            }
            viewModelScope.launch {
                patternAnnotationRepository.clearProject(projectId)
                repository.updatePattern(
                    id = projectId,
                    patternUri = null,
                    patternName = null,
                    currentPatternPage = 0,
                    patternRowMapping = null,
                )
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
            stepSize: Int,
            delta: Int? = null,
        ) {
            val state = _uiState.value
            val projectId = state.projectId ?: return
            viewModelScope.launch {
                inAppReviewManager.recordAction()
                if (delta != null) {
                    repository.adjustProjectCountWithHistory(
                        id = projectId,
                        delta = delta,
                        stepSize = stepSize,
                        action = action,
                        previousValue = previousValue,
                        newValue = newValue,
                    )
                } else {
                    repository.updateProjectCounterStateWithHistory(
                        id = projectId,
                        count = newValue,
                        stepSize = stepSize,
                        action = action,
                        previousValue = previousValue,
                        newValue = newValue,
                    )
                }
                savePendingSessionState(projectId, state.sessionSeconds)
                syncWidget(projectId, state.projectName, newValue)
            }
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
                    targetRows = state.totalRows?.takeIf { it > 0 },
                    sectionName = state.sectionName?.takeIf { it.isNotBlank() },
                    currentStitch = state.currentStitch,
                    totalStitches = state.stitchCount?.takeIf { it > 0 },
                    stitchTrackingEnabled = state.stitchTrackingEnabled,
                )
            CounterWidgetState.save(context, widgetData)
            val widget = CounterWidget()
            val manager = androidx.glance.appwidget.GlanceAppWidgetManager(context)
            manager.getGlanceIds(CounterWidget::class.java).forEach { id ->
                widget.update(context, id)
            }
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
            viewModelScope.launch {
                repository.getProject(id)?.let {
                    persistCurrentSessionIfNeeded()
                    startProjectSession(it)
                }
            }
        }

        fun generateSummary() {
            viewModelScope.launch {
                val state = _uiState.value
                val projectId = state.projectId ?: return@launch
                _uiState.update { it.copy(isSummaryLoading = true, projectSummary = null, summaryError = null) }

                val sessionCount =
                    repository
                        .getSessionsForProject(projectId)
                        .first()
                        .size
                val totalMinutes = repository.getTotalMinutesForProject(projectId)
                val avgRows = if (sessionCount > 0) state.counter.count.toDouble() / sessionCount else 0.0
                val createdAt = state.projects.find { it.id == projectId }?.createdAt ?: System.currentTimeMillis()
                val daysActive = ((System.currentTimeMillis() - createdAt) / 86_400_000).toInt().coerceAtLeast(1)
                val counterSummary =
                    state.projectCounters
                        .takeIf { it.isNotEmpty() }
                        ?.joinToString(", ") { "${it.name}: ${it.count}" }

                val lastSession = repository.getLatestSession(projectId)
                val hoursSinceLastSession =
                    lastSession?.let {
                        (System.currentTimeMillis() - it.endedAt) / 3_600_000
                    }

                val yarnDetailedInfo =
                    state.linkedYarns.firstOrNull()?.first?.let { yarnId ->
                        yarnCardRepository.getCard(yarnId)?.let { card ->
                            buildString {
                                append("${card.brand} ${card.yarnName}".trim())
                                card.weightCategory.takeIf { it.isNotBlank() }?.let { append(", $it") }
                                card.fiberContent.takeIf { it.isNotBlank() }?.let { append(", $it") }
                                if (card.lengthMeters.isNotBlank() && card.weightGrams.isNotBlank()) {
                                    append(", ${card.lengthMeters}m/${card.weightGrams}g")
                                }
                                card.needleSize.takeIf { it.isNotBlank() }?.let { append(", needle $it") }
                                card.gaugeInfo.takeIf { it.isNotBlank() }?.let { append(", gauge $it") }
                            }
                        }
                    }

                val data =
                    ProjectSummarizer.ProjectData(
                        name = state.projectName,
                        currentRow = state.counter.count,
                        patternName = state.patternName,
                        yarnInfo =
                            state.linkedYarns
                                .map { it.second }
                                .takeIf { it.isNotEmpty() }
                                ?.joinToString(", "),
                        yarnDetailedInfo = yarnDetailedInfo,
                        totalSessionMinutes = totalMinutes,
                        sessionCount = sessionCount,
                        averageRowsPerSession = avgRows,
                        stitchCount = state.stitchCount,
                        notes = state.notes,
                        daysActive = daysActive,
                        counterSummary = counterSummary,
                        hoursSinceLastSession = hoursSinceLastSession,
                        lastSessionEndRow = lastSession?.endRow,
                    )

                if (!aiQuotaManager.hasQuota()) {
                    _uiState.update {
                        it.copy(
                            projectSummary = null,
                            summaryError = context.getString(R.string.ai_quota_exhausted),
                            isSummaryLoading = false,
                        )
                    }
                    return@launch
                }

                val language =
                    preferencesManager.preferences
                        .first()
                        .appLanguage
                        .promptLanguageName()
                val aiSummary = ProjectSummarizer.summarize(geminiAiService, data, language)
                if (aiSummary != null) {
                    aiQuotaManager.recordCall()
                    _uiState.update {
                        it.copy(projectSummary = aiSummary, isSummaryLoading = false)
                    }
                    refreshAiAvailability()
                } else {
                    // Gemini-kutsu epäonnistui — fallback
                    val fallback = ProjectSummarizer.simpleSummary(data)
                    _uiState.update {
                        it.copy(
                            projectSummary = fallback,
                            summaryError = context.getString(R.string.ai_summary_fallback),
                            isSummaryLoading = false,
                        )
                    }
                }
            }
        }

        fun clearSummary() {
            _uiState.update { it.copy(projectSummary = null, summaryError = null) }
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
            counters: List<ProjectCounterEntity>,
            persist: Boolean,
        ) {
            val syncedCounters =
                counters.map { counter ->
                    if (counter.counterType == "REPEAT_SECTION") {
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

        // === Äänikomennot v2: AI-tulkinta, TTS, cache ===

        private val _voiceResponse = MutableSharedFlow<String>(extraBufferCapacity = 1)
        val voiceResponse: SharedFlow<String> = _voiceResponse.asSharedFlow()

        // Signaali CounterScreenille: käynnistä v2 continuous mode fallbackina.
        // Kantaa valinnaisen virheviestin snackbaria varten.
        private val _fallbackToV2 = MutableSharedFlow<String?>(extraBufferCapacity = 1)
        val fallbackToV2: SharedFlow<String?> = _fallbackToV2.asSharedFlow()

        // Välimuisti: viimeiset 10 AI-tulkittua komentoa (session-elinaika)
        private val voiceCommandCache =
            object : LinkedHashMap<String, AiVoiceAction>(10, 0.75f, true) {
                override fun removeEldestEntry(eldest: Map.Entry<String, AiVoiceAction>): Boolean = size > 10
            }
        private var lastRecognizedText: String = ""
        private var lastRecognizedTimestamp: Long = 0L

        // Offline-vihje näytetään kerran per session-elinaika
        private var hasShownOfflineHint: Boolean = false

        /**
         * Paikallisesti tunnistetun komennon vahvistus TTS:lle.
         * Vain ei-triviaalit komennot puhuvat — +1/−1 pysyy hiljaa (haptic riittää).
         */
        fun emitLocalVoiceFeedback(command: VoiceCommand) {
            val response =
                when (command) {
                    is VoiceCommand.Increment -> {
                        if (command.count > 1) {
                            context.getString(R.string.voice_row_current, _uiState.value.counter.count)
                        } else {
                            null
                        }
                    }

                    is VoiceCommand.Decrement -> {
                        if (command.count > 1) {
                            context.getString(R.string.voice_back_to_row, _uiState.value.counter.count)
                        } else {
                            null
                        }
                    }

                    VoiceCommand.Undo -> {
                        context.getString(R.string.voice_undone_row, _uiState.value.counter.count)
                    }

                    VoiceCommand.Reset -> {
                        context.getString(R.string.voice_counter_reset)
                    }

                    VoiceCommand.StitchIncrement,
                    VoiceCommand.StitchDecrement,
                    -> {
                        if (_uiState.value.stitchTrackingEnabled) {
                            context.getString(R.string.voice_stitch_at, _uiState.value.currentStitch)
                        } else {
                            null
                        }
                    }

                    VoiceCommand.Help -> {
                        context.getString(R.string.voice_help_full)
                    }

                    VoiceCommand.StopListening -> {
                        null
                    }
                }
            response?.let { _voiceResponse.tryEmit(it) }
        }

        fun interpretVoiceCommand(recognizedText: String) {
            viewModelScope.launch {
                val normalizedText = recognizedText.lowercase().trim().replace(Regex("\\s+"), " ")

                // Deduplikaatio: 3s ikkuna
                val now = android.os.SystemClock.elapsedRealtime()
                if (normalizedText == lastRecognizedText && now - lastRecognizedTimestamp < 3_000) return@launch
                lastRecognizedText = normalizedText
                lastRecognizedTimestamp = now

                // Cache-haku
                voiceCommandCache[normalizedText]?.let { cachedAction ->
                    executeVoiceAction(cachedAction)
                    return@launch
                }

                // Kiintiötarkistus
                if (!aiQuotaManager.hasVoiceQuota()) {
                    _voiceResponse.tryEmit(context.getString(R.string.voice_quota_daily_exhausted))
                    return@launch
                }

                // AI-tulkinta
                val state = _uiState.value
                val projectContext =
                    VoiceCommandInterpreter.ProjectContext(
                        projectName = state.projectName,
                        currentRow = state.counter.count,
                        targetRows = state.totalRows,
                        stitchTrackingEnabled = state.stitchTrackingEnabled,
                        currentStitch = state.currentStitch,
                        totalStitches = state.stitchCount,
                        activeCounters =
                            state.projectCounters.map {
                                VoiceCommandInterpreter.CounterInfo(
                                    name = it.name,
                                    type = it.counterType,
                                    currentCount = it.count,
                                )
                            },
                        sessionSeconds = state.sessionSeconds,
                        linkedYarnNames = state.linkedYarns.map { it.second },
                        patternName = state.patternName ?: state.linkedPattern?.name,
                        shapingCounters =
                            state.projectCounters
                                .filter { it.counterType == "SHAPING" }
                                .map {
                                    VoiceCommandInterpreter.ShapingInfo(
                                        name = it.name,
                                        currentCount = it.count,
                                        shapeEveryN = it.shapeEveryN,
                                    )
                                },
                    )

                val action =
                    VoiceCommandInterpreter.interpret(
                        geminiAiService,
                        recognizedText,
                        projectContext,
                        java.util.Locale
                            .getDefault()
                            .toLanguageTag(),
                    )

                aiQuotaManager.recordVoiceCall()
                // Älä tallenna epäonnistuneita tuloksia välimuistiin
                if (action != AiVoiceAction.Unknown) {
                    voiceCommandCache[normalizedText] = action
                }
                val response = executeVoiceAction(action)
                _voiceResponse.tryEmit(response)
            }
        }

        // — Live API (v3) —

        fun startLiveVoice() {
            if (voiceLiveSession.isActive()) return
            viewModelScope.launch {
                if (!canStartLiveVoice()) return@launch
                _uiState.update { it.copy(isLiveSessionActive = true) }

                // Seuraa session-tilan muutoksia ENNEN start()-kutsua,
                // koska start() suspendaa koko audiokeskustelun ajan
                val stateJob = observeLiveVoiceState()

                // Suspendaa koko audiokeskustelun ajan
                voiceLiveSession.start(buildProjectVoiceContext(), buildFunctionCallHandler())

                // Sessio päättyi normaalisti
                stateJob.cancel()
                _uiState.update { it.copy(isLiveSessionActive = false) }
            }
        }

        private fun observeLiveVoiceState(): Job =
            viewModelScope.launch {
                voiceLiveSession.state.collect(::handleLiveVoiceState)
            }

        private suspend fun canStartLiveVoice(): Boolean {
            if (!BuildConfig.DEBUG && !proManager.hasFeature(ProFeature.VOICE_LIVE)) {
                _fallbackToV2.tryEmit(null)
                return false
            }
            if (!isOnline()) {
                _fallbackToV2.tryEmit(offlineHintMessageOrNull())
                return false
            }
            if (!voiceLiveQuotaManager.hasQuota()) {
                _fallbackToV2.tryEmit(context.getString(R.string.voice_live_quota_exhausted))
                return false
            }
            return true
        }

        /** Palauttaa offline-vihjeen vain ensimmäisellä kerralla per sessio */
        private fun offlineHintMessageOrNull(): String? {
            if (hasShownOfflineHint) return null
            hasShownOfflineHint = true
            return context.getString(R.string.voice_offline_mode)
        }

        private fun handleLiveVoiceState(liveState: LiveVoiceState) {
            when (liveState) {
                LiveVoiceState.ERROR -> {
                    _uiState.update { it.copy(isLiveSessionActive = false) }
                    _fallbackToV2.tryEmit(buildLiveVoiceErrorMessage())
                }

                LiveVoiceState.IDLE -> {
                    _uiState.update { it.copy(isLiveSessionActive = false) }
                }

                LiveVoiceState.CONNECTING,
                LiveVoiceState.ACTIVE,
                -> {
                    Unit
                }
            }
        }

        private fun buildLiveVoiceErrorMessage(): String? {
            val detail = voiceLiveSession.lastError.orEmpty()
            val isNetworkError =
                detail.contains("UnknownHostException", ignoreCase = true) ||
                    detail.contains("ConnectException", ignoreCase = true) ||
                    detail.contains("SocketTimeout", ignoreCase = true) ||
                    detail.contains("IOException", ignoreCase = true) ||
                    detail.contains("Unable to resolve", ignoreCase = true)
            return if (isNetworkError) {
                offlineHintMessageOrNull()
            } else {
                context.getString(R.string.voice_live_error)
            }
        }

        fun stopLiveVoice() {
            viewModelScope.launch {
                voiceLiveSession.stop()
                _uiState.update { it.copy(isLiveSessionActive = false) }
            }
        }

        private fun isOnline(): Boolean {
            val cm = context.getSystemService(android.net.ConnectivityManager::class.java) ?: return false
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            // VALIDATED varmistaa että yhteys oikeasti toimii (captive portal / ei-dataa -tilanteet hylätään)
            return caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }

        private fun buildProjectVoiceContext(): ProjectVoiceContext {
            val state = _uiState.value
            return ProjectVoiceContext(
                projectName = state.projectName,
                currentRow = state.counter.count,
                targetRows = state.totalRows,
                sectionName = state.sectionName,
                stitchTrackingEnabled = state.stitchTrackingEnabled,
                currentStitch = state.currentStitch,
                totalStitches = state.stitchCount,
                activeCounters =
                    state.projectCounters.map {
                        ProjectVoiceContext.CounterSummary(it.name, it.counterType, it.count)
                    },
                sessionMinutes = state.sessionSeconds / 60,
                totalSessionMinutes = state.totalSessionMinutes,
                linkedYarnNames = state.linkedYarns.map { it.second },
                patternName = state.patternName ?: state.linkedPattern?.name,
                currentPatternPage = state.currentPatternPage,
                reminders =
                    state.reminders.filter { !it.isCompleted }.map {
                        ProjectVoiceContext.ReminderSummary(it.targetRow, it.message)
                    },
                notes = state.notes,
            )
        }

        @Suppress("CyclomaticComplexMethod") // Dispatch map — jokainen haara on yksinkertainen
        private fun buildFunctionCallHandler(): (FunctionCallPart) -> FunctionResponsePart =
            { functionCall ->
                val args = functionCall.args
                val action =
                    when (functionCall.name) {
                        "increment" -> {
                            AiVoiceAction.Increment(args["count"]?.jsonPrimitive?.intOrNull ?: 1)
                        }

                        "decrement" -> {
                            AiVoiceAction.Decrement(args["count"]?.jsonPrimitive?.intOrNull ?: 1)
                        }

                        "undo" -> {
                            AiVoiceAction.Undo
                        }

                        "reset" -> {
                            AiVoiceAction.Reset
                        }

                        "stitch_increment" -> {
                            AiVoiceAction.StitchIncrement
                        }

                        "stitch_decrement" -> {
                            AiVoiceAction.StitchDecrement
                        }

                        "add_note" -> {
                            AiVoiceAction.AddNote(args["text"]?.jsonPrimitive?.content ?: "")
                        }

                        "dismiss_reminder" -> {
                            AiVoiceAction.DismissReminder
                        }

                        "add_reminder" -> {
                            AiVoiceAction.AddReminder(
                                row = args["row"]?.jsonPrimitive?.intOrNull ?: 0,
                                message = args["message"]?.jsonPrimitive?.content ?: "",
                            )
                        }

                        "counter_change" -> {
                            val name = args["name"]?.jsonPrimitive?.content ?: ""
                            when (args["operation"]?.jsonPrimitive?.content) {
                                "increment" -> AiVoiceAction.IncrementCounter(name)
                                "decrement" -> AiVoiceAction.DecrementCounter(name)
                                "reset" -> AiVoiceAction.ResetCounter(name)
                                else -> AiVoiceAction.Unknown
                            }
                        }

                        "set_section" -> {
                            AiVoiceAction.SetSection(args["name"]?.jsonPrimitive?.content ?: "")
                        }

                        "configure_counter" -> {
                            val value = args["value"]?.jsonPrimitive?.content ?: ""
                            when (args["setting"]?.jsonPrimitive?.content) {
                                "step_size" -> {
                                    AiVoiceAction.SetStepSize(value.toIntOrNull() ?: 1)
                                }

                                "stitch_count" -> {
                                    AiVoiceAction.SetStitchCount(value.toIntOrNull() ?: 0)
                                }

                                "stitch_tracking" -> {
                                    AiVoiceAction.ToggleStitchTracking(value.toBooleanStrictOrNull() ?: false)
                                }

                                else -> {
                                    AiVoiceAction.Unknown
                                }
                            }
                        }

                        "page_navigation" -> {
                            when (args["direction"]?.jsonPrimitive?.content) {
                                "next" -> AiVoiceAction.NextPage
                                "previous" -> AiVoiceAction.PreviousPage
                                "goto" -> AiVoiceAction.GoToPage(args["page"]?.jsonPrimitive?.intOrNull ?: 1)
                                else -> AiVoiceAction.Unknown
                            }
                        }

                        "complete_project" -> {
                            AiVoiceAction.CompleteProject
                        }

                        "generate_summary" -> {
                            AiVoiceAction.GenerateSummary
                        }

                        "query_project" -> {
                            when (args["topic"]?.jsonPrimitive?.content) {
                                "progress" -> AiVoiceAction.QueryProgress
                                "remaining" -> AiVoiceAction.QueryRemaining
                                "session_time" -> AiVoiceAction.QuerySessionTime
                                "total_time" -> AiVoiceAction.QueryTotalTime
                                "yarn" -> AiVoiceAction.QueryYarn
                                "instruction" -> AiVoiceAction.QueryInstruction
                                "shaping" -> AiVoiceAction.QueryShaping
                                "stitches" -> AiVoiceAction.QueryStitches
                                "reminders" -> AiVoiceAction.QueryReminders
                                "counters" -> AiVoiceAction.QueryCounters
                                "notes" -> AiVoiceAction.QueryNotes
                                "summary" -> AiVoiceAction.QuerySummary
                                "project" -> AiVoiceAction.QueryProject
                                "section" -> AiVoiceAction.QuerySection
                                else -> AiVoiceAction.Unknown
                            }
                        }

                        "help" -> {
                            AiVoiceAction.Help
                        }

                        else -> {
                            AiVoiceAction.Unknown
                        }
                    }
                val response = executeVoiceAction(action)
                FunctionResponsePart(
                    functionCall.name,
                    JsonObject(mapOf("response" to JsonPrimitive(response))),
                    functionCall.id,
                )
            }

        /**
         * Suorittaa äänikomennon ja palauttaa vastausviestin.
         * V2 kutsuu tätä interpretVoiceCommand()-metodista,
         * V3 (Live API) kutsuu buildFunctionCallHandler()-kautta.
         */
        internal fun executeVoiceAction(action: AiVoiceAction): String {
            val state = _uiState.value
            return if (isVoiceQueryAction(action)) {
                executeVoiceQueryAction(action, state)
            } else {
                executeVoiceCommandAction(action, state)
            }
        }

        private fun isVoiceQueryAction(action: AiVoiceAction): Boolean =
            when (action) {
                is AiVoiceAction.QueryProgress,
                is AiVoiceAction.QueryRemaining,
                is AiVoiceAction.QuerySessionTime,
                is AiVoiceAction.QueryTotalTime,
                is AiVoiceAction.QueryYarn,
                is AiVoiceAction.QueryInstruction,
                is AiVoiceAction.QueryShaping,
                is AiVoiceAction.QueryStitches,
                is AiVoiceAction.QueryReminders,
                is AiVoiceAction.QueryCounters,
                is AiVoiceAction.QueryNotes,
                is AiVoiceAction.QuerySummary,
                is AiVoiceAction.QueryProject,
                is AiVoiceAction.QuerySection,
                -> true

                else -> false
            }

        private fun executeVoiceCommandAction(
            action: AiVoiceAction,
            state: CounterUiState,
        ): String =
            when (action) {
                is AiVoiceAction.Increment -> {
                    handleVoiceIncrement(action)
                }

                is AiVoiceAction.Decrement -> {
                    handleVoiceDecrement(action)
                }

                is AiVoiceAction.Undo -> {
                    undo()
                    context.getString(R.string.voice_undone_row, _uiState.value.counter.count)
                }

                is AiVoiceAction.Reset -> {
                    context.getString(R.string.voice_counter_reset)
                }

                is AiVoiceAction.AddNote -> {
                    handleVoiceAddNote(state, action)
                }

                is AiVoiceAction.StitchIncrement -> {
                    handleVoiceStitch(state, increment = true)
                }

                is AiVoiceAction.StitchDecrement -> {
                    handleVoiceStitch(state, increment = false)
                }

                is AiVoiceAction.Help -> {
                    context.getString(R.string.voice_help_full)
                }

                is AiVoiceAction.DismissReminder -> {
                    handleVoiceDismissReminder(state)
                }

                is AiVoiceAction.IncrementCounter -> {
                    handleVoiceCounterChange(state, action.name, delta = 1)
                }

                is AiVoiceAction.DecrementCounter -> {
                    handleVoiceCounterChange(state, action.name, delta = -1)
                }

                is AiVoiceAction.ResetCounter -> {
                    handleVoiceResetCounter(state, action.name)
                }

                is AiVoiceAction.SetSection -> {
                    setSectionName(action.name)
                    context.getString(R.string.voice_section_set, action.name)
                }

                is AiVoiceAction.SetStepSize -> {
                    setStepSize(action.size)
                    context.getString(R.string.voice_step_size_set, action.size)
                }

                is AiVoiceAction.SetStitchCount -> {
                    setStitchCount(action.count)
                    context.getString(R.string.voice_stitch_count_set, action.count)
                }

                is AiVoiceAction.ToggleStitchTracking -> {
                    setStitchTrackingEnabled(action.enabled)
                    if (action.enabled) {
                        context.getString(
                            R.string.voice_stitch_tracking_on,
                        )
                    } else {
                        context.getString(R.string.voice_stitch_tracking_off)
                    }
                }

                is AiVoiceAction.NextPage -> {
                    handleVoicePageNav(state) {
                        updatePatternPage(state.currentPatternPage + 1)
                        ; context.getString(
                            R.string.voice_page_current,
                            _uiState.value.currentPatternPage + 1,
                        )
                    }
                }

                is AiVoiceAction.PreviousPage -> {
                    handleVoicePreviousPage(state)
                }

                is AiVoiceAction.GoToPage -> {
                    handleVoicePageNav(state) {
                        updatePatternPage(action.page - 1)
                        context.getString(R.string.voice_page_current, action.page)
                    }
                }

                is AiVoiceAction.CompleteProject -> {
                    completeProject()
                    context.getString(R.string.voice_project_completed, state.projectName)
                }

                is AiVoiceAction.GenerateSummary -> {
                    if (state.isAiAvailable) {
                        generateSummary()
                        context.getString(R.string.voice_summary_generating)
                    } else {
                        context.getString(R.string.voice_summary_unavailable)
                    }
                }

                is AiVoiceAction.AddReminder -> {
                    addReminder(action.row, null, action.message)
                    context.getString(R.string.voice_reminder_added, action.message, action.row)
                }

                is AiVoiceAction.Unknown -> {
                    context.getString(R.string.voice_command_unknown)
                }

                else -> {
                    error("Kyselykomento ohjattiin väärään käsittelijään: $action")
                }
            }

        private fun executeVoiceQueryAction(
            action: AiVoiceAction,
            state: CounterUiState,
        ): String =
            when (action) {
                is AiVoiceAction.QueryProgress -> {
                    voiceQueryProgress(state)
                }

                is AiVoiceAction.QueryRemaining -> {
                    voiceQueryRemaining(state)
                }

                is AiVoiceAction.QuerySessionTime -> {
                    voiceQuerySessionTime(state)
                }

                is AiVoiceAction.QueryTotalTime -> {
                    voiceQueryTotalTime(state)
                }

                is AiVoiceAction.QueryYarn -> {
                    voiceQueryYarn(state)
                }

                is AiVoiceAction.QueryInstruction -> {
                    voiceQueryInstruction(state)
                }

                is AiVoiceAction.QueryShaping -> {
                    voiceQueryShaping(state)
                }

                is AiVoiceAction.QueryStitches -> {
                    voiceQueryStitches(state)
                }

                is AiVoiceAction.QueryReminders -> {
                    voiceQueryReminders(state)
                }

                is AiVoiceAction.QueryCounters -> {
                    voiceQueryCounters(state)
                }

                is AiVoiceAction.QueryNotes -> {
                    voiceQueryNotes(state)
                }

                is AiVoiceAction.QuerySummary -> {
                    state.projectSummary?.takeIf { it.isNotBlank() }
                        ?: context.getString(R.string.voice_summary_none)
                }

                is AiVoiceAction.QueryProject -> {
                    context.getString(R.string.voice_project_name, state.projectName)
                }

                is AiVoiceAction.QuerySection -> {
                    if (!state.sectionName.isNullOrBlank()) {
                        context.getString(
                            R.string.voice_section_current,
                            state.sectionName,
                        )
                    } else {
                        context.getString(R.string.voice_section_none)
                    }
                }

                else -> {
                    error("Toimintokomento ohjattiin väärään kyselykäsittelijään: $action")
                }
            }

        // — Voice action helpers —

        private fun handleVoiceIncrement(action: AiVoiceAction.Increment): String {
            repeat(action.count) { increment() }
            return context.getString(R.string.voice_row_current, _uiState.value.counter.count)
        }

        private fun handleVoiceDecrement(action: AiVoiceAction.Decrement): String {
            repeat(action.count) { decrement() }
            return context.getString(R.string.voice_back_to_row, _uiState.value.counter.count)
        }

        private fun handleVoiceAddNote(
            state: CounterUiState,
            action: AiVoiceAction.AddNote,
        ): String {
            val separator = if (state.notes.isNotBlank()) "\n" else ""
            setNotes(state.notes + separator + action.text)
            return context.getString(R.string.voice_note_saved)
        }

        private fun handleVoiceStitch(
            state: CounterUiState,
            increment: Boolean,
        ): String {
            if (!state.stitchTrackingEnabled) return context.getString(R.string.voice_stitches_disabled)
            if (increment) incrementStitch() else decrementStitch()
            return context.getString(R.string.voice_stitch_at, _uiState.value.currentStitch)
        }

        private fun handleVoiceDismissReminder(state: CounterUiState): String {
            val alert = state.activeAlert ?: return context.getString(R.string.voice_reminder_no_active)
            dismissReminder(alert.id)
            return context.getString(R.string.voice_reminder_dismissed)
        }

        private fun handleVoiceCounterChange(
            state: CounterUiState,
            name: String,
            delta: Int,
        ): String {
            val counter =
                state.projectCounters.firstOrNull { it.name.equals(name, ignoreCase = true) }
                    ?: return context.getString(R.string.voice_counter_not_found, name)
            if (delta > 0) incrementProjectCounter(counter) else decrementProjectCounter(counter)
            val newCount =
                if (delta >
                    0
                ) {
                    counter.count + counter.stepSize
                } else {
                    (counter.count - counter.stepSize).coerceAtLeast(0)
                }
            return context.getString(
                if (delta >
                    0
                ) {
                    R.string.voice_counter_incremented
                } else {
                    R.string.voice_counter_decremented
                },
                counter.name,
                newCount,
            )
        }

        private fun handleVoiceResetCounter(
            state: CounterUiState,
            name: String,
        ): String {
            val counter =
                state.projectCounters.firstOrNull { it.name.equals(name, ignoreCase = true) }
                    ?: return context.getString(R.string.voice_counter_not_found, name)
            resetProjectCounter(counter.id)
            return context.getString(R.string.voice_counter_reset_named, counter.name)
        }

        private fun handleVoicePageNav(
            state: CounterUiState,
            navigate: () -> String,
        ): String = if (state.patternUri != null) navigate() else context.getString(R.string.voice_instruction_none)

        private fun handleVoicePreviousPage(state: CounterUiState): String =
            when {
                state.patternUri == null -> {
                    context.getString(R.string.voice_instruction_none)
                }

                state.currentPatternPage <= 0 -> {
                    context.getString(R.string.voice_page_first)
                }

                else -> {
                    updatePatternPage(state.currentPatternPage - 1)
                    context.getString(
                        R.string.voice_page_current,
                        _uiState.value.currentPatternPage + 1,
                    )
                }
            }

        // — Voice query helpers —

        private fun voiceQueryProgress(state: CounterUiState): String =
            if (state.totalRows != null && state.totalRows > 0) {
                context.getString(
                    R.string.voice_row_of_target,
                    state.counter.count,
                    state.totalRows,
                    (state.counter.count * 100) / state.totalRows,
                )
            } else {
                context.getString(R.string.voice_row_current, state.counter.count)
            }

        private fun voiceQueryRemaining(state: CounterUiState): String =
            if (state.totalRows != null) {
                val remaining = (state.totalRows - state.counter.count).coerceAtLeast(0)
                if (remaining ==
                    0
                ) {
                    context.getString(R.string.voice_remaining_done)
                } else {
                    context.getString(R.string.voice_rows_remaining, remaining)
                }
            } else {
                context.getString(R.string.voice_no_target)
            }

        private fun voiceQuerySessionTime(state: CounterUiState): String {
            val minutes = (state.sessionSeconds / 60).toInt()
            return if (minutes >=
                1
            ) {
                context.getString(R.string.voice_session_minutes, minutes)
            } else {
                context.getString(R.string.voice_session_under_minute)
            }
        }

        private fun voiceQueryTotalTime(state: CounterUiState): String {
            val totalMinutes = state.totalSessionMinutes
            if (totalMinutes < 1) return context.getString(R.string.voice_total_time_none)
            val hours = totalMinutes / 60
            val mins = totalMinutes % 60
            return if (hours >
                0
            ) {
                context.getString(R.string.voice_total_time_hours, hours, mins)
            } else {
                context.getString(R.string.voice_total_time_minutes, mins)
            }
        }

        private fun voiceQueryYarn(state: CounterUiState): String =
            if (state.linkedYarns.isNotEmpty()) {
                val firstName = state.linkedYarns.first().second
                if (state.linkedYarns.size ==
                    1
                ) {
                    firstName
                } else {
                    context.getString(
                        R.string.voice_yarn_multiple,
                        firstName,
                        state.linkedYarns.size - 1,
                    )
                }
            } else {
                context.getString(R.string.voice_yarn_none)
            }

        private fun voiceQueryInstruction(state: CounterUiState): String {
            val name = state.patternName ?: state.linkedPattern?.name
            return if (name !=
                null
            ) {
                context.getString(R.string.voice_pattern_info, name, state.currentPatternPage + 1)
            } else {
                context.getString(R.string.voice_instruction_none)
            }
        }

        private fun voiceQueryShaping(state: CounterUiState): String {
            val shaping = state.projectCounters.firstOrNull { it.counterType == "SHAPING" }
            return when {
                shaping != null && shaping.shapeEveryN != null && shaping.shapeEveryN > 0 -> {
                    context.getString(
                        R.string.voice_shaping_next,
                        shaping.name,
                        shaping.shapeEveryN - (state.counter.count % shaping.shapeEveryN),
                    )
                }

                shaping != null -> {
                    context.getString(R.string.voice_shaping_at, shaping.name, shaping.count)
                }

                else -> {
                    context.getString(R.string.voice_shaping_none)
                }
            }
        }

        private fun voiceQueryStitches(state: CounterUiState): String =
            when {
                state.stitchTrackingEnabled && state.stitchCount != null && state.stitchCount > 0 -> {
                    context.getString(
                        R.string.voice_stitches_total,
                        state.stitchCount * state.counter.count,
                        state.stitchCount,
                        state.counter.count,
                    )
                }

                state.stitchTrackingEnabled -> {
                    context.getString(R.string.voice_stitches_current, state.currentStitch)
                }

                else -> {
                    context.getString(R.string.voice_stitches_disabled)
                }
            }

        private fun voiceQueryReminders(state: CounterUiState): String {
            val upcoming =
                state.reminders
                    .filter {
                        !it.isCompleted && it.targetRow > state.counter.count
                    }.sortedBy { it.targetRow }
            if (upcoming.isEmpty()) return context.getString(R.string.voice_reminders_none)
            val next = upcoming.first()
            val rowsUntil = next.targetRow - state.counter.count
            return if (upcoming.size ==
                1
            ) {
                context.getString(R.string.voice_reminder_next, next.message, rowsUntil)
            } else {
                context.getString(
                    R.string.voice_reminders_multiple,
                    next.message,
                    rowsUntil,
                    upcoming.size - 1,
                )
            }
        }

        private fun voiceQueryCounters(state: CounterUiState): String =
            when {
                state.projectCounters.isEmpty() -> {
                    context.getString(R.string.voice_counters_none)
                }

                state.projectCounters.size == 1 -> {
                    context.getString(
                        R.string.voice_counter_single,
                        state.projectCounters.first().name,
                        state.projectCounters.first().count,
                    )
                }

                else -> {
                    context
                        .getString(
                            R.string.voice_counters_list,
                            state.projectCounters.size,
                            state.projectCounters
                                .joinToString(
                                    ", ",
                                ) {
                                    "${it.name}: ${it.count}"
                                },
                        )
                }
            }

        private fun voiceQueryNotes(state: CounterUiState): String =
            if (state.notes.isBlank()) {
                context.getString(R.string.voice_notes_empty)
            } else {
                val preview = state.notes.take(MAX_VOICE_NOTES_LENGTH)
                val truncated = if (state.notes.length > MAX_VOICE_NOTES_LENGTH) "…" else ""
                context.getString(R.string.voice_notes_content, preview + truncated)
            }

        override fun onCleared() {
            val state = _uiState.value
            clearPendingSessionState()
            super.onCleared()
            @Suppress("TooGenericExceptionCaught")
            CoroutineScope(Dispatchers.IO + NonCancellable).launch {
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
            const val MAX_VOICE_NOTES_LENGTH = 200
        }
    }
