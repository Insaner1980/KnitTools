package com.finnvek.knittools.ui.screens.session

import androidx.lifecycle.SavedStateHandle
import com.finnvek.knittools.data.local.SessionEntity
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.repository.CounterRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
class SessionHistoryViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val projectId = 42L

    private lateinit var repository: CounterRepository
    private lateinit var proManager: ProManager

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        proManager = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): SessionHistoryViewModel {
        val savedState = SavedStateHandle(mapOf("projectId" to projectId))
        return SessionHistoryViewModel(savedState, repository, proManager)
    }

    private fun sessionAt(hoursAgo: Long): SessionEntity {
        val time = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(hoursAgo)
        return SessionEntity(
            projectId = projectId,
            startedAt = time,
            endedAt = time + TimeUnit.MINUTES.toMillis(30),
            startRow = 0,
            endRow = 10,
            durationMinutes = 30,
        )
    }

    @Test
    fun `pro user sees all sessions`() =
        runTest {
            val sessions = listOf(sessionAt(1), sessionAt(48), sessionAt(100))
            every { repository.getSessionsForProject(projectId) } returns flowOf(sessions)
            every { proManager.hasFeature(ProFeature.FULL_HISTORY) } returns true

            val vm = createViewModel()
            val result = vm.sessions.first()

            assertEquals(3, result.size)
        }

    @Test
    fun `free user sees only last 24h sessions`() =
        runTest {
            val recentSession = sessionAt(2)
            val oldSession = sessionAt(48)
            every { repository.getSessionsForProject(projectId) } returns flowOf(listOf(recentSession, oldSession))
            every { proManager.hasFeature(ProFeature.FULL_HISTORY) } returns false

            val vm = createViewModel()
            val result = vm.sessions.first()

            assertEquals(1, result.size)
            assertEquals(recentSession.startedAt, result.first().startedAt)
        }

    @Test
    fun `free user with no recent sessions sees empty list`() =
        runTest {
            val oldSessions = listOf(sessionAt(48), sessionAt(72))
            every { repository.getSessionsForProject(projectId) } returns flowOf(oldSessions)
            every { proManager.hasFeature(ProFeature.FULL_HISTORY) } returns false

            val vm = createViewModel()
            val result = vm.sessions.first()

            assertTrue(result.isEmpty())
        }

    @Test
    fun `isPro reflects proManager state`() {
        every { repository.getSessionsForProject(projectId) } returns flowOf(emptyList())

        every { proManager.hasFeature(ProFeature.FULL_HISTORY) } returns true
        assertTrue(createViewModel().isPro)

        every { proManager.hasFeature(ProFeature.FULL_HISTORY) } returns false
        assertFalse(createViewModel().isPro)
    }
}
