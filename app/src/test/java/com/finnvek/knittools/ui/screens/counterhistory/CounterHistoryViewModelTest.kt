package com.finnvek.knittools.ui.screens.counterhistory

import androidx.lifecycle.SavedStateHandle
import com.finnvek.knittools.domain.model.CounterHistory
import com.finnvek.knittools.domain.model.CounterHistoryAction
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.repository.CounterRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
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
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class CounterHistoryViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val repository = mockk<CounterRepository>()
    private val project = MutableStateFlow<CounterProject?>(CounterProject(id = 42, name = "Cardigan"))
    private val events = MutableStateFlow<List<CounterHistory>>(emptyList())
    private val format = CounterHistoryFormat(Locale.UK, ZoneId.of("Europe/Helsinki"), "d MMM y", "HH:mm:ss")

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        every { repository.observeProject(42) } returns project
        every { repository.observeCounterHistory(42) } returns events
    }

    @After fun teardown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(id: Long? = 42): CounterHistoryViewModel =
        CounterHistoryViewModel(SavedStateHandle(mapOf("projectId" to id)), repository, dispatcher)

    @Test fun loadingWaitsForRealDataAndFormattingThenShowsEmptyProject() =
        runTest {
            val vm = viewModel()
            assertTrue(vm.uiState.value.loading)
            backgroundScope.launch(dispatcher) { vm.uiState.collect {} }
            assertTrue(vm.uiState.value.loading)
            vm.setFormat(format)
            assertFalse(vm.uiState.value.loading)
            assertEquals("Cardigan", vm.uiState.value.projectName)
            assertTrue(
                vm.uiState.value.days
                    .isEmpty(),
            )
        }

    @Test fun persistedUpdatesAndUndoRemovalReplaceVisibleRows() =
        runTest {
            val vm = viewModel()
            vm.setFormat(format)
            backgroundScope.launch(dispatcher) { vm.uiState.collect {} }
            val event = event(1, "2026-09-14T12:13:14Z")
            events.value = listOf(event)
            assertEquals(
                event,
                vm.uiState.value.days
                    .single()
                    .rows
                    .single()
                    .event,
            )
            events.value = emptyList()
            assertTrue(
                vm.uiState.value.days
                    .isEmpty(),
            )
        }

    @Test fun completionRetainsRowsAndProjectDeletionClearsThem() =
        runTest {
            events.value = listOf(event(1, "2026-09-14T12:13:14Z"))
            val vm = viewModel()
            vm.setFormat(format)
            backgroundScope.launch(dispatcher) { vm.uiState.collect {} }
            project.value = project.value?.copy(isCompleted = true)
            assertEquals(1, vm.uiState.value.days.size)
            project.value = null
            assertTrue(vm.uiState.value.projectMissing)
            assertTrue(
                vm.uiState.value.days
                    .isEmpty(),
            )
        }

    @Test fun invalidAndMissingIdsDoNotObserveAnyProject() =
        runTest {
            listOf(null, 0L, -1L).forEach { id ->
                val vm = viewModel(id)
                backgroundScope.launch(dispatcher) { vm.uiState.collect {} }
                assertTrue(vm.uiState.value.projectMissing)
            }
            verify(exactly = 0) { repository.observeCounterHistory(any()) }
        }

    @Test fun separateRouteInstancesCannotLeakProjectHistory() =
        runTest {
            every { repository.observeProject(7) } returns MutableStateFlow(CounterProject(id = 7, name = "Socks"))
            every { repository.observeCounterHistory(7) } returns MutableStateFlow(emptyList())
            events.value = listOf(event(1, "2026-09-14T12:13:14Z"))
            val first = viewModel()
            val second = viewModel(7)
            first.setFormat(format)
            second.setFormat(format)
            backgroundScope.launch(dispatcher) { first.uiState.collect {} }
            backgroundScope.launch(dispatcher) { second.uiState.collect {} }
            assertEquals(1, first.uiState.value.days.size)
            assertEquals("Socks", second.uiState.value.projectName)
            assertTrue(
                second.uiState.value.days
                    .isEmpty(),
            )
        }

    @Test fun groupsUseOneLocalZoneAndKeepRepositoryTieOrderWithSecondPrecision() {
        val rows =
            listOf(event(3, "2026-09-13T21:00:01Z"), event(2, "2026-09-13T21:00:01Z"), event(1, "2026-09-13T20:59:59Z"))
        val days = presentCounterHistory(rows, format)
        assertEquals(listOf("2026-09-14", "2026-09-13"), days.map { it.key })
        assertEquals(listOf(3L, 2L), days.first().rows.map { it.event.id })
        assertEquals(
            "00:00:01",
            days
                .first()
                .rows
                .first()
                .time,
        )
        assertEquals(
            "58",
            days
                .first()
                .rows
                .first()
                .newValue,
        )
    }

    @Test fun unknownActionsRemainVisibleAndFormattingCanChangeZoneAndLocale() {
        val unknown = event(1, "2026-09-13T21:00:01Z").copy(action = CounterHistoryAction.fromPersistedValue("future"))
        val days = presentCounterHistory(listOf(unknown), format.copy(zone = ZoneId.of("UTC"), locale = Locale.GERMAN))
        assertEquals("2026-09-13", days.single().key)
        assertEquals(
            CounterHistoryAction.CHANGED,
            days
                .single()
                .rows
                .single()
                .event.action,
        )
        assertEquals(
            "21:00:01",
            days
                .single()
                .rows
                .single()
                .time,
        )
    }

    @Test fun thousandsOfRetainedRowsAreNotTruncated() {
        val rows = (5000L downTo 1L).map { event(it, "2026-09-14T12:13:14Z") }
        assertEquals(5000, presentCounterHistory(rows, format).single().rows.size)
    }

    private fun event(
        id: Long,
        timestamp: String,
    ) = CounterHistory(id, CounterHistoryAction.INCREASE, 56, 58, Instant.parse(timestamp).toEpochMilli())
}
