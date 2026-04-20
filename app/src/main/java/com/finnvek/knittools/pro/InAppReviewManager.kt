package com.finnvek.knittools.pro

import android.app.Activity
import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
            context.reviewDataStore.edit { prefs ->
                prefs[KEY_ACTION_COUNT] = (prefs[KEY_ACTION_COUNT] ?: 0) + 1
            }
        }

        /**
         * Pyytää arvostelua jos ehdot täyttyvät:
         * - Arvostelua ei ole vielä pyydetty
         * - Käyttäjä on Pro TAI toimintoja on kertynyt riittävästi
         *
         * Google rajoittaa näyttötiheyttä omalla kiintiöllään,
         * joten kutsu on turvallinen vaikka ehdot täyttyisivät usein.
         */
        suspend fun maybeRequestReview(
            activity: Activity,
            isPro: Boolean,
        ) {
            val prefs = context.reviewDataStore.data.first()
            if (prefs[KEY_REVIEW_REQUESTED] == true) return

            val actions = prefs[KEY_ACTION_COUNT] ?: 0
            if (!isPro && actions < ACTIONS_THRESHOLD) return

            context.reviewDataStore.edit { it[KEY_REVIEW_REQUESTED] = true }

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
        }
    }
