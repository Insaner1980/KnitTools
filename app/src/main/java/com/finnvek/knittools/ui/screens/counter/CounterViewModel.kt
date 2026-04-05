package com.finnvek.knittools.ui.screens.counter

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.knittools.R
import com.finnvek.knittools.ai.nano.NanoAvailability
import com.finnvek.knittools.ai.nano.NanoStatus
import com.finnvek.knittools.ai.nano.ProjectSummarizer
import com.finnvek.knittools.data.datastore.PreferencesManager
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.SessionEntity
import com.finnvek.knittools.data.local.YarnCardEntity
import com.finnvek.knittools.domain.calculator.CounterLogic
import com.finnvek.knittools.domain.calculator.CounterState
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.repository.YarnCardRepository
import com.finnvek.knittools.widget.CounterWidget
import com.finnvek.knittools.widget.CounterWidgetState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class CounterUiState(
    val projectName: String = "My Project",
    val counter: CounterState = CounterState(),
    val secondaryCount: Int = 0,
    val notes: String = "",
    val sessionSeconds: Long = 0,
    val projectId: Long? = null,
    val hapticFeedback: Boolean = true,
    val keepScreenAwake: Boolean = false,
    val isPro: Boolean = false,
    val projects: List<CounterProjectEntity> = emptyList(),
    val sectionName: String? = null,
    val stitchCount: Int? = null,
    val linkedYarns: List<Pair<Long, String>> = emptyList(),
    val totalSessionMinutes: Int = 0,
    val projectSummary: String? = null,
    val isSummaryLoading: Boolean = false,
)

@HiltViewModel
class CounterViewModel
    @Inject
    constructor(
        private val repository: CounterRepository,
        private val preferencesManager: PreferencesManager,
        private val proManager: ProManager,
        private val yarnCardRepository: YarnCardRepository,
        @ApplicationContext private val context: Context,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(CounterUiState())
        val uiState: StateFlow<CounterUiState> = _uiState.asStateFlow()

        val savedYarnCards: StateFlow<List<YarnCardEntity>> =
            yarnCardRepository.getAllCards().stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList(),
            )

        private var timerJob: Job? = null
        private var selectedProjectJob: Job? = null
        private var linkedYarnIdsCache: String = ""
        private var isForeground = true

        // Session tracking
        private var sessionStartedAt: Long = System.currentTimeMillis()
        private var sessionStartRow: Int = 0

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
                    _uiState.update { it.copy(isPro = proState.isPro) }
                    if (!proState.isPro) {
                        pruneHistoryForFree()
                    }
                }
            }
        }

        private fun loadOrCreateProject() {
            viewModelScope.launch {
                repository.getAllProjects().collect { list ->
                    if (list.isEmpty()) {
                        val id = repository.createProject(context.getString(R.string.default_project_name))
                        _uiState.update { it.copy(projectId = id, projects = emptyList()) }
                    } else {
                        _uiState.update { it.copy(projects = list) }
                        val currentId = _uiState.value.projectId
                        if (currentId == null || list.none { it.id == currentId }) {
                            sessionStartRow = list.first().count
                            selectProject(list.first())
                        }
                    }
                }
            }
        }

        fun selectProject(project: CounterProjectEntity) {
            // Tallenna edellinen sessio ennen projektin vaihtoa
            saveCurrentSession()

            sessionStartedAt = System.currentTimeMillis()
            sessionStartRow = project.count
            linkedYarnIdsCache = project.yarnCardIds

            _uiState.update {
                it.copy(
                    projectId = project.id,
                    projectName = project.name,
                    counter = CounterState(count = project.count, stepSize = project.stepSize),
                    secondaryCount = project.secondaryCount,
                    notes = project.notes,
                    sectionName = project.sectionName,
                    stitchCount = project.stitchCount,
                    sessionSeconds = 0,
                )
            }
            observeSelectedProject(project.id)
            viewModelScope.launch {
                syncWidget()
                loadLinkedYarnNames(project.yarnCardIds)
                loadTotalSessionMinutes(project.id)
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
                val newIds = (currentIds + cardId).joinToString(",")
                repository.updateProjectYarnCardIds(id, newIds)
            }
        }

        fun unlinkYarnCard(cardId: Long) {
            viewModelScope.launch {
                val id = _uiState.value.projectId ?: return@launch
                val project = repository.getProject(id) ?: return@launch
                val currentIds = project.yarnCardIds.split(",").mapNotNull { it.trim().toLongOrNull() }
                val newIds = currentIds.filter { it != cardId }.joinToString(",")
                repository.updateProjectYarnCardIds(id, newIds)
            }
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

        private fun saveCurrentSession() {
            val state = _uiState.value
            val projectId = state.projectId ?: return
            val durationMinutes = (state.sessionSeconds / 60).toInt()
            if (durationMinutes < 1 && state.counter.count == sessionStartRow) return

            val session =
                SessionEntity(
                    projectId = projectId,
                    startedAt = sessionStartedAt,
                    endedAt = System.currentTimeMillis(),
                    startRow = sessionStartRow,
                    endRow = state.counter.count,
                    durationMinutes = maxOf(1, durationMinutes),
                )
            // Käytä NonCancellable koska tätä kutsutaan myös onCleared:ssa
            CoroutineScope(Dispatchers.IO + NonCancellable).launch {
                repository.insertSession(session)
            }
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

        fun deleteProject(id: Long) {
            viewModelScope.launch {
                repository.deleteProject(id)
                if (_uiState.value.projectId == id) {
                    _uiState.update { it.copy(projectId = null) }
                }
            }
        }

        fun increment() {
            _uiState.update { state ->
                state.copy(counter = CounterLogic.increment(state.counter))
            }
            persistCount("increment", delta = _uiState.value.counter.stepSize)
        }

        fun decrement() {
            _uiState.update { state ->
                state.copy(counter = CounterLogic.decrement(state.counter))
            }
            persistCount("decrement", delta = -_uiState.value.counter.stepSize)
        }

        fun undo() {
            _uiState.update { state ->
                state.copy(counter = CounterLogic.undo(state.counter))
            }
            persistCount("undo")
        }

        fun reset() {
            _uiState.update { state ->
                state.copy(counter = CounterLogic.reset(state.counter))
            }
            persistCount("reset")
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

        fun resetSecondary() {
            _uiState.update { it.copy(secondaryCount = 0) }
            persistSecondary()
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
            _uiState.update { state ->
                state.copy(counter = CounterLogic.setStepSize(state.counter, size))
            }
        }

        private fun persistCount(
            action: String,
            delta: Int? = null,
        ) {
            viewModelScope.launch {
                val state = _uiState.value
                val id = state.projectId ?: return@launch
                state.counter.previousCount?.let { prev ->
                    if (delta != null) {
                        repository.adjustProjectCountWithHistory(
                            id = id,
                            delta = delta,
                            stepSize = state.counter.stepSize,
                            action = action,
                            previousValue = prev,
                            newValue = state.counter.count,
                        )
                    } else {
                        repository.updateProjectCounterStateWithHistory(
                            id = id,
                            count = state.counter.count,
                            stepSize = state.counter.stepSize,
                            action = action,
                            previousValue = prev,
                            newValue = state.counter.count,
                        )
                    }
                }
                syncWidget()
            }
        }

        private fun persistSecondary() {
            viewModelScope.launch {
                val state = _uiState.value
                val id = state.projectId ?: return@launch
                repository.updateProjectSecondaryCount(id, state.secondaryCount)
            }
        }

        private fun pruneHistoryForFree() {
            viewModelScope.launch {
                val id = _uiState.value.projectId ?: return@launch
                val cutoff = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(HISTORY_LIMIT_HOURS)
                repository.deleteHistoryBefore(id, cutoff)
            }
        }

        private suspend fun syncWidget() {
            val state = _uiState.value
            val id = state.projectId ?: return
            CounterWidgetState.save(context, state.projectName, state.counter.count, id)
            CounterWidget().updateAll(context)
        }

        private fun startTimer() {
            timerJob =
                viewModelScope.launch {
                    while (true) {
                        delay(1000)
                        if (isForeground) {
                            _uiState.update { it.copy(sessionSeconds = it.sessionSeconds + 1) }
                        }
                    }
                }
        }

        fun selectProjectById(id: Long) {
            viewModelScope.launch {
                repository.getProject(id)?.let { selectProject(it) }
            }
        }

        fun generateSummary() {
            viewModelScope.launch {
                val state = _uiState.value
                val projectId = state.projectId ?: return@launch
                _uiState.update { it.copy(isSummaryLoading = true, projectSummary = null) }

                val sessionCount =
                    repository
                        .getSessionsForProject(projectId)
                        .stateIn(viewModelScope)
                        .value.size
                val totalMinutes = repository.getTotalMinutesForProject(projectId)
                val avgRows = if (sessionCount > 0) state.counter.count.toDouble() / sessionCount else 0.0
                val createdAt = state.projects.find { it.id == projectId }?.createdAt ?: System.currentTimeMillis()
                val daysActive = ((System.currentTimeMillis() - createdAt) / 86_400_000).toInt().coerceAtLeast(1)

                val data =
                    ProjectSummarizer.ProjectData(
                        name = state.projectName,
                        currentRow = state.counter.count,
                        sectionName = state.sectionName,
                        totalSessionMinutes = totalMinutes,
                        sessionCount = sessionCount,
                        averageRowsPerSession = avgRows,
                        linkedYarns = state.linkedYarns.map { it.second },
                        notes = state.notes,
                        daysActive = daysActive,
                    )

                // Yritä Nanoa, fallback yksinkertaiseen yhteenvetoon
                val nanoAvailable = NanoAvailability.check() != NanoStatus.UNAVAILABLE
                val summary =
                    if (nanoAvailable) {
                        ProjectSummarizer.summarize(data) ?: ProjectSummarizer.simpleSummary(data)
                    } else {
                        ProjectSummarizer.simpleSummary(data)
                    }

                _uiState.update { it.copy(projectSummary = summary, isSummaryLoading = false) }
            }
        }

        fun clearSummary() {
            _uiState.update { it.copy(projectSummary = null) }
        }

        fun setSectionName(name: String?) {
            _uiState.update { it.copy(sectionName = name) }
            viewModelScope.launch {
                val id = _uiState.value.projectId ?: return@launch
                repository.updateProjectSectionName(id, name)
            }
        }

        fun setStitchCount(count: Int?) {
            _uiState.update { it.copy(stitchCount = count) }
            viewModelScope.launch {
                val id = _uiState.value.projectId ?: return@launch
                repository.updateProjectStitchCount(id, count)
            }
        }

        override fun onCleared() {
            super.onCleared()
            saveCurrentSession()
            selectedProjectJob?.cancel()
            ProcessLifecycleOwner.get().lifecycle.removeObserver(lifecycleObserver)
        }

        private companion object {
            const val HISTORY_LIMIT_HOURS = 24L
        }
    }
