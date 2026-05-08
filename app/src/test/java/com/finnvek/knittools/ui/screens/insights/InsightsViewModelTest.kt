package com.finnvek.knittools.ui.screens.insights

import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.repository.CounterRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InsightsViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var repository: CounterRepository
    private lateinit var proManager: ProManager

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        proManager = mockk()
        every { repository.getAllProjects() } returns flowOf(emptyList())
        every { repository.getAllSessions(null) } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = InsightsViewModel(repository, proManager)

    @Test
    fun `isPro uses insights charts feature gate`() {
        every { proManager.hasFeature(ProFeature.INSIGHTS_CHARTS) } returns true
        assertTrue(createViewModel().isPro)

        every { proManager.hasFeature(ProFeature.INSIGHTS_CHARTS) } returns false
        assertFalse(createViewModel().isPro)
    }
}
