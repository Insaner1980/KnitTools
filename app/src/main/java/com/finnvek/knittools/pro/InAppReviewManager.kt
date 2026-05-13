package com.finnvek.knittools.pro

import android.app.Activity
import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.finnvek.knittools.data.datastore.editPreferencesSafely
import com.finnvek.knittools.data.datastore.safePreferencesData
import com.google.android.play.core.review.ReviewManagerFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InAppReviewManager
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) {
        /**
         * Tallentaa käyttäjän toiminnon (esim. laskurin increment/decrement).
         * Kun raja ylittyy, arvostelu voidaan pyytää.
         */
        suspend fun recordAction() {
            context.reviewDataStore.editPreferencesSafely("Arvostelulaskurin tallennus") { prefs ->
                prefs[KEY_ACTION_COUNT] = (prefs[KEY_ACTION_COUNT] ?: 0) + 1
            }
        }

        /**
         * Pyytää arvostelua jos ehdot täyttyvät:
         * - Arvostelua ei ole vielä pyydetty
         * - Toimintoja on kertynyt riittävästi
         *
         * Google rajoittaa näyttötiheyttä omalla kiintiöllään, mutta sovelluksen
         * oma käyttöraja estää liian aikaisen pyynnön ja turhat API-kutsut.
         */
        suspend fun maybeRequestReview(activity: Activity) {
            val prefs = context.reviewDataStore.safePreferencesData.first()
            val actions = prefs[KEY_ACTION_COUNT] ?: 0
            if (!shouldRequestReview(prefs[KEY_REVIEW_REQUESTED] == true, actions)) return

            val saved =
                context.reviewDataStore.editPreferencesSafely("Arvostelupyynnön tallennus") {
                    it[KEY_REVIEW_REQUESTED] = true
                }
            if (!saved) return

            val manager = ReviewManagerFactory.create(context)
            manager.requestReviewFlow().addOnSuccessListener { reviewInfo ->
                manager.launchReviewFlow(activity, reviewInfo)
            }
        }

        companion object {
            private val Context.reviewDataStore by preferencesDataStore(
                name = "review_state",
                corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            )
            private val KEY_REVIEW_REQUESTED = booleanPreferencesKey("review_requested")
            private val KEY_ACTION_COUNT = intPreferencesKey("action_count")
            const val ACTIONS_THRESHOLD = 20

            internal fun shouldRequestReview(
                reviewRequested: Boolean,
                actionCount: Int,
            ): Boolean = !reviewRequested && actionCount >= ACTIONS_THRESHOLD
        }
    }
