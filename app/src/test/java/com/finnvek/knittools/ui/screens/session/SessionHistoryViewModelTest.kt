package com.finnvek.knittools.ui.screens.session

import androidx.lifecycle.SavedStateHandle
import com.finnvek.knittools.domain.model.KnitSession
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.repository.CounterRepository
import io.mockk.coEvery
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

    private fun sessionAt(hoursAgo: Long): KnitSession {
        val time = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(hoursAgo)
        return KnitSession(
            id = hoursAgo,
            projectId = projectId,
            startedAt = time,
            endedAt = time + TimeUnit.MINUTES.toMillis(30),
            startRow = 0,
            endRow = 10,
            durationMinutes = 30,
            durationSeconds = TimeUnit.MINUTES.toSeconds(30),
            rowsWorked = 10,
        )
    }

    @Test
    fun `pro user sees all sessions`() =
        runTest {
            val sessions = listOf(sessionAt(1), sessionAt(48), sessionAt(100))
            coEvery { repository.getProject(projectId) } returns mockk()
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
            coEvery { repository.getProject(projectId) } returns mockk()
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
            coEvery { repository.getProject(projectId) } returns mockk()
            every { repository.getSessionsForProject(projectId) } returns flowOf(oldSessions)
            every { proManager.hasFeature(ProFeature.FULL_HISTORY) } returns false

            val vm = createViewModel()
            val result = vm.sessions.first()

            assertTrue(result.isEmpty())
        }

    @Test
    fun `sessions are sorted newest first with id tie breaker`() =
        runTest {
            val timestamp = System.currentTimeMillis()
            val older =
                KnitSession(
                    id = 1L,
                    projectId = projectId,
                    startedAt = timestamp - 1_000L,
                    endedAt = timestamp,
                    startRow = 0,
                    endRow = 2,
                    durationMinutes = 5,
                    durationSeconds = TimeUnit.MINUTES.toSeconds(5),
                    rowsWorked = 2,
                )
            val tieLowId = older.copy(id = 2L, startedAt = timestamp)
            val tieHighId = older.copy(id = 3L, startedAt = timestamp)
            coEvery { repository.getProject(projectId) } returns mockk()
            every { repository.getSessionsForProject(projectId) } returns flowOf(listOf(older, tieLowId, tieHighId))
            every { proManager.hasFeature(ProFeature.FULL_HISTORY) } returns true

            val result = createViewModel().sessions.first()

            assertEquals(listOf(3L, 2L, 1L), result.map { it.id })
        }

    @Test
    fun `deleteSession delegates to repository`() =
        runTest {
            coEvery { repository.getProject(projectId) } returns mockk()
            every { repository.getSessionsForProject(projectId) } returns flowOf(emptyList())
            every { proManager.hasFeature(ProFeature.FULL_HISTORY) } returns true
            coEvery { repository.deleteSession(7L) } returns Unit

            createViewModel().deleteSession(7L)

            io.mockk.coVerify { repository.deleteSession(7L) }
        }

    @Test
    fun `isPro reflects proManager state`() {
        coEvery { repository.getProject(projectId) } returns mockk()
        every { repository.getSessionsForProject(projectId) } returns flowOf(emptyList())

        every { proManager.hasFeature(ProFeature.FULL_HISTORY) } returns true
        assertTrue(createViewModel().isPro)

        every { proManager.hasFeature(ProFeature.FULL_HISTORY) } returns false
        assertFalse(createViewModel().isPro)
    }

    @Test
    fun `missing project marks history for fallback`() =
        runTest {
            coEvery { repository.getProject(projectId) } returns null
            every { repository.getSessionsForProject(projectId) } returns flowOf(emptyList())
            every { proManager.hasFeature(ProFeature.FULL_HISTORY) } returns true

            val vm = createViewModel()

            assertTrue(vm.projectMissing.value)
        }
}
