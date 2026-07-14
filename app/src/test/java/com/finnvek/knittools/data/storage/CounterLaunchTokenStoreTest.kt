package com.finnvek.knittools.data.storage

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CounterLaunchTokenStoreTest {
    @Test
    fun `issued launch id can be consumed only once`() {
        val preferences = LaunchTokenPreferences()
        val launchId = CounterLaunchTokenStore.issueLaunchId(preferences.context, NOW_MILLIS)

        assertNotNull(preferences.storedValues["launch_ids"])
        assertTrue(CounterLaunchTokenStore.consumeLaunchId(preferences.context, launchId, NOW_MILLIS))
        assertFalse(CounterLaunchTokenStore.consumeLaunchId(preferences.context, launchId, NOW_MILLIS))
    }

    @Test
    fun `unused launch id expires`() {
        val preferences = LaunchTokenPreferences()
        val launchId = CounterLaunchTokenStore.issueLaunchId(preferences.context, NOW_MILLIS)

        assertFalse(
            CounterLaunchTokenStore.consumeLaunchId(
                context = preferences.context,
                launchId = launchId,
                nowMillis = NOW_MILLIS + CounterLaunchTokenStore.TOKEN_TTL_MILLIS,
            ),
        )
    }

    @Test
    fun `consuming one launch id preserves another valid launch id`() {
        val preferences = LaunchTokenPreferences()
        val firstLaunchId = CounterLaunchTokenStore.issueLaunchId(preferences.context, NOW_MILLIS)
        val secondLaunchId = CounterLaunchTokenStore.issueLaunchId(preferences.context, NOW_MILLIS)

        assertTrue(CounterLaunchTokenStore.consumeLaunchId(preferences.context, firstLaunchId, NOW_MILLIS))
        assertTrue(CounterLaunchTokenStore.consumeLaunchId(preferences.context, secondLaunchId, NOW_MILLIS))
    }

    @Test
    fun `legacy launch id without issuance time is rejected`() {
        val preferences = LaunchTokenPreferences(storedValue = "legacy-launch-id")

        assertFalse(
            CounterLaunchTokenStore.consumeLaunchId(
                context = preferences.context,
                launchId = "legacy-launch-id",
                nowMillis = NOW_MILLIS,
            ),
        )
    }

    @Test
    fun `launch id with future issuance time is rejected`() {
        val preferences = LaunchTokenPreferences()
        val launchId = CounterLaunchTokenStore.issueLaunchId(preferences.context, NOW_MILLIS)

        assertFalse(
            CounterLaunchTokenStore.consumeLaunchId(
                context = preferences.context,
                launchId = launchId,
                nowMillis = NOW_MILLIS - 1L,
            ),
        )
    }

    @Test
    fun `launch id with negative issuance time is rejected`() {
        val preferences = LaunchTokenPreferences(storedValue = "-1\tinvalid-launch-id")

        assertFalse(
            CounterLaunchTokenStore.consumeLaunchId(
                context = preferences.context,
                launchId = "invalid-launch-id",
                nowMillis = NOW_MILLIS,
            ),
        )
    }

    @Test
    fun `launch is rejected when consumption cannot be persisted`() {
        val preferences = LaunchTokenPreferences(commitSucceeds = false)
        val launchId = CounterLaunchTokenStore.issueLaunchId(preferences.context, NOW_MILLIS)

        assertFalse(CounterLaunchTokenStore.consumeLaunchId(preferences.context, launchId, NOW_MILLIS))
    }

    private class LaunchTokenPreferences(
        storedValue: String? = null,
        private val commitSucceeds: Boolean = true,
    ) {
        val storedValues = mutableMapOf<String, String>()
        private val editor = mockk<SharedPreferences.Editor>()
        private val sharedPreferences = mockk<SharedPreferences>()
        val context = mockk<Context>()

        init {
            storedValue?.let { storedValues["launch_ids"] = it }
            every { context.applicationContext } returns context
            every { context.getSharedPreferences(any(), any()) } returns sharedPreferences
            every { sharedPreferences.getString(any(), any()) } answers {
                storedValues[firstArg()] ?: secondArg()
            }
            every { sharedPreferences.edit() } returns editor
            every { editor.putString(any(), any()) } answers {
                storedValues[firstArg()] = secondArg()
                editor
            }
            every { editor.commit() } returns commitSucceeds
        }
    }

    private companion object {
        const val NOW_MILLIS = 1_000_000L
    }
}
