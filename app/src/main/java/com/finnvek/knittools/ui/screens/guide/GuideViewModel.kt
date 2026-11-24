package com.finnvek.knittools.ui.screens.guide

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.knittools.analytics.UsageAnalytics
import com.finnvek.knittools.analytics.UsageEvent
import com.finnvek.knittools.di.IoDispatcher
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.repository.CounterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@HiltViewModel
class GuideViewModel
    @Inject
    constructor(
        private val repository: CounterRepository,
        proManager: ProManager,
        private val analytics: UsageAnalytics,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : ViewModel() {
        val proState =
            combine(proManager.proState, proManager.initialStateReady) { state, ready ->
                state.takeIf { ready }
            }

        private var opened = false

        fun onOpened() {
            if (opened) return
            opened = true
            analytics.track(UsageEvent.GUIDE_OPENED)
        }

        fun projectForAction(action: GuideAction): Deferred<Long?> =
            viewModelScope.async(ioDispatcher) {
                when (action) {
                    GuideAction.OpenCounterForLatestProject -> {
                        repository
                            .getActiveProjects()
                            .first()
                            .firstOrNull()
                            ?.id
                    }

                    GuideAction.OpenLatestProjectPage -> {
                        repository
                            .getAllProjects()
                            .first()
                            .firstOrNull()
                            ?.id
                    }

                    else -> {
                        null
                    }
                }
            }
    }
