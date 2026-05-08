package com.finnvek.knittools.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.knittools.ai.live.VoiceLiveQuotaManager
import com.finnvek.knittools.ai.live.VoiceLiveUsage
import com.finnvek.knittools.billing.BillingManager
import com.finnvek.knittools.billing.RestorePurchasesResult
import com.finnvek.knittools.data.datastore.AppLanguage
import com.finnvek.knittools.data.datastore.AppPreferences
import com.finnvek.knittools.data.datastore.PreferencesManager
import com.finnvek.knittools.data.datastore.ThemeMode
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.pro.ProState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val preferencesManager: PreferencesManager,
        private val billingManager: BillingManager,
        voiceLiveQuotaManager: VoiceLiveQuotaManager,
        proManager: ProManager,
    ) : ViewModel() {
        val preferences: StateFlow<AppPreferences> =
            preferencesManager.preferences.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = AppPreferences(),
            )
        val proState: StateFlow<ProState> = proManager.proState

        val voiceLiveUsage: StateFlow<VoiceLiveUsage> =
            voiceLiveQuotaManager.usage.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = VoiceLiveUsage(0f, VoiceLiveQuotaManager.MONTHLY_ALLOWANCE),
            )

        private val _messages = MutableSharedFlow<Int>()
        val messages: SharedFlow<Int> = _messages.asSharedFlow()
        private val _languageChanged = MutableSharedFlow<Unit>()
        val languageChanged: SharedFlow<Unit> = _languageChanged.asSharedFlow()

        fun setThemeMode(mode: ThemeMode) {
            viewModelScope.launch { preferencesManager.setThemeMode(mode) }
        }

        fun setAppLanguage(language: AppLanguage) {
            viewModelScope.launch {
                preferencesManager.setAppLanguage(language)
                _languageChanged.emit(Unit)
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

        fun setShowKnittingTips(enabled: Boolean) {
            viewModelScope.launch { preferencesManager.setShowKnittingTips(enabled) }
        }

        fun setVoiceLiveEnabled(enabled: Boolean) {
            viewModelScope.launch { preferencesManager.setVoiceLiveEnabled(enabled) }
        }

        fun restorePurchases() {
            viewModelScope.launch {
                when (billingManager.restorePurchasesWithResult()) {
                    RestorePurchasesResult.RESTORED -> {
                        _messages.emit(com.finnvek.knittools.R.string.pro_restored)
                    }

                    RestorePurchasesResult.NOT_FOUND -> {
                        _messages.emit(
                            com.finnvek.knittools.R.string.no_purchases_found,
                        )
                    }
                }
            }
        }
    }
