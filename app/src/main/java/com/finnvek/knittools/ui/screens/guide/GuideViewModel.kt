package com.finnvek.knittools.ui.screens.guide

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.knittools.R
import com.finnvek.knittools.analytics.UsageAnalytics
import com.finnvek.knittools.analytics.UsageEvent
import com.finnvek.knittools.di.IoDispatcher
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.ui.tips.TipSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GuideViewModel
    @Inject
    constructor(
        private val repository: CounterRepository,
        proManager: ProManager,
        private val tipSession: TipSession,
        private val analytics: UsageAnalytics,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : ViewModel() {
        val proState =
            combine(proManager.proState, proManager.initialStateReady) { state, ready ->
                state.takeIf { ready }
            }

        private val messageChannel = Channel<Int>(Channel.BUFFERED)
        val messages = messageChannel.receiveAsFlow()
        private var opened = false

        fun onOpened() {
            if (opened) return
            opened = true
            analytics.track(UsageEvent.GUIDE_OPENED)
        }

        fun projectForAction(action: GuideAction): Deferred<Long?> =
            viewModelScope.async(ioDispatcher) {
                when (action) {
                    GuideAction.OpenCounterForLatestProject ->
                        repository
                            .getActiveProjects()
                            .first()
                            .firstOrNull()
                            ?.id
                    GuideAction.OpenLatestProjectPage ->
                        repository
                            .getAllProjects()
                            .first()
                            .firstOrNull()
                            ?.id
                    else -> null
                }
            }

        // CPD-OFF: Kumpikin näkymämalli omistaa resetoinnin elinkaaren ja oman ilmoituskanavansa.
        fun showTipsAgain() {
            viewModelScope.launch {
                val message =
                    if (tipSession.resetTips().await()) {
                        R.string.settings_tips_reset_done
                    } else {
                        R.string.generic_error_unknown
                    }
                messageChannel.send(message)
            }
        }
        // CPD-ON
    }
