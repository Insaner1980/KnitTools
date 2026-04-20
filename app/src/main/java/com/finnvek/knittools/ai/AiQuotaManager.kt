package com.finnvek.knittools.ai

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

private val Context.aiQuotaStore by preferencesDataStore(name = "ai_quota")

/**
 * Seuraa kuukausittaista AI-käyttöä credit-järjestelmää varten.
 * Laskuri nollautuu automaattisesti kuukauden vaihtuessa.
 */
@Singleton
class AiQuotaManager
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) {
        /**
         * Tarkistaa onko kiintiötä jäljellä.
         * Nollaa laskurin jos kuukausi on vaihtunut.
         */
        suspend fun hasQuota(): Boolean {
            ensureMonthCurrent()
            val prefs = context.aiQuotaStore.data.first()
            val used = prefs[KEY_CALLS_THIS_MONTH] ?: 0
            val extras = prefs[KEY_EXTRA_CREDITS] ?: 0
            return used < (MONTHLY_ALLOWANCE + extras)
        }

        /**
         * Kirjaa yhden AI-kutsun. Kutsu tätä jokaisen onnistuneen Gemini-kutsun jälkeen.
         */
        suspend fun recordCall() {
            ensureMonthCurrent()
            context.aiQuotaStore.edit { prefs ->
                val current = prefs[KEY_CALLS_THIS_MONTH] ?: 0
                prefs[KEY_CALLS_THIS_MONTH] = current + 1
            }
        }

        /**
         * Palauttaa kuluvan kuukauden käyttötiedot näytettäväksi Asetuksissa.
         */
        val usage: Flow<AiUsage> =
            context.aiQuotaStore.data.map { prefs ->
                val monthKey = prefs[KEY_MONTH_KEY] ?: ""
                val currentMonth = YearMonth.now().toString()
                val used = if (monthKey == currentMonth) prefs[KEY_CALLS_THIS_MONTH] ?: 0 else 0
                val extras = prefs[KEY_EXTRA_CREDITS] ?: 0
                AiUsage(
                    usedThisMonth = used,
                    monthlyAllowance = MONTHLY_ALLOWANCE,
                    extraCredits = extras,
                )
            }

        /**
         * Lisää ostettuja lisäkredittejä (consumable IAP).
         */
        suspend fun addExtraCredits(amount: Int) {
            context.aiQuotaStore.edit { prefs ->
                val current = prefs[KEY_EXTRA_CREDITS] ?: 0
                prefs[KEY_EXTRA_CREDITS] = current + amount
            }
        }

        /**
         * Tarkistaa onko ääni-AI-kiintiötä jäljellä (sama kuukausikiintiö kuin muille AI-kutsuille).
         */
        suspend fun hasVoiceQuota(): Boolean = hasQuota()

        /**
         * Kirjaa yhden ääni-AI-kutsun kuukausikiintiöön.
         */
        suspend fun recordVoiceCall() = recordCall()

        private suspend fun ensureMonthCurrent() {
            val currentMonth = YearMonth.now().toString()
            context.aiQuotaStore.edit { prefs ->
                val storedMonth = prefs[KEY_MONTH_KEY]
                if (storedMonth != currentMonth) {
                    prefs[KEY_MONTH_KEY] = currentMonth
                    prefs[KEY_CALLS_THIS_MONTH] = 0
                }
            }
        }

        companion object {
            private val KEY_CALLS_THIS_MONTH = intPreferencesKey("ai_calls_this_month")
            private val KEY_MONTH_KEY = stringPreferencesKey("ai_month_key")
            private val KEY_EXTRA_CREDITS = intPreferencesKey("ai_extra_credits")

            /** Pro-käyttäjien kuukausikiintiö (voidaan säätää myöhemmin) */
            const val MONTHLY_ALLOWANCE = 500
        }
    }

data class AiUsage(
    val usedThisMonth: Int,
    val monthlyAllowance: Int,
    val extraCredits: Int,
) {
    val remaining: Int
        get() = (monthlyAllowance + extraCredits - usedThisMonth).coerceAtLeast(0)
}
