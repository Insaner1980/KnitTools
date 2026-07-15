package com.finnvek.knittools.ui.screens.notes

import androidx.lifecycle.SavedStateHandle
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.repository.CounterRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotesEditorViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val applicationScope = CoroutineScope(testDispatcher)
    private lateinit var repository: CounterRepository
    private lateinit var proManager: ProManager

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        proManager = mockk()
        every { proManager.hasFeature(ProFeature.NOTES) } returns true
    }

    @After
    fun tearDown() {
        applicationScope.cancel()
        Dispatchers.resetMain()
    }

    private fun vm(notes: String = ""): NotesEditorViewModel {
        val project =
            CounterProject(
                id = 1L,
                name = "Test",
                notes = notes,
            )
        every { repository.observeProject(1L) } returns flowOf(project)
        coEvery { repository.saveProjectNotes(1L, any(), any()) } answers {
            project.copy(notes = arg(2))
        }
        return NotesEditorViewModel(
            repository = repository,
            proManager = proManager,
            applicationScope = applicationScope,
            savedStateHandle = SavedStateHandle(mapOf("projectId" to 1L)),
        )
    }

    @Test
    fun `initial state loads project notes and Pro flag`() =
        runTest {
            val viewModel = vm(notes = "hello")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals("Test", state.projectName)
            assertEquals("hello", state.notes)
            assertTrue(state.isLoaded)
            assertTrue(state.isPro)
            assertFalse(state.isMissingProject)
        }

    @Test
    fun `missing project marks editor for fallback and skips saves`() =
        runTest {
            every { repository.observeProject(1L) } returns flowOf(null)
            val viewModel =
                NotesEditorViewModel(
                    repository = repository,
                    proManager = proManager,
                    applicationScope = applicationScope,
                    savedStateHandle = SavedStateHandle(mapOf("projectId" to 1L)),
                )
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isMissingProject)

            viewModel.onNotesChanged("Should not persist")
            viewModel.saveImmediately()

            coVerify(exactly = 0) { repository.updateProjectNotes(any(), any()) }
            coVerify(exactly = 0) { repository.saveProjectNotes(any(), any(), any()) }
        }

    @Test
    fun `observed external notes do not overwrite local note edits`() =
        runTest {
            val emitExternal = CompletableDeferred<Unit>()
            every { repository.observeProject(1L) } returns
                flow {
                    emit(CounterProject(id = 1L, name = "Test", notes = "Base"))
                    emitExternal.await()
                    emit(CounterProject(id = 1L, name = "Test", notes = "External edit"))
                }
            coEvery { repository.saveProjectNotes(1L, any(), any()) } answers {
                CounterProject(id = 1L, name = "Test", notes = arg(2))
            }
            val viewModel =
                NotesEditorViewModel(
                    repository = repository,
                    proManager = proManager,
                    applicationScope = applicationScope,
                    savedStateHandle = SavedStateHandle(mapOf("projectId" to 1L)),
                )
            advanceUntilIdle()

            viewModel.onNotesChanged("Local edit")
            emitExternal.complete(Unit)
            runCurrent()

            assertEquals("Local edit", viewModel.uiState.value.notes)
            viewModel.saveImmediately()
            advanceUntilIdle()
        }

    @Test
    fun `saveImmediately waits for repository save before callback`() =
        runTest {
            val events = mutableListOf<String>()
            val viewModel = vm(notes = "Base")
            advanceUntilIdle()
            coEvery {
                repository.saveProjectNotes(1L, "Base", "Local edit")
            } coAnswers {
                events += "saved"
                CounterProject(id = 1L, name = "Test", notes = "Local edit")
            }

            viewModel.onNotesChanged("Local edit")
            viewModel.saveImmediately { events += "callback" }
            advanceUntilIdle()

            assertEquals(listOf("saved", "callback"), events)
        }

    @Test
    fun `debounced save uses original persisted notes as merge base`() =
        runTest {
            coEvery {
                repository.saveProjectNotes(1L, "Base", "Local edit")
            } returns CounterProject(id = 1L, name = "Test", notes = "Local edit")
            val viewModel = vm(notes = "Base")
            advanceUntilIdle()

            viewModel.onNotesChanged("Local edit")
            advanceTimeBy(1_000)
            advanceUntilIdle()

            coVerify(exactly = 1) { repository.saveProjectNotes(1L, "Base", "Local edit") }
        }

    @Test
    fun `local edit is stored as a saved state draft during debounce`() =
        runTest {
            val project = CounterProject(id = 1L, name = "Test", notes = "Base")
            val savedStateHandle = SavedStateHandle(mapOf("projectId" to 1L))
            every { repository.observeProject(1L) } returns flowOf(project)
            val viewModel =
                NotesEditorViewModel(
                    repository = repository,
                    proManager = proManager,
                    applicationScope = applicationScope,
                    savedStateHandle = savedStateHandle,
                )
            runCurrent()

            viewModel.onNotesChanged("Local edit")

            assertEquals("Local edit", savedStateHandle.get<String>("notesDraft"))
            assertEquals("Base", savedStateHandle.get<String>("notesDraftBase"))
            coVerify(exactly = 0) { repository.saveProjectNotes(any(), any(), any()) }
        }

    @Test
    fun `restored draft survives process recreation and is autosaved`() =
        runTest {
            val project = CounterProject(id = 1L, name = "Test", notes = "Base")
            every { repository.observeProject(1L) } returns flowOf(project)
            coEvery {
                repository.saveProjectNotes(1L, "Base", "Local edit")
            } returns project.copy(notes = "Local edit")
            val savedStateHandle =
                SavedStateHandle(
                    mapOf(
                        "projectId" to 1L,
                        "notesDraft" to "Local edit",
                        "notesDraftBase" to "Base",
                    ),
                )
            val viewModel =
                NotesEditorViewModel(
                    repository = repository,
                    proManager = proManager,
                    applicationScope = applicationScope,
                    savedStateHandle = savedStateHandle,
                )

            runCurrent()

            assertEquals("Local edit", viewModel.uiState.value.notes)
            coVerify(exactly = 0) { repository.saveProjectNotes(any(), any(), any()) }

            advanceTimeBy(1_000)
            advanceUntilIdle()

            coVerify(exactly = 1) { repository.saveProjectNotes(1L, "Base", "Local edit") }
            assertNull(savedStateHandle.get<String>("notesDraft"))
            assertNull(savedStateHandle.get<String>("notesDraftBase"))
        }

    @Test
    fun `non-pro editor does not read or persist note changes`() =
        runTest {
            every { proManager.hasFeature(ProFeature.NOTES) } returns false
            val viewModel = vm(notes = "Base")
            advanceUntilIdle()

            viewModel.onNotesChanged("Local edit")
            viewModel.saveImmediately()
            advanceUntilIdle()

            assertEquals("", viewModel.uiState.value.notes)
            coVerify(exactly = 0) { repository.updateProjectNotes(any(), any()) }
            coVerify(exactly = 0) { repository.saveProjectNotes(any(), any(), any()) }
        }
}
