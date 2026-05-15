package com.finnvek.knittools.ui.screens.ravelry

import com.finnvek.knittools.auth.RavelryAuthManager
import com.finnvek.knittools.data.remote.PatternDetail
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.repository.RavelryRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RavelryViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var repository: RavelryRepository
    private lateinit var proManager: ProManager
    private lateinit var authManager: RavelryAuthManager

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        proManager = mockk()
        authManager = mockk(relaxed = true)

        every { authManager.isAuthenticated } returns MutableStateFlow(false)
        every { repository.getSavedPatterns() } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(isPro: Boolean): RavelryViewModel {
        every { proManager.hasFeature(ProFeature.UNLIMITED_PROJECTS) } returns isPro
        return RavelryViewModel(
            repository,
            proManager,
            authManager,
        )
    }

    @Test
    fun `free user with existing active project is routed to pro upgrade instead of creating project`() =
        runTest {
            val pattern = PatternDetail(id = 42, name = "Test Pattern", permalink = "test-pattern")
            coEvery { repository.getPatternDetail(42) } returns pattern
            coEvery { repository.isPatternSaved(42) } returns false
            coEvery { repository.getActiveProjectCount() } returns 1

            val vm = createViewModel(isPro = false)
            vm.loadDetail(42)
            advanceUntilIdle()
            var upgradeEvents = 0
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                vm.upgradeToPro.collect {
                    upgradeEvents += 1
                }
            }

            vm.createProjectFromPattern()

            assertEquals(1, upgradeEvents)
            coVerify(exactly = 0) { repository.createProjectFromPattern(any()) }
        }

    @Test
    fun `savePattern ignores repeated taps while save is in flight`() =
        runTest {
            val pattern = PatternDetail(id = 42, name = "Test Pattern", permalink = "test-pattern")
            coEvery { repository.getPatternDetail(42) } returns pattern
            coEvery { repository.isPatternSaved(42) } returns false
            coEvery { repository.savePattern(pattern) } returns 7L
            val vm = createViewModel(isPro = true)
            vm.loadDetail(42)
            advanceUntilIdle()

            vm.savePattern()
            vm.savePattern()
            advanceUntilIdle()

            coVerify(exactly = 1) { repository.savePattern(pattern) }
        }
}
