package com.finnvek.knittools.ui.screens.yarn

import androidx.lifecycle.ViewModel
import com.finnvek.knittools.repository.YarnCardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class YarnEstimatorViewModel
    @Inject
    constructor(
        repository: YarnCardRepository,
    ) : ViewModel() {
        val yarnCards = repository.getAllCards()
    }
