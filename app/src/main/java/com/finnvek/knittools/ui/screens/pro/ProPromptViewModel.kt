package com.finnvek.knittools.ui.screens.pro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.pro.TrialManager
import com.finnvek.knittools.pro.TrialStartResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
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

        private val _trialStartResults = MutableSharedFlow<TrialStartResult>()
        val trialStartResults = _trialStartResults.asSharedFlow()

        fun startTrial() {
            viewModelScope.launch {
                _trialStartResults.emit(proManager.startTrial())
            }
        }

        fun markContextualPromptShown() {
            viewModelScope.launch { trialManager.markTrialEndNoticeShown() }
        }
    }
