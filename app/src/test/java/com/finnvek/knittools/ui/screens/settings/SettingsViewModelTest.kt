package com.finnvek.knittools.ui.screens.settings

import com.finnvek.knittools.ai.live.VoiceLiveQuotaManager
import com.finnvek.knittools.ai.live.VoiceLiveUsage
import com.finnvek.knittools.billing.BillingManager
import com.finnvek.knittools.data.datastore.PreferencesManager
import com.finnvek.knittools.data.datastore.ThemeMode
import com.finnvek.knittools.pro.ProManager
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var preferencesManager: PreferencesManager
    private lateinit var billingManager: BillingManager
    private lateinit var voiceLiveQuotaManager: VoiceLiveQuotaManager
    private lateinit var proManager: ProManager

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        preferencesManager = mockk(relaxed = true)
        billingManager = mockk(relaxed = true)
        voiceLiveQuotaManager = mockk(relaxed = true)
        proManager = mockk(relaxed = true)
        every { preferencesManager.preferences } returns
            flowOf(
                com.finnvek.knittools.data.datastore
                    .AppPreferences(),
            )
        every { voiceLiveQuotaManager.usage } returns
            flowOf(VoiceLiveUsage(0f, VoiceLiveQuotaManager.MONTHLY_ALLOWANCE))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() =
        SettingsViewModel(preferencesManager, billingManager, voiceLiveQuotaManager, proManager)

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
    fun `setShowKnittingTips calls preferencesManager`() =
        runTest {
            val vm = createViewModel()
            vm.setShowKnittingTips(false)

            coVerify { preferencesManager.setShowKnittingTips(false) }
        }

    @Test
    fun `restorePurchases calls billingManager`() =
        runTest {
            val vm = createViewModel()
            vm.restorePurchases()

            coVerify { billingManager.restorePurchasesWithResult() }
        }
}
