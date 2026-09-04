package com.finnvek.knittools.ui.screens.settings

import com.finnvek.knittools.billing.BillingManager
import com.finnvek.knittools.billing.RestorePurchasesResult
import com.finnvek.knittools.data.datastore.PreferencesManager
import com.finnvek.knittools.data.datastore.ThemeMode
import com.finnvek.knittools.pro.ProManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var preferencesManager: PreferencesManager
    private lateinit var billingManager: BillingManager
    private lateinit var proManager: ProManager

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        preferencesManager = mockk(relaxed = true)
        billingManager = mockk(relaxed = true)
        proManager = mockk(relaxed = true)
        every { preferencesManager.preferences } returns
            kotlinx.coroutines.flow.flowOf(
                com.finnvek.knittools.data.datastore
                    .AppPreferences(),
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = SettingsViewModel(preferencesManager, billingManager, proManager)

    @Test
    fun `setThemeMode calls preferencesManager`() =
        runTest {
            val vm = createViewModel()
            vm.setThemeMode(ThemeMode.DARK)

            coVerify { preferencesManager.setThemeMode(ThemeMode.DARK) }
        }

    @Test
    fun `setHapticFeedback calls preferencesManager`() =
        runTest {
            val vm = createViewModel()
            vm.setHapticFeedback(false)

            coVerify { preferencesManager.setHapticFeedback(false) }
        }

    @Test
    fun `setKeepScreenAwake calls preferencesManager`() =
        runTest {
            val vm = createViewModel()
            vm.setKeepScreenAwake(true)

            coVerify { preferencesManager.setKeepScreenAwake(true) }
        }

    @Test
    fun `setUseImperial calls preferencesManager`() =
        runTest {
            val vm = createViewModel()
            vm.setUseImperial(true)

            coVerify { preferencesManager.setUseImperial(true) }
        }

    @Test
    fun `restorePurchases calls billingManager`() =
        runTest {
            val vm = createViewModel()
            vm.restorePurchases()

            coVerify { billingManager.restorePurchasesWithResult() }
        }

    @Test
    fun `repeated restore request does not start a concurrent billing query`() =
        runTest {
            val releaseRestore = CompletableDeferred<Unit>()
            coEvery { billingManager.restorePurchasesWithResult() } coAnswers {
                releaseRestore.await()
                RestorePurchasesResult.RESTORED
            }
            val vm = createViewModel()

            vm.restorePurchases()
            vm.restorePurchases()
            runCurrent()

            assertTrue(vm.isRestoring.value)
            coVerify(exactly = 1) { billingManager.restorePurchasesWithResult() }
            releaseRestore.complete(Unit)
            runCurrent()
            assertFalse(vm.isRestoring.value)
        }

    @Test
    fun `restore result waits for a collector after recreation`() =
        runTest {
            coEvery { billingManager.restorePurchasesWithResult() } returns RestorePurchasesResult.RESTORED
            val vm = createViewModel()

            vm.restorePurchases()

            assertEquals(
                com.finnvek.knittools.R.string.pro_restored,
                withTimeoutOrNull(1) { vm.messages.first() },
            )
        }
}
