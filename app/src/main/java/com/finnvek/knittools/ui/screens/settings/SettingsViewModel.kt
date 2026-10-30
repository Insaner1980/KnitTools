package com.finnvek.knittools.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.knittools.R
import com.finnvek.knittools.billing.BillingManager
import com.finnvek.knittools.billing.RestorePurchasesResult
import com.finnvek.knittools.data.datastore.AppLanguage
import com.finnvek.knittools.data.datastore.AppPreferences
import com.finnvek.knittools.data.datastore.PreferencesManager
import com.finnvek.knittools.data.datastore.ThemeMode
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.pro.ProState
import com.finnvek.knittools.ui.tips.TipSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val preferencesManager: PreferencesManager,
        private val billingManager: BillingManager,
        proManager: ProManager,
        private val tipSession: TipSession,
    ) : ViewModel() {
        val preferences: StateFlow<AppPreferences> =
            preferencesManager.preferences.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = AppPreferences(),
            )
        val proState: StateFlow<ProState> = proManager.proState
        val proStateReady: StateFlow<Boolean> = proManager.initialStateReady

        private val messageChannel = Channel<Int>(Channel.BUFFERED)
        val messages = messageChannel.receiveAsFlow()
        private val _isRestoring = MutableStateFlow(false)
        val isRestoring: StateFlow<Boolean> = _isRestoring.asStateFlow()

        fun setThemeMode(mode: ThemeMode) {
            viewModelScope.launch { preferencesManager.setThemeMode(mode) }
        }

        fun setUsageAnalyticsEnabled(enabled: Boolean) {
            viewModelScope.launch {
                if (!preferencesManager.setUsageAnalyticsEnabled(enabled)) {
                    messageChannel.send(R.string.generic_error_unknown)
                }
            }
        }

        fun setAppLanguage(language: AppLanguage) {
            viewModelScope.launch {
                preferencesManager.setAppLanguage(language)
            }
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
            if (_isRestoring.value) return
            _isRestoring.value = true
            viewModelScope.launch {
                try {
                    val messageRes =
                        when (billingManager.restorePurchasesWithResult()) {
                            RestorePurchasesResult.RESTORED -> R.string.pro_restored
                            RestorePurchasesResult.NOT_FOUND -> R.string.no_purchases_found
                            RestorePurchasesResult.FAILED -> R.string.generic_error_unknown
                        }
                    messageChannel.send(messageRes)
                } finally {
                    _isRestoring.value = false
                }
            }
        }

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
    }
