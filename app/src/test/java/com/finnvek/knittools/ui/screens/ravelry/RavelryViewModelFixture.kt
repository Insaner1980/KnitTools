package com.finnvek.knittools.ui.screens.ravelry

import androidx.lifecycle.SavedStateHandle
import com.finnvek.knittools.auth.RavelryAuthManager
import com.finnvek.knittools.auth.RavelryAuthState
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.repository.RavelryRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before

@OptIn(ExperimentalCoroutinesApi::class)
abstract class RavelryViewModelFixture {
    protected lateinit var testDispatcher: TestDispatcher

    protected lateinit var repository: RavelryRepository
    protected lateinit var proManager: ProManager
    protected lateinit var authManager: RavelryAuthManager
    protected lateinit var authState: MutableStateFlow<RavelryAuthState>

    @Before
    fun setup() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        proManager = mockk()
        every { proManager.proState } returns
            MutableStateFlow(
                com.finnvek.knittools.pro
                    .ProState(),
            )
        authManager = mockk(relaxed = true)
        authState = MutableStateFlow(RavelryAuthState.NotConnected)

        every { authManager.authState } returns authState
        coEvery { authManager.refreshAuthStatus() } returns RavelryAuthState.NotConnected
        every { repository.getSavedPatterns() } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    protected fun createViewModel(
        isPro: Boolean,
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
    ): RavelryViewModel {
        every { proManager.hasFeature(ProFeature.UNLIMITED_PROJECTS) } returns isPro
        return RavelryViewModel(
            repository,
            proManager,
            authManager,
            savedStateHandle,
        )
    }
}
