package com.finnvek.knittools.ai.live

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

private val Context.voiceLiveQuotaStore by preferencesDataStore(name = "voice_live_quota")

/**
 * Seuraa kuukausittaista Live API ääni-minuuttikiintiötä.
 * Erillinen AiQuotaManagerista (tekstipohjaiset kutsut).
 */
@Singleton
class VoiceLiveQuotaManager
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) {
        suspend fun hasQuota(): Boolean {
            ensureMonthCurrent()
            val prefs = context.voiceLiveQuotaStore.data.first()
            val used = prefs[KEY_MINUTES_THIS_MONTH] ?: 0f
            return used < MONTHLY_ALLOWANCE
        }

        suspend fun recordMinutes(minutes: Float) {
            ensureMonthCurrent()
            context.voiceLiveQuotaStore.edit { prefs ->
                val current = prefs[KEY_MINUTES_THIS_MONTH] ?: 0f
                prefs[KEY_MINUTES_THIS_MONTH] = current + minutes
            }
        }

        val usage: Flow<VoiceLiveUsage> =
            context.voiceLiveQuotaStore.data.map { prefs ->
                val monthKey = prefs[KEY_MONTH_KEY] ?: ""
                val currentMonth = YearMonth.now().toString()
                val used = if (monthKey == currentMonth) prefs[KEY_MINUTES_THIS_MONTH] ?: 0f else 0f
                VoiceLiveUsage(
                    usedMinutes = used,
                    monthlyAllowance = MONTHLY_ALLOWANCE,
                )
            }

        private suspend fun ensureMonthCurrent() {
            val currentMonth = YearMonth.now().toString()
            context.voiceLiveQuotaStore.edit { prefs ->
                val storedMonth = prefs[KEY_MONTH_KEY]
                if (storedMonth != currentMonth) {
                    prefs[KEY_MONTH_KEY] = currentMonth
                    prefs[KEY_MINUTES_THIS_MONTH] = 0f
                }
            }
        }

        companion object {
            private val KEY_MINUTES_THIS_MONTH = floatPreferencesKey("voice_minutes_this_month")
            private val KEY_MONTH_KEY = stringPreferencesKey("voice_month_key")

            const val MONTHLY_ALLOWANCE = 30f
        }
    }

data class VoiceLiveUsage(
    val usedMinutes: Float,
    val monthlyAllowance: Float,
) {
    val remainingMinutes: Float
        get() = (monthlyAllowance - usedMinutes).coerceAtLeast(0f)
}
