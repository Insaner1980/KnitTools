package com.finnvek.knittools.ui.screens.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.knittools.data.local.SessionEntity
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.repository.CounterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class SessionHistoryViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        repository: CounterRepository,
        private val proManager: ProManager,
    ) : ViewModel() {
        private val projectId: Long = savedStateHandle["projectId"] ?: 0L

        val sessions: StateFlow<List<SessionEntity>> =
            repository
                .getSessionsForProject(projectId)
                .map { sessions ->
                    if (proManager.hasFeature(ProFeature.FULL_HISTORY)) {
                        // Pro: koko historia
                        sessions
                    } else {
                        // Free: vain viimeiset 24h
                        val cutoff = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(24)
                        sessions.filter { it.startedAt >= cutoff }
                    }
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        val isPro: Boolean get() = proManager.hasFeature(ProFeature.FULL_HISTORY)
    }
