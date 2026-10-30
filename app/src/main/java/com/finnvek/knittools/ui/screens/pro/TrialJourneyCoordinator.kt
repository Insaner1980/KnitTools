package com.finnvek.knittools.ui.screens.pro

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Kokeilun aloitus säilyy jonossa, kun alkuperäinen toiminto jatkuu omassa näkymässään. */
@Singleton
class TrialJourneyCoordinator
    @Inject
    constructor() {
        private val _pending = MutableStateFlow(false)
        val pending = _pending.asStateFlow()

        fun requestStartedJourney() {
            _pending.value = true
        }

        fun consume() {
            _pending.value = false
        }
    }
