package com.finnvek.knittools.ui.screens.home

import androidx.lifecycle.ViewModel
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.pro.ProState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        proManager: ProManager,
    ) : ViewModel() {
        val proState: StateFlow<ProState> = proManager.proState
    }
