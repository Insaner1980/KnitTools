package com.finnvek.knittools.ui.screens.counter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.knittools.data.datastore.PreferencesManager
import com.finnvek.knittools.domain.calculator.CounterLogic
import com.finnvek.knittools.domain.calculator.CounterState
import com.finnvek.knittools.repository.CounterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CounterUiState(
    val projectName: String = "My Project",
    val counter: CounterState = CounterState(),
    val sessionSeconds: Long = 0,
    val projectId: Long? = null,
    val hapticFeedback: Boolean = true,
    val keepScreenAwake: Boolean = false,
)

@HiltViewModel
class CounterViewModel
    @Inject
    constructor(
        private val repository: CounterRepository,
        private val preferencesManager: PreferencesManager,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(CounterUiState())
        val uiState: StateFlow<CounterUiState> = _uiState.asStateFlow()

        private var timerJob: Job? = null

        init {
            loadOrCreateProject()
            startTimer()
            observePreferences()
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

        private fun loadOrCreateProject() {
            viewModelScope.launch {
                val projects = repository.getProjectCount()
                if (projects == 0) {
                    val id = repository.createProject("My Project")
                    _uiState.update { it.copy(projectId = id) }
                } else {
                    repository.getAllProjects().collect { list ->
                        list.firstOrNull()?.let { project ->
                            _uiState.update {
                                it.copy(
                                    projectId = project.id,
                                    projectName = project.name,
                                    counter =
                                        CounterState(
                                            count = project.count,
                                            stepSize = project.stepSize,
                                        ),
                                )
                            }
                        }
                    }
                }
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

        fun increment() {
            _uiState.update { state ->
                val newCounter = CounterLogic.increment(state.counter)
                state.copy(counter = newCounter)
            }
            persistCount("increment")
        }

        fun decrement() {
            _uiState.update { state ->
                val newCounter = CounterLogic.decrement(state.counter)
                state.copy(counter = newCounter)
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
    }
