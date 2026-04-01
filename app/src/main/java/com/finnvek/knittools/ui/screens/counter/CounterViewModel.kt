package com.finnvek.knittools.ui.screens.counter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.knittools.data.datastore.PreferencesManager
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.domain.calculator.CounterLogic
import com.finnvek.knittools.domain.calculator.CounterState
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.repository.CounterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
)

@HiltViewModel
class CounterViewModel
    @Inject
    constructor(
        private val repository: CounterRepository,
        private val preferencesManager: PreferencesManager,
        private val proManager: ProManager,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(CounterUiState())
        val uiState: StateFlow<CounterUiState> = _uiState.asStateFlow()

        private var timerJob: Job? = null

        init {
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
                        val id = repository.createProject("My Project")
                        _uiState.update { it.copy(projectId = id, projects = emptyList()) }
                    } else {
                        _uiState.update { it.copy(projects = list) }
                        if (_uiState.value.projectId == null) {
                            selectProject(list.first())
                        }
                    }
                }
            }
        }

        fun selectProject(project: CounterProjectEntity) {
            _uiState.update {
                it.copy(
                    projectId = project.id,
                    projectName = project.name,
                    counter = CounterState(count = project.count, stepSize = project.stepSize),
                    secondaryCount = project.secondaryCount,
                    notes = project.notes,
                )
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
            persistCount("increment")
        }

        fun decrement() {
            _uiState.update { state ->
                state.copy(counter = CounterLogic.decrement(state.counter))
            }
            persistCount("decrement")
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

        fun resetSecondary() {
            _uiState.update { it.copy(secondaryCount = 0) }
            persistSecondary()
        }

        fun setNotes(notes: String) {
            if (!proManager.hasFeature(ProFeature.NOTES)) return
            _uiState.update { it.copy(notes = notes) }
            viewModelScope.launch {
                val id = _uiState.value.projectId ?: return@launch
                repository.getProject(id)?.let { project ->
                    repository.updateProject(project.copy(notes = notes))
                }
            }
        }

        fun setProjectName(name: String) {
            _uiState.update { it.copy(projectName = name) }
            viewModelScope.launch {
                val id = _uiState.value.projectId ?: return@launch
                repository.getProject(id)?.let { project ->
                    repository.updateProject(project.copy(name = name))
                }
            }
        }

        fun setStepSize(size: Int) {
            _uiState.update { state ->
                state.copy(counter = CounterLogic.setStepSize(state.counter, size))
            }
        }

        private fun persistCount(action: String) {
            viewModelScope.launch {
                val state = _uiState.value
                val id = state.projectId ?: return@launch
                repository.getProject(id)?.let { project ->
                    repository.updateProject(
                        project.copy(
                            count = state.counter.count,
                            stepSize = state.counter.stepSize,
                        ),
                    )
                }
                state.counter.previousCount?.let { prev ->
                    repository.addHistoryEntry(id, action, prev, state.counter.count)
                }
            }
        }

        private fun persistSecondary() {
            viewModelScope.launch {
                val state = _uiState.value
                val id = state.projectId ?: return@launch
                repository.getProject(id)?.let { project ->
                    repository.updateProject(project.copy(secondaryCount = state.secondaryCount))
                }
            }
        }

        private fun pruneHistoryForFree() {
            viewModelScope.launch {
                val id = _uiState.value.projectId ?: return@launch
                val cutoff = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(HISTORY_LIMIT_HOURS)
                repository.deleteHistoryBefore(id, cutoff)
            }
        }

        private fun startTimer() {
            timerJob =
                viewModelScope.launch {
                    while (true) {
                        delay(1000)
                        _uiState.update { it.copy(sessionSeconds = it.sessionSeconds + 1) }
                    }
                }
        }

        private companion object {
            const val HISTORY_LIMIT_HOURS = 24L
        }
    }
