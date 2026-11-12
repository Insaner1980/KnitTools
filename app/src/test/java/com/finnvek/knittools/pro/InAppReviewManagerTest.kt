package com.finnvek.knittools.pro

import android.app.Activity
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.preferencesOf
import com.finnvek.knittools.data.datastore.editPreferencesSafely
import com.finnvek.knittools.data.datastore.safePreferencesData
import com.google.android.gms.tasks.Task
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InAppReviewManagerTest {
    @After
    fun tearDown() = unmockkAll()

    @Test
    fun `concurrent activity owners can claim only one review request`() =
        runTest {
            val actionCount = intPreferencesKey("action_count")
            val requested = booleanPreferencesKey("review_requested")
            val persisted = MutableStateFlow<Preferences>(preferencesOf(actionCount to 20))
            val allowWrites = CompletableDeferred<Unit>()
            val writes = Mutex()
            val context = mockk<Context>(relaxed = true)
            every { context.applicationContext } returns context
            mockkStatic("com.finnvek.knittools.data.datastore.PreferencesDataStoreSafetyKt")
            every { any<DataStore<Preferences>>().safePreferencesData } returns persisted
            coEvery { any<DataStore<Preferences>>().editPreferencesSafely(any()) } coAnswers {
                val transform = secondArg<suspend (MutablePreferences) -> Unit>()
                allowWrites.await()
                writes.withLock {
                    val next = persisted.value.toMutablePreferences()
                    transform(next)
                    persisted.value = next
                }
                true
            }
            val info = mockk<ReviewInfo>()
            val task = mockk<Task<ReviewInfo>>()
            every { task.isComplete } returns true
            every { task.exception } returns null
            every { task.isCanceled } returns false
            every { task.result } returns info
            val play = mockk<ReviewManager>(relaxed = true)
            every { play.requestReviewFlow() } returns task
            mockkStatic(ReviewManagerFactory::class)
            every { ReviewManagerFactory.create(context) } returns play
            val manager = InAppReviewManager(context)

            val first = launch { manager.maybeRequestReview(mockk<Activity>()) }
            val second = launch { manager.maybeRequestReview(mockk<Activity>()) }
            testScheduler.runCurrent()
            verify(exactly = 0) { play.requestReviewFlow() }
            allowWrites.complete(Unit)
            first.join()
            second.join()

            assertTrue(persisted.value[requested] == true)
            assertEquals(20, persisted.value[actionCount])
            verify(exactly = 1) { play.requestReviewFlow() }
        }

    @Test
    fun `recorded action count saturates without losing review eligibility`() {
        val count = InAppReviewManager.nextActionCount(Int.MAX_VALUE)

        assertEquals(Int.MAX_VALUE, count)
        assertTrue(InAppReviewManager.shouldRequestReview(false, count))
        assertEquals(Int.MAX_VALUE, InAppReviewManager.nextActionCount(Int.MAX_VALUE - 1))
    }

    @Test
    fun `recorded action count recovers invalid negative values and increments normally`() {
        assertEquals(1, InAppReviewManager.nextActionCount(Int.MIN_VALUE))
        assertEquals(1, InAppReviewManager.nextActionCount(-1))
        assertEquals(1, InAppReviewManager.nextActionCount(0))
        assertEquals(20, InAppReviewManager.nextActionCount(19))
    }

    @Test
    fun `shouldRequestReview returns false when review was already requested`() {
        val result =
            InAppReviewManager.shouldRequestReview(
                reviewRequested = true,
                actionCount = InAppReviewManager.ACTIONS_THRESHOLD,
            )

        assertFalse(result)
    }

    @Test
    fun `shouldRequestReview waits for enough recorded actions`() {
        val result =
            InAppReviewManager.shouldRequestReview(
                reviewRequested = false,
                actionCount = InAppReviewManager.ACTIONS_THRESHOLD - 1,
            )

        assertFalse(result)
    }

    @Test
    fun `shouldRequestReview allows request at action threshold`() {
        val result =
            InAppReviewManager.shouldRequestReview(
                reviewRequested = false,
                actionCount = InAppReviewManager.ACTIONS_THRESHOLD,
            )

        assertTrue(result)
    }
}
