package com.finnvek.knittools.ui.screens.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.knittools.domain.model.KnitSession
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.ui.navigation.toPositiveRouteIdOrNull
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessionHistoryViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val repository: CounterRepository,
    ) : ViewModel() {
        private val projectId: Long? = savedStateHandle.get<Long>("projectId")?.toPositiveRouteIdOrNull()
        private val projectState =
            (projectId?.let(repository::observeProject) ?: flowOf(null))
                .map { project -> ProjectState(name = project?.name, missing = project == null) }
                .stateIn(viewModelScope, SharingStarted.Eagerly, ProjectState(missing = projectId == null))

        val projectMissing: StateFlow<Boolean> =
            projectState
                .map { it.missing }
                .stateIn(viewModelScope, SharingStarted.Eagerly, projectId == null)

        /**
         * Insights on uusi sisäänkäynti tälle näytölle, joten ruudulla pitää lukea kenen
         * istuntoja katsotaan — pelkkä "History" ei kerro sitä enää.
         */
        val projectName: StateFlow<String?> =
            projectState
                .map { it.name }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

        val sessions: StateFlow<List<KnitSession>> =
            (projectId?.let { repository.getSessionsForProject(it) } ?: flowOf(emptyList()))
                .map { sessions ->
                    sessions.sortedWith(
                        compareByDescending<KnitSession> { it.startedAt }
                            .thenByDescending { it.id },
                    )
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        fun deleteSession(sessionId: Long) {
            viewModelScope.launch {
                repository.deleteSession(sessionId)
            }
        }

        private data class ProjectState(
            val name: String? = null,
            val missing: Boolean = false,
        )
    }
