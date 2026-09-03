package com.finnvek.knittools.ui.screens.pro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.pro.TrialManager
import com.finnvek.knittools.pro.TrialStartResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProPromptViewModel
    @Inject
    constructor(
        private val proManager: ProManager,
        private val trialManager: TrialManager,
    ) : ViewModel() {
        val proState = proManager.proState

        private val trialStartResultChannel = Channel<TrialStartResult>(Channel.BUFFERED)
        val trialStartResults = trialStartResultChannel.receiveAsFlow()

        fun startTrial() {
            viewModelScope.launch {
                trialStartResultChannel.send(proManager.startTrial())
            }
        }

        fun markContextualPromptShown() {
            viewModelScope.launch { trialManager.markTrialEndNoticeShown() }
        }
    }
