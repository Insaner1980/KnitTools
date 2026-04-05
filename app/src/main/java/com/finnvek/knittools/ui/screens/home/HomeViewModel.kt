package com.finnvek.knittools.ui.screens.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.knittools.R
import com.finnvek.knittools.data.datastore.PreferencesManager
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.pro.ProState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        proManager: ProManager,
        preferencesManager: PreferencesManager,
        @ApplicationContext context: Context,
    ) : ViewModel() {
        val proState: StateFlow<ProState> = proManager.proState

        val showTips: StateFlow<Boolean> =
            preferencesManager.preferences
                .map { it.showKnittingTips }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

        private val tips = context.resources.getStringArray(R.array.knitting_tips)
        private val _currentTip = MutableStateFlow(tips.random())
        val currentTip: StateFlow<String> = _currentTip.asStateFlow()
    }
