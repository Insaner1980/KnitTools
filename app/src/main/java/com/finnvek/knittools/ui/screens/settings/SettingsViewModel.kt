package com.finnvek.knittools.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.knittools.billing.BillingManager
import com.finnvek.knittools.data.datastore.AppPreferences
import com.finnvek.knittools.data.datastore.PreferencesManager
import com.finnvek.knittools.data.datastore.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val preferencesManager: PreferencesManager,
        private val billingManager: BillingManager,
    ) : ViewModel() {
        val preferences: StateFlow<AppPreferences> =
            preferencesManager.preferences.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = AppPreferences(),
            )

        fun setThemeMode(mode: ThemeMode) {
            viewModelScope.launch { preferencesManager.setThemeMode(mode) }
        }

        fun setHapticFeedback(enabled: Boolean) {
            viewModelScope.launch { preferencesManager.setHapticFeedback(enabled) }
        }

        fun setKeepScreenAwake(enabled: Boolean) {
            viewModelScope.launch { preferencesManager.setKeepScreenAwake(enabled) }
        }

        fun setUseImperial(imperial: Boolean) {
            viewModelScope.launch { preferencesManager.setUseImperial(imperial) }
        }

        fun restorePurchases() {
            billingManager.restorePurchases()
        }
    }
