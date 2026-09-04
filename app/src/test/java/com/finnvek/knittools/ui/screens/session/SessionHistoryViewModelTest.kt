package com.finnvek.knittools.ui.screens.session

import androidx.lifecycle.SavedStateHandle
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.KnitSession
import com.finnvek.knittools.repository.CounterRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
class SessionHistoryViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val projectId = 42L

    private lateinit var repository: CounterRepository
    private lateinit var project: MutableStateFlow<CounterProject?>

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        project = MutableStateFlow(mockk())
        // Naytto lukee projektin nimen otsikkokontekstiksi, koska Insights on uusi
        // sisaankaynti eika pelkka "History" kerro kenen istuntoja katsotaan.
        every { repository.observeProject(projectId) } returns project
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): SessionHistoryViewModel {
        val savedState = SavedStateHandle(mapOf("projectId" to projectId))
        return SessionHistoryViewModel(savedState, repository)
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
    fun `all saved sessions remain visible`() =
        runTest {
            val sessions = listOf(sessionAt(1), sessionAt(48), sessionAt(100))
            every { repository.getSessionsForProject(projectId) } returns flowOf(sessions)

            val vm = createViewModel()
            val result = vm.sessions.first()

            assertEquals(3, result.size)
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
            every { repository.getSessionsForProject(projectId) } returns flowOf(listOf(older, tieLowId, tieHighId))

            val result = createViewModel().sessions.first()

            assertEquals(listOf(3L, 2L, 1L), result.map { it.id })
        }

    @Test
    fun `deleteSession delegates to repository`() =
        runTest {
            every { repository.getSessionsForProject(projectId) } returns flowOf(emptyList())
            coEvery { repository.deleteSession(7L) } returns Unit

            createViewModel().deleteSession(7L)

            io.mockk.coVerify { repository.deleteSession(7L) }
        }

    @Test
    fun `missing project marks history for fallback`() =
        runTest {
            project.value = null
            every { repository.getSessionsForProject(projectId) } returns flowOf(emptyList())

            val vm = createViewModel()

            assertTrue(vm.projectMissing.value)
        }

    @Test
    fun `deleting observed project marks history for fallback`() =
        runTest {
            every { repository.getSessionsForProject(projectId) } returns flowOf(emptyList())
            val vm = createViewModel()

            project.value = null

            assertTrue(vm.projectMissing.value)
        }
}
