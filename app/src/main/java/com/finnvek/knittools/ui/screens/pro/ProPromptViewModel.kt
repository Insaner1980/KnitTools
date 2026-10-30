package com.finnvek.knittools.ui.screens.pro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.pro.TrialManager
import com.finnvek.knittools.pro.TrialStartResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProPromptViewModel
    @Inject
    constructor(
        private val proManager: ProManager,
        private val trialManager: TrialManager,
        private val trialJourneyCoordinator: TrialJourneyCoordinator,
    ) : ViewModel() {
        val proState = proManager.proState

        private val _trialStartResults = MutableSharedFlow<TrialStartResult>(extraBufferCapacity = 1)
        val trialStartResults = _trialStartResults.asSharedFlow()
        private val _isStartingTrial = MutableStateFlow(false)
        val isStartingTrial = _isStartingTrial.asStateFlow()

        fun startTrial() {
            if (_isStartingTrial.value) return
            _isStartingTrial.value = true
            viewModelScope.launch {
                try {
                    val result = proManager.startTrial()
                    if (result == TrialStartResult.Started) trialJourneyCoordinator.requestStartedJourney()
                    _trialStartResults.emit(result)
                } finally {
                    _isStartingTrial.value = false
                }
            }
        }

        fun markContextualPromptShown() {
            viewModelScope.launch { trialManager.markTrialEndNoticeShown() }
        }
    }
