package com.finnvek.knittools.ui.screens.session

import androidx.lifecycle.SavedStateHandle
import com.finnvek.knittools.domain.model.KnitSession
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.pro.ProState
import com.finnvek.knittools.pro.ProStatus
import com.finnvek.knittools.repository.CounterRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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
    private lateinit var proState: MutableStateFlow<ProState>

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        proManager = mockk()
        proState = MutableStateFlow(ProState())
        every { proManager.proState } returns proState
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
            proState.value = ProState(status = ProStatus.PRO_PURCHASED)

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
            proState.value = ProState(status = ProStatus.PRO_PURCHASED)

            val result = createViewModel().sessions.first()

            assertEquals(listOf(3L, 2L, 1L), result.map { it.id })
        }

    @Test
    fun `deleteSession delegates to repository`() =
        runTest {
            coEvery { repository.getProject(projectId) } returns mockk()
            every { repository.getSessionsForProject(projectId) } returns flowOf(emptyList())
            coEvery { repository.deleteSession(7L) } returns Unit

            createViewModel().deleteSession(7L)

            io.mockk.coVerify { repository.deleteSession(7L) }
        }

    @Test
    fun `isPro reflects proManager state`() =
        runTest {
            coEvery { repository.getProject(projectId) } returns mockk()
            every { repository.getSessionsForProject(projectId) } returns flowOf(emptyList())

            val vm = createViewModel()
            val job = launch { vm.isPro.collect {} }
            advanceUntilIdle()

            assertFalse(vm.isPro.value)
            proState.value = ProState(status = ProStatus.PRO_PURCHASED)
            advanceUntilIdle()
            assertTrue(vm.isPro.value)

            job.cancel()
        }

    @Test
    fun `sessions update when pro purchase is restored`() =
        runTest {
            val recentSession = sessionAt(2)
            val oldSession = sessionAt(48)
            coEvery { repository.getProject(projectId) } returns mockk()
            every { repository.getSessionsForProject(projectId) } returns
                MutableStateFlow(listOf(recentSession, oldSession))

            val vm = createViewModel()
            val job = launch { vm.sessions.collect {} }
            advanceUntilIdle()

            assertEquals(listOf(recentSession.id), vm.sessions.value.map { it.id })
            proState.value = ProState(status = ProStatus.PRO_PURCHASED)
            advanceUntilIdle()
            assertEquals(listOf(recentSession.id, oldSession.id), vm.sessions.value.map { it.id })

            job.cancel()
        }

    @Test
    fun `isPro reflects proManager state through initial value`() {
        coEvery { repository.getProject(projectId) } returns mockk()
        every { repository.getSessionsForProject(projectId) } returns flowOf(emptyList())

        proState.value = ProState(status = ProStatus.PRO_PURCHASED)
        assertTrue(createViewModel().isPro.value)
    }

    @Test
    fun `missing project marks history for fallback`() =
        runTest {
            coEvery { repository.getProject(projectId) } returns null
            every { repository.getSessionsForProject(projectId) } returns flowOf(emptyList())

            val vm = createViewModel()

            assertTrue(vm.projectMissing.value)
        }
}
