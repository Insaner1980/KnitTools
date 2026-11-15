package com.finnvek.knittools.pro

import android.app.Activity
import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.finnvek.knittools.data.datastore.editPreferencesSafely
import com.finnvek.knittools.data.datastore.safePreferencesData
import com.google.android.play.core.review.ReviewManagerFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InAppReviewManager
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) {
        val reviewEligibility: Flow<Boolean> =
            context.reviewDataStore.safePreferencesData
                .map { prefs ->
                    shouldRequestReview(
                        reviewRequested = prefs[KEY_REVIEW_REQUESTED] == true,
                        actionCount = prefs[KEY_ACTION_COUNT] ?: 0,
                    )
                }.distinctUntilChanged()

        /**
         * Tallentaa käyttäjän toiminnon (esim. laskurin increment/decrement).
         * Kun raja ylittyy, arvostelu voidaan pyytää.
         */
        suspend fun recordAction() {
            context.reviewDataStore.editPreferencesSafely { prefs ->
                prefs[KEY_ACTION_COUNT] = nextActionCount(prefs[KEY_ACTION_COUNT] ?: 0)
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
            var claimed = false
            var launched = false
            try {
                withContext(NonCancellable) {
                    val saved =
                        context.reviewDataStore.editPreferencesSafely { prefs ->
                            if (shouldRequestReview(
                                    prefs[KEY_REVIEW_REQUESTED] == true,
                                    prefs[KEY_ACTION_COUNT] ?: 0,
                                )
                            ) {
                                prefs[KEY_REVIEW_REQUESTED] = true
                                claimed = true
                            }
                        }
                    if (!saved) claimed = false
                }
                if (!claimed) return

                currentCoroutineContext().ensureActive()
                val manager = ReviewManagerFactory.create(context)
                val reviewInfo = manager.requestReviewFlow().await()
                currentCoroutineContext().ensureActive()
                if (activity.isFinishing ||
                    activity.isDestroyed ||
                    (activity is LifecycleOwner && activity.lifecycle.currentState != Lifecycle.State.RESUMED)
                ) {
                    return
                }
                manager.launchReviewFlow(activity, reviewInfo)
                launched = true
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                // Play review is optional. Keep the app usable after a failed request.
            } finally {
                if (claimed && !launched) {
                    withContext(NonCancellable) {
                        context.reviewDataStore.editPreferencesSafely { it[KEY_REVIEW_REQUESTED] = false }
                    }
                }
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

            internal fun nextActionCount(current: Int): Int = current.coerceIn(0, Int.MAX_VALUE - 1) + 1

            internal fun shouldRequestReview(
                reviewRequested: Boolean,
                actionCount: Int,
            ): Boolean = !reviewRequested && actionCount >= ACTIONS_THRESHOLD
        }
    }
