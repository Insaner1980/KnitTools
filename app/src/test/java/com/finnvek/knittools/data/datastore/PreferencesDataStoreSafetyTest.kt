package com.finnvek.knittools.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class PreferencesDataStoreSafetyTest {
    @Test
    fun `observation recovers after an IO read failure`() =
        runTest {
            val store = mockk<DataStore<Preferences>>()
            val recovered = preferencesOf(stringPreferencesKey("name") to "Recovered")
            var attempts = 0
            every { store.data } returns
                flow {
                    if (++attempts == 1) throw IOException("Controlled preferences read failure")
                    emit(recovered)
                }

            assertEquals(listOf(emptyPreferences(), recovered), store.safePreferencesData.take(2).toList())
        }
}
