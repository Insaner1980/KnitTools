package com.finnvek.knittools.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.knittools.data.datastore.PreferencesManager
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.pro.ProState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

// Nimestä huolimatta tämä ei ole koko sovelluksen home/start-näkymän ViewModel,
// vaan Tools-välilehden entry-ruudun state.
@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        proManager: ProManager,
        preferencesManager: PreferencesManager,
    ) : ViewModel() {
        val proState: StateFlow<ProState> = proManager.proState

        val useImperial: StateFlow<Boolean> =
            preferencesManager.preferences
                .map { it.useImperial }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    }
