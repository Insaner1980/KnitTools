package com.finnvek.knittools.ui.screens.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.knittools.domain.model.KnitSession
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.ui.navigation.toPositiveRouteIdOrNull
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class SessionHistoryViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val repository: CounterRepository,
        private val proManager: ProManager,
    ) : ViewModel() {
        private val projectId: Long? = savedStateHandle.get<Long>("projectId")?.toPositiveRouteIdOrNull()
        private val _projectMissing = MutableStateFlow(projectId == null)
        val projectMissing: StateFlow<Boolean> = _projectMissing.asStateFlow()

        init {
            projectId?.let { loadedProjectId ->
                viewModelScope.launch {
                    if (repository.getProject(loadedProjectId) == null) {
                        _projectMissing.value = true
                    }
                }
            }
        }

        val isPro: StateFlow<Boolean> =
            proManager
                .hasFeatureFlow(ProFeature.FULL_HISTORY)
                .stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5000),
                    proManager.hasFeature(ProFeature.FULL_HISTORY),
                )

        val sessions: StateFlow<List<KnitSession>> =
            combine(
                projectId?.let { repository.getSessionsForProject(it) } ?: flowOf(emptyList()),
                isPro,
            ) { sessions, canUseFullHistory ->
                val visibleSessions =
                    if (canUseFullHistory) {
                        // Pro: koko historia
                        sessions
                    } else {
                        // Free: vain viimeiset 24h
                        val cutoff = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(24)
                        sessions.filter { it.startedAt >= cutoff }
                    }
                visibleSessions.sortedWith(
                    compareByDescending<KnitSession> { it.startedAt }
                        .thenByDescending { it.id },
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        fun deleteSession(sessionId: Long) {
            viewModelScope.launch {
                repository.deleteSession(sessionId)
            }
        }
    }
