package com.finnvek.knittools.ui.screens.notes

import androidx.lifecycle.SavedStateHandle
import com.finnvek.knittools.data.storage.NotesDraft
import com.finnvek.knittools.data.storage.NotesDraftStorage
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.pro.ProState
import com.finnvek.knittools.pro.ProStatus
import com.finnvek.knittools.repository.CancelledNotesMerge
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.repository.ProjectNotesSaveResult
import com.finnvek.knittools.repository.mergeProjectNotes
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
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
import java.io.IOException
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalCoroutinesApi::class)
abstract class NotesEditorTestFixture {
    private val testDispatcher = StandardTestDispatcher()
    protected val applicationScope = CoroutineScope(testDispatcher)
    protected lateinit var repository: CounterRepository
    protected lateinit var proManager: ProManager

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        proManager = mockk()
        every { proManager.proState } returns MutableStateFlow(ProState())
        every { proManager.hasFeature(ProFeature.NOTES) } returns true
        coEvery {
            repository.saveProjectNotes(any(), any(), any(), any())
        } returns ProjectNotesSaveResult.PersistenceFailure
    }

    @After
    fun tearDown() {
        applicationScope.cancel()
        Dispatchers.resetMain()
    }

    protected fun vm(
        notes: String = "",
        notesCreated: Boolean = notes.isNotBlank(),
    ): NotesEditorViewModel {
        val project =
            CounterProject(
                id = 1L,
                name = "Test",
                notes = notes,
                notesCreated = notesCreated,
            )
        every { repository.observeProject(1L) } returns flowOf(project)
        coEvery { repository.saveProjectNotes(1L, any(), any(), any()) } answers {
            ProjectNotesSaveResult.Saved(project.copy(notes = arg(2)))
        }
        return createEditor()
    }

    protected fun createEditor(
        savedStateHandle: SavedStateHandle = SavedStateHandle(mapOf("projectId" to 1L)),
        draftStorage: NotesDraftStorage = mockk(),
    ): NotesEditorViewModel =
        NotesEditorViewModel(repository, proManager, applicationScope, savedStateHandle, draftStorage)
}

@OptIn(ExperimentalCoroutinesApi::class)
class NotesEditorViewModelTest : NotesEditorTestFixture() {
    @Test
    fun `initial state loads project notes and Pro flag`() =
        runTest {
            val viewModel = vm(notes = "hello")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals("Test", state.projectName)
            assertEquals("hello", state.notes)
            assertTrue(state.isLoaded)
            assertTrue(state.canEditNotes)
            assertFalse(state.isMissingProject)
        }

    @Test
    fun `missing project marks editor for fallback and skips saves`() =
        runTest {
            every { repository.observeProject(1L) } returns flowOf(null)
            val viewModel = createEditor()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isMissingProject)

            viewModel.onNotesChanged("Should not persist")
            viewModel.saveImmediately()

            coVerify(exactly = 0) { repository.saveProjectNotes(any(), any(), any(), any()) }
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
            coEvery { repository.saveProjectNotes(1L, any(), any(), any()) } answers {
                ProjectNotesSaveResult.Saved(CounterProject(id = 1L, name = "Test", notes = arg(2)))
            }
            val viewModel = createEditor()
            advanceUntilIdle()

            viewModel.onNotesChanged("Local edit")
            emitExternal.complete(Unit)
            runCurrent()

            assertEquals("Local edit", viewModel.uiState.value.notes)
            viewModel.saveImmediately()
            advanceUntilIdle()
        }

    @Test
    fun `saveImmediately saves without waiting for the debounce`() =
        runTest {
            val viewModel = vm(notes = "Base")
            runCurrent()

            viewModel.onNotesChanged("Local edit")
            viewModel.saveImmediately(exitAfterSave = true)
            runCurrent()

            coVerify(exactly = 1) { repository.saveProjectNotes(1L, "Base", "Local edit", false) }
            assertTrue(viewModel.consumeExitRequest())
        }

    @Test
    fun `repeated immediate saves join the in-flight write and exit once`() =
        runTest {
            val viewModel = vm(notes = "Base")
            val result = CompletableDeferred<ProjectNotesSaveResult>()
            coEvery { repository.saveProjectNotes(1L, "Base", "Local edit", false) } coAnswers { result.await() }
            runCurrent()

            viewModel.onNotesChanged("Local edit")
            advanceTimeBy(1_000)
            runCurrent()
            repeat(2) {
                viewModel.saveImmediately(exitAfterSave = true)
                runCurrent()
            }
            viewModel.saveImmediately()
            runCurrent()
            assertFalse(viewModel.consumeExitRequest())
            result.complete(ProjectNotesSaveResult.Saved(CounterProject(id = 1L, name = "Test", notes = "Local edit")))
            runCurrent()

            coVerify(exactly = 1) { repository.saveProjectNotes(any(), any(), any(), any()) }
            assertTrue(viewModel.consumeExitRequest())
            assertFalse(viewModel.consumeExitRequest())
        }

    @Test
    fun `debounced save uses original persisted notes as merge base`() =
        runTest {
            coEvery {
                repository.saveProjectNotes(1L, "Base", "Local edit", false)
            } returns ProjectNotesSaveResult.Saved(CounterProject(id = 1L, name = "Test", notes = "Local edit"))
            val viewModel = vm(notes = "Base")
            advanceUntilIdle()

            viewModel.onNotesChanged("Earlier edit")
            runCurrent()
            advanceTimeBy(600)
            viewModel.onNotesChanged("Local edit")
            runCurrent()
            advanceTimeBy(999)
            runCurrent()
            coVerify(exactly = 0) { repository.saveProjectNotes(any(), any(), any(), any()) }

            advanceTimeBy(1)
            runCurrent()

            coVerify(exactly = 1) { repository.saveProjectNotes(1L, "Base", "Local edit", false) }
        }

    @Test
    fun `local edit is stored as a saved state draft during debounce`() =
        runTest {
            val project = CounterProject(id = 1L, name = "Test", notes = "Base")
            val savedStateHandle = SavedStateHandle(mapOf("projectId" to 1L))
            every { repository.observeProject(1L) } returns flowOf(project)
            val viewModel = createEditor(savedStateHandle)
            runCurrent()

            viewModel.onNotesChanged("Local edit")

            assertEquals("Local edit", savedStateHandle.get<String>("notesDraft"))
            assertEquals("Base", savedStateHandle.get<String>("notesDraftBase"))
            coVerify(exactly = 0) { repository.saveProjectNotes(any(), any(), any(), any()) }
        }

    @Test
    fun `reconstructed saved state restores the draft and autosaves it`() =
        runTest {
            val project = CounterProject(id = 1L, name = "Test", notes = "External edit")
            val mergedNotes = "Local edit\n\n---\n\nExternal edit"
            every { repository.observeProject(1L) } returns flowOf(project)
            coEvery {
                repository.saveProjectNotes(1L, "Base", "Local edit", false)
            } returns ProjectNotesSaveResult.Saved(project.copy(notes = mergedNotes))
            val savedStateHandle =
                SavedStateHandle(
                    mapOf(
                        "projectId" to 1L,
                        "notesDraft" to "Local edit",
                        "notesDraftBase" to "Base",
                    ),
                )
            val viewModel = createEditor(savedStateHandle)

            runCurrent()

            assertEquals("Local edit", viewModel.uiState.value.notes)
            assertEquals("Base", savedStateHandle.get<String>("notesDraftBase"))
            coVerify(exactly = 0) { repository.saveProjectNotes(any(), any(), any(), any()) }

            advanceTimeBy(1_000)
            advanceUntilIdle()

            coVerify(exactly = 1) { repository.saveProjectNotes(1L, "Base", "Local edit", false) }
            assertEquals(mergedNotes, viewModel.uiState.value.notes)
            assertNull(savedStateHandle.get<String>("notesDraft"))
            assertNull(savedStateHandle.get<String>("notesDraftBase"))
        }

    @Test
    fun `correcting a word during a committed save does not restore the old word`() =
        runTest {
            val project = observeProjectWithBaseNotes()
            var storedNotes = project.notes
            val committedSaveResponse = CompletableDeferred<Unit>()
            coEvery { repository.getProject(1L) } coAnswers { project.copy(notes = storedNotes) }
            coEvery { repository.saveProjectNotes(1L, any(), any(), false) } coAnswers {
                val requestedNotes = arg<String>(2)
                storedNotes = mergeProjectNotes(arg(1), requestedNotes, storedNotes)
                if (requestedNotes == "Base red") committedSaveResponse.await()
                ProjectNotesSaveResult.Saved(project.copy(notes = storedNotes))
            }
            val viewModel = createEditor()
            runCurrent()

            viewModel.onNotesChanged("Base red")
            viewModel.saveImmediately()
            runCurrent()
            assertEquals("Base red", storedNotes)

            viewModel.onNotesChanged("Base blue")
            committedSaveResponse.complete(Unit)
            advanceUntilIdle()

            assertEquals("Base blue", storedNotes)
            assertEquals("Base blue", viewModel.uiState.value.notes)
        }

    @Test
    fun `replaying an interrupted merge does not restore text deleted after the commit`() =
        runTest {
            val project = CounterProject(id = 1L, name = "Test", notes = "Base", notesCreated = true)
            val projects = MutableStateFlow(project)
            every { repository.observeProject(1L) } returns projects
            val merged = "Base independent red"
            coEvery { repository.saveProjectNotes(1L, "Base", "Base red", false) } coAnswers {
                try {
                    awaitCancellation()
                } catch (cancelled: CancellationException) {
                    throw CancelledNotesMerge("Base independent", merged, cancelled)
                }
            }
            val finishRead = CompletableDeferred<Unit>()
            coEvery { repository.getProject(1L) } coAnswers {
                finishRead.await()
                project
            }
            coEvery { repository.saveProjectNotes(1L, "Base", "Base blue", false) } returns
                ProjectNotesSaveResult.Saved(project.copy(notes = "Base blue"))
            val viewModel = createEditor()
            runCurrent()
            viewModel.onNotesChanged("Base red")
            viewModel.saveImmediately()
            runCurrent()
            viewModel.onNotesChanged("Base blue")
            viewModel.saveImmediately(exitAfterSave = true)
            runCurrent()

            // The observer is still delivering the earlier merge; the database already holds the deletion.
            projects.value = project.copy(notes = merged)
            runCurrent()
            finishRead.complete(Unit)
            advanceUntilIdle()

            assertTrue(viewModel.consumeExitRequest())
            assertEquals("Base blue", viewModel.uiState.value.notes)
            coVerify(exactly = 1) { repository.saveProjectNotes(1L, "Base", "Base blue", false) }
        }

    @Test
    fun `missing project after interrupted autosave prevents another write`() =
        runTest {
            observeProjectWithBaseNotes()
            val obsoleteResult = CompletableDeferred<ProjectNotesSaveResult>()
            coEvery { repository.saveProjectNotes(1L, "Base", "Original edit", false) } coAnswers {
                obsoleteResult.await()
            }
            coEvery { repository.getProject(1L) } returns null
            val viewModel = createEditor()
            runCurrent()

            viewModel.onNotesChanged("Original edit")
            viewModel.saveImmediately()
            runCurrent()
            viewModel.onNotesChanged("Newer edit")
            viewModel.saveImmediately(exitAfterSave = true)
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isMissingProject)
            assertFalse(viewModel.uiState.value.exitRequested)
            coVerify(exactly = 1) { repository.saveProjectNotes(1L, any(), any(), false) }
        }

    @Test
    fun `late merged save preserves both newer typing and the external addition`() =
        runTest {
            val project = observeProjectWithBaseNotes()
            val handle = SavedStateHandle(mapOf("projectId" to 1L))
            var completeFirstSave: ((ProjectNotesSaveResult) -> Unit)? = null
            coEvery { repository.saveProjectNotes(1L, "Base", "Draft A", false) } coAnswers {
                suspendCoroutine { continuation -> completeFirstSave = { continuation.resume(it) } }
            }
            val firstMerge = "Draft A\nExternal addition"
            val secondMerge = "Draft B\nExternal addition"
            coEvery { repository.saveProjectNotes(1L, firstMerge, secondMerge, false) } returns
                ProjectNotesSaveResult.Saved(project.copy(notes = secondMerge))
            val viewModel = createEditor(handle)
            runCurrent()

            viewModel.onNotesChanged("Draft A")
            advanceTimeBy(1_000)
            runCurrent()
            viewModel.onNotesChanged("Draft B")
            runCurrent()
            requireNotNull(completeFirstSave)(ProjectNotesSaveResult.Saved(project.copy(notes = firstMerge)))
            runCurrent()

            assertEquals(secondMerge, viewModel.uiState.value.notes)
            assertEquals(secondMerge, handle.get<String>("notesDraft"))
            assertEquals(firstMerge, handle.get<String>("notesDraftBase"))

            advanceTimeBy(1_000)
            runCurrent()

            coVerify(exactly = 1) { repository.saveProjectNotes(1L, firstMerge, secondMerge, false) }
            assertEquals(secondMerge, viewModel.uiState.value.notes)
            assertNull(handle.get<String>("notesDraft"))
            assertNull(handle.get<String>("notesDraftBase"))
        }

    @Test
    fun `failed immediate save keeps editor open and draft available`() =
        runTest {
            val savedStateHandle = SavedStateHandle(mapOf("projectId" to 1L))
            every { repository.observeProject(1L) } returns
                flowOf(CounterProject(id = 1L, name = "Test", notes = "Base", notesCreated = true))
            coEvery {
                repository.saveProjectNotes(1L, "Base", "Local edit", false)
            } returns ProjectNotesSaveResult.PersistenceFailure
            val viewModel = createEditor(savedStateHandle)
            advanceUntilIdle()
            viewModel.onNotesChanged("Local edit")
            viewModel.saveImmediately(exitAfterSave = true)
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.exitRequested)
            assertEquals("Local edit", viewModel.uiState.value.notes)
            assertEquals("Local edit", savedStateHandle.get<String>("notesDraft"))
        }

    @Test
    fun `autosave failure is visible and retry preserves the merge base and clears the error`() =
        runTest {
            val project = CounterProject(id = 1L, name = "Test", notes = "Base", notesCreated = true)
            val handle = SavedStateHandle(mapOf("projectId" to 1L))
            val projects = MutableStateFlow(project)
            every { repository.observeProject(1L) } returns projects
            val viewModel = createEditor(handle)
            runCurrent()
            viewModel.onNotesChanged("Local edit")
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.saveFailed)
            assertEquals("Local edit", handle.get<String>("notesDraft"))
            projects.value = project.copy(notes = "External edit")
            runCurrent()
            assertEquals("Local edit", viewModel.uiState.value.notes)
            coEvery { repository.saveProjectNotes(1L, "Base", "Local edit", false) } returns
                ProjectNotesSaveResult.Saved(project.copy(notes = "External edit\n\nLocal edit"))
            viewModel.saveImmediately(exitAfterSave = true)
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.exitRequested)
            assertFalse(viewModel.uiState.value.saveFailed)
            assertEquals("External edit\n\nLocal edit", viewModel.uiState.value.notes)
            assertNull(handle.get<String>("notesDraft"))
            assertNull(handle.get<String>("notesDraftBase"))
        }

    @Test
    fun `discard serializes deletion behind old draft write without touching the new draft file`() =
        runTest {
            observeProjectWithBaseNotes()
            val storage = mockk<NotesDraftStorage>()
            val files = mutableMapOf<UUID, NotesDraft>()
            val handle = SavedStateHandle(mapOf("projectId" to 1L))
            val old = "a".repeat(17_000)
            val newer = "b".repeat(17_000)
            var finishOldWrite: (() -> Unit)? = null
            var oldId: UUID? = null
            coEvery { storage.write(1L, any(), any()) } coAnswers {
                val id = secondArg<UUID>()
                val draft = thirdArg<NotesDraft>()
                if (draft.notes == old) {
                    oldId = id
                    suspendCoroutine { continuation -> finishOldWrite = { continuation.resume(Unit) } }
                }
                files[id] = draft
                true
            }
            coEvery { storage.delete(1L, any()) } answers {
                files.remove(secondArg<UUID>())
            }
            val vm = createEditor(handle, storage)
            runCurrent()
            vm.onNotesChanged("Recoverable inline draft")
            vm.onNotesChanged(old)
            runCurrent()
            assertEquals(requireNotNull(oldId).toString(), handle.get<String>("notesDraftFileId"))
            assertEquals("Recoverable inline draft", handle.get<String>("notesDraft"))

            vm.discardChanges()
            vm.onNotesChanged(newer)
            runCurrent()
            val newId = UUID.fromString(handle.get<String>("notesDraftFileId"))
            assertTrue(oldId != newId)
            requireNotNull(finishOldWrite).invoke()
            runCurrent()

            assertEquals(mapOf(newId to NotesDraft(newer, "Base")), files)
            assertEquals(newId.toString(), handle.get<String>("notesDraftFileId"))
        }

    @Test
    fun `explicit discard cancels debounce and prevents the final persistence flush`() =
        runTest {
            observeProjectWithBaseNotes()
            val handle = SavedStateHandle(mapOf("projectId" to 1L))
            val viewModel = createEditor(handle)
            runCurrent()
            viewModel.onNotesChanged("Failed edit")
            viewModel.saveImmediately()
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.saveFailed)
            viewModel.onNotesChanged("Later edit")
            viewModel.discardChanges()
            val store = androidx.lifecycle.ViewModelStore()
            store.put("editor", viewModel)
            store.clear()
            advanceUntilIdle()
            assertNull(handle.get<String>("notesDraft"))
            assertNull(handle.get<String>("notesDraftBase"))
            assertFalse(viewModel.uiState.value.saveFailed)
            coVerify(exactly = 1) { repository.saveProjectNotes(any(), any(), any(), any()) }
        }

    @Test
    fun `clearing the editor retains the latest long draft when final save fails`() =
        runTest {
            observeProjectWithBaseNotes()
            val handle = SavedStateHandle(mapOf("projectId" to 1L))
            val storage = mockk<NotesDraftStorage>()
            val files = mutableMapOf<UUID, NotesDraft>()
            val notes = "Latest notes. ".repeat(1500)
            coEvery { storage.write(1L, any(), any()) } answers {
                files[secondArg<UUID>()] = thirdArg<NotesDraft>()
                true
            }
            coEvery { repository.saveProjectNotes(1L, "Base", notes, false) } returns
                ProjectNotesSaveResult.PersistenceFailure
            val viewModel = createEditor(handle, storage)
            val store = androidx.lifecycle.ViewModelStore()
            store.put("editor", viewModel)
            runCurrent()
            viewModel.onNotesChanged(notes)
            val fileId = UUID.fromString(requireNotNull(handle.get<String>("notesDraftFileId")))
            store.clear()
            advanceUntilIdle()

            assertEquals(NotesDraft(notes, "Base"), files[fileId])
            coVerify(exactly = 1) { repository.saveProjectNotes(1L, "Base", notes, false) }
        }

    @Test
    fun `a failed shutdown reconciliation read leaves the last long edit recoverable`() =
        runTest {
            val project = observeProjectWithBaseNotes()
            val handle = SavedStateHandle(mapOf("projectId" to 1L))
            val storage = mockk<NotesDraftStorage>(relaxUnitFun = true)
            val files = mutableMapOf<UUID, NotesDraft>()
            coEvery { storage.write(1L, any(), any()) } answers {
                files[secondArg<UUID>()] = thirdArg<NotesDraft>()
                true
            }
            coEvery { storage.read(1L, any()) } answers { files[secondArg<UUID>()] }
            coEvery { repository.saveProjectNotes(1L, "Base", "First edit", false) } coAnswers { awaitCancellation() }
            coEvery { repository.getProject(1L) } throws IOException("Read failed")
            val viewModel = createEditor(handle, storage)
            val store = androidx.lifecycle.ViewModelStore()
            store.put("editor", viewModel)
            runCurrent()
            viewModel.onNotesChanged("First edit")
            viewModel.saveImmediately()
            runCurrent()

            val notes = "Base" + " long edit".repeat(2_000)
            viewModel.onNotesChanged(notes)
            store.clear()
            advanceUntilIdle()

            val fileId = UUID.fromString(requireNotNull(handle.get<String>("notesDraftFileId")))
            assertEquals(NotesDraft(notes, "Base"), files[fileId])
            coVerify(exactly = 1) { repository.getProject(1L) }
            val external = "Base external"
            every { repository.observeProject(1L) } returns flowOf(project.copy(notes = external))
            coEvery { repository.saveProjectNotes(1L, "Base", notes, false) } answers {
                ProjectNotesSaveResult.Saved(project.copy(notes = mergeProjectNotes(arg(1), arg(2), external)))
            }
            val restored = createEditor(handle, storage)
            runCurrent()
            assertEquals(notes, restored.uiState.value.notes)
            advanceUntilIdle()
            assertEquals(external + notes.removePrefix("Base"), restored.uiState.value.notes)
        }

    @Test
    fun `typing the previous text replaces an edit observed during its save`() =
        runTest {
            val project = CounterProject(id = 1L, name = "Test", notes = "Base", notesCreated = true)
            val projects = MutableStateFlow(project)
            every { repository.observeProject(1L) } returns projects
            coEvery { repository.getProject(1L) } coAnswers { projects.value }
            coEvery { repository.saveProjectNotes(1L, "Base", "Earlier edit", false) } coAnswers {
                projects.value = project.copy(notes = "Earlier edit")
                kotlinx.coroutines.awaitCancellation()
            }
            coEvery { repository.saveProjectNotes(1L, "Earlier edit", "Base", false) } returns
                ProjectNotesSaveResult.Saved(project)
            val handle = SavedStateHandle(mapOf("projectId" to 1L))
            val viewModel = createEditor(handle)
            runCurrent()
            viewModel.onNotesChanged("Earlier edit")
            viewModel.saveImmediately()
            runCurrent()

            viewModel.onNotesChanged("Base")
            assertEquals("Base", viewModel.uiState.value.notes)
            advanceUntilIdle()

            coVerify(exactly = 1) { repository.saveProjectNotes(1L, "Earlier edit", "Base", false) }
            assertEquals("Base", viewModel.uiState.value.notes)
        }

    @Test
    fun `discard retains an acknowledged save before its observation arrives`() =
        runTest {
            val viewModel = vm(notes = "Base")
            runCurrent()
            viewModel.onNotesChanged("Saved text")
            viewModel.saveImmediately()
            runCurrent()

            viewModel.onNotesChanged("Abandoned edit")
            viewModel.discardChanges()
            advanceUntilIdle()

            assertEquals("Saved text", viewModel.uiState.value.notes)
            coVerify(exactly = 1) { repository.saveProjectNotes(any(), any(), any(), any()) }
        }

    private fun observeProjectWithBaseNotes(): CounterProject {
        val project = CounterProject(id = 1L, name = "Test", notes = "Base", notesCreated = true)
        every { repository.observeProject(1L) } returns flowOf(project)
        return project
    }

    @Test
    fun `discard preserves an observation newer than the pending save result`() =
        runTest {
            val project = CounterProject(id = 1L, name = "Test", notes = "Base", notesCreated = true)
            val projects = MutableStateFlow(project)
            val result = CompletableDeferred<ProjectNotesSaveResult>()
            every { repository.observeProject(1L) } returns projects
            coEvery { repository.saveProjectNotes(any(), any(), any(), any()) } coAnswers { result.await() }
            val viewModel = createEditor()
            runCurrent()
            viewModel.onNotesChanged("Saved earlier")
            viewModel.saveImmediately()
            runCurrent()
            projects.value = project.copy(notes = "Saved later by another editor")
            runCurrent()
            result.complete(ProjectNotesSaveResult.Saved(project.copy(notes = "Saved earlier")))
            runCurrent()

            viewModel.onNotesChanged("Abandoned edit")
            viewModel.discardChanges()
            advanceUntilIdle()

            assertEquals("Saved later by another editor", viewModel.uiState.value.notes)
            coVerify(exactly = 1) { repository.saveProjectNotes(any(), any(), any(), any()) }
        }

    @Test
    fun `discard adopts the latest observed notes without saving the abandoned draft`() =
        runTest {
            val project = CounterProject(id = 1L, name = "Test", notes = "Base", notesCreated = true)
            val projects = MutableStateFlow(project)
            every { repository.observeProject(1L) } returns projects
            val handle = SavedStateHandle(mapOf("projectId" to 1L))
            val viewModel = createEditor(handle)
            runCurrent()

            viewModel.onNotesChanged("Abandoned local edit")
            projects.value = project.copy(notes = "Saved by another editor")
            runCurrent()
            assertEquals("Abandoned local edit", viewModel.uiState.value.notes)

            viewModel.discardChanges()
            advanceUntilIdle()

            assertEquals("Saved by another editor", viewModel.uiState.value.notes)
            assertNull(handle.get<String>("notesDraft"))
            coVerify(exactly = 0) { repository.saveProjectNotes(any(), any(), any(), any()) }

            viewModel.onNotesChanged("Saved by another editor\nNew edit")
            viewModel.saveImmediately()
            advanceUntilIdle()
            coVerify(exactly = 1) {
                repository.saveProjectNotes(1L, "Saved by another editor", "Saved by another editor\nNew edit", false)
            }
        }

    @Test
    fun `final persistence propagates repository cancellation`() =
        runTest {
            val viewModel = vm(notes = "Base")
            runCurrent()
            viewModel.onNotesChanged("Unsaved edit")
            val cancellation = CancellationException("Cancelled persistence")
            coEvery { repository.saveProjectNotes(1L, "Base", "Unsaved edit", false) } throws cancellation
            val store = androidx.lifecycle.ViewModelStore()
            store.put("editor", viewModel)

            store.clear()
            val flush = applicationScope.coroutineContext[Job]?.children?.single()
            var completionCause: Throwable? = null
            checkNotNull(flush).invokeOnCompletion { completionCause = it }
            runCurrent()

            assertTrue(flush.isCancelled)
            assertEquals(cancellation, completionCause)
            coVerify(exactly = 1) { repository.saveProjectNotes(1L, "Base", "Unsaved edit", false) }
        }
}

@OptIn(ExperimentalCoroutinesApi::class)
class NotesEditorDraftRecoveryTest : NotesEditorTestFixture() {
    @Test
    fun `failed final save restores a reconciled draft that grew too large for saved state`() =
        verifyRestoredCorrection("Base" + " external".repeat(2_000), expectFile = true)

    @Test
    fun `failed final save restores a reconciled draft that still fits inline`() =
        verifyRestoredCorrection("Base external", expectFile = false)

    @Test
    fun `failed file and database writes restore the correction against the stored note`() =
        verifyRestoredCorrection("Base" + " external".repeat(2_000), expectFile = true, fileWriteSucceeds = false)

    @Test
    fun `a recovery patch applies only to the matching note and recognizes an already saved result`() {
        val base = "Header red footer"
        val edited = "Header blue footer"
        val patch = requireNotNull(encodeNotesDraftPatch(base, edited, 512))
        assertEquals(NotesDraft(edited, base), restoreNotesDraftPatch(patch, base))
        assertEquals(NotesDraft(edited, edited), restoreNotesDraftPatch(patch, edited))
        assertNull(restoreNotesDraftPatch(patch, "Header new footer"))
        assertNull(restoreNotesDraftPatch("invalid", base))
        assertNull(encodeNotesDraftPatch(base, "x".repeat(513), 512))
    }

    @Test
    fun `a recovery patch with a changed database base requires an explicit retry`() =
        runTest {
            val project = CounterProject(id = 1L, name = "Test", notes = "Changed independently", notesCreated = true)
            every { repository.observeProject(1L) } returns flowOf(project)
            val patch = requireNotNull(encodeNotesDraftPatch("Base external red", "Base external blue", 512))
            val handle =
                SavedStateHandle(
                    mapOf(
                        "projectId" to 1L,
                        "notesDraft" to "Base blue",
                        "notesDraftBase" to "Base",
                        "notesDraftPatch" to patch,
                    ),
                )
            val editor = createEditor(handle)
            advanceUntilIdle()
            assertEquals("Base blue", editor.uiState.value.notes)
            assertTrue(editor.uiState.value.saveFailed)
            val store = androidx.lifecycle.ViewModelStore()
            store.put("editor", editor)
            store.clear()
            advanceUntilIdle()
            coVerify(exactly = 0) { repository.saveProjectNotes(any(), any(), any(), any()) }

            val reopened = createEditor(handle)
            advanceUntilIdle()
            reopened.saveImmediately()
            advanceUntilIdle()
            coVerify(exactly = 1) { repository.saveProjectNotes(1L, "Base", "Base blue", false) }
        }

    private fun verifyRestoredCorrection(
        external: String,
        expectFile: Boolean,
        fileWriteSucceeds: Boolean = true,
    ) = runTest {
        val project = CounterProject(id = 1L, name = "Test", notes = "Base", notesCreated = true)
        val stored = "$external red"
        val corrected = "$external blue"
        every { repository.observeProject(1L) } returns flowOf(project)
        coEvery { repository.getProject(1L) } returns project.copy(notes = stored)
        coEvery { repository.saveProjectNotes(1L, "Base", "Base red", false) } coAnswers {
            try {
                awaitCancellation()
            } catch (cancelled: CancellationException) {
                throw CancelledNotesMerge(external, stored, cancelled)
            }
        }
        val storage = mockk<NotesDraftStorage>(relaxUnitFun = true)
        val files = mutableMapOf<UUID, NotesDraft>()
        coEvery { storage.write(1L, any(), any()) } answers {
            if (fileWriteSucceeds) files[secondArg<UUID>()] = thirdArg<NotesDraft>()
            fileWriteSucceeds
        }
        coEvery { storage.read(1L, any()) } answers { files[secondArg<UUID>()] }
        val handle = SavedStateHandle(mapOf("projectId" to 1L))
        val editor = createEditor(handle, storage)
        val store = androidx.lifecycle.ViewModelStore()
        store.put("editor", editor)
        runCurrent()
        editor.onNotesChanged("Base red")
        editor.saveImmediately()
        runCurrent()
        editor.onNotesChanged("Base blue")
        assertNull(handle.get<String>("notesDraftFileId"))
        store.clear()
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.saveProjectNotes(1L, stored, corrected, false) }
        val fileId = handle.get<String>("notesDraftFileId")
        assertEquals(expectFile, fileId != null)
        if (fileId == null) {
            assertEquals(corrected, handle.get<String>("notesDraft"))
            assertEquals(stored, handle.get<String>("notesDraftBase"))
        } else if (fileWriteSucceeds) {
            assertEquals(NotesDraft(corrected, stored), files[UUID.fromString(fileId)])
            assertNull(handle.get<String>("notesDraft"))
        } else {
            assertTrue(files.isEmpty())
            assertTrue(requireNotNull(handle.get<String>("notesDraftPatch")).length < stored.length)
        }
        every { repository.observeProject(1L) } returns flowOf(project.copy(notes = stored))
        coEvery { repository.saveProjectNotes(1L, stored, corrected, false) } returns
            ProjectNotesSaveResult.Saved(project.copy(notes = corrected))
        val restoredHandle = SavedStateHandle(handle.keys().associateWith { handle.get<Any>(it) })
        val restored = createEditor(restoredHandle, storage)
        runCurrent()
        assertEquals(corrected, restored.uiState.value.notes)
        advanceUntilIdle()
        coVerify(exactly = 2) { repository.saveProjectNotes(1L, stored, corrected, false) }
        assertNull(restoredHandle.get<String>("notesDraftPatch"))
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class NotesEditorAccessTest : NotesEditorTestFixture() {
    @Test
    fun `late entitlement unlocks first notes and preserves the draft across access changes`() =
        runTest {
            val entitlement = MutableStateFlow(ProState())
            every { proManager.proState } returns entitlement
            every { proManager.hasFeature(ProFeature.NOTES) } answers {
                entitlement.value.hasFeature(ProFeature.NOTES, debugUnlockAllFeatures = false)
            }
            val handle =
                SavedStateHandle(
                    mapOf("projectId" to 1L, "notesDraft" to "Restored draft", "notesDraftBase" to ""),
                )
            every { repository.observeProject(1L) } returns
                flowOf(CounterProject(id = 1L, name = "Test", notesCreated = false))
            val viewModel = createEditor(handle)
            runCurrent()
            assertFalse(viewModel.uiState.value.canEditNotes)

            entitlement.value = ProState(status = ProStatus.TRIAL_ACTIVE)
            runCurrent()
            assertTrue(viewModel.uiState.value.canEditNotes)
            assertEquals("Restored draft", viewModel.uiState.value.notes)
            viewModel.onNotesChanged("Edited draft")

            entitlement.value = ProState(status = ProStatus.TRIAL_EXPIRED)
            runCurrent()
            assertFalse(viewModel.uiState.value.canEditNotes)
            assertEquals("Edited draft", viewModel.uiState.value.notes)
            assertEquals("Edited draft", handle.get<String>("notesDraft"))

            entitlement.value = ProState(status = ProStatus.PRO_PURCHASED)
            runCurrent()
            assertTrue(viewModel.uiState.value.canEditNotes)
            assertEquals("Edited draft", viewModel.uiState.value.notes)
            coEvery { repository.saveProjectNotes(1L, "", "Edited draft", false) } returns
                ProjectNotesSaveResult.Saved(
                    CounterProject(id = 1L, name = "Test", notes = "Edited draft", notesCreated = true),
                )
            viewModel.saveImmediately()
            advanceUntilIdle()
            coVerify(exactly = 1) { repository.saveProjectNotes(1L, "", "Edited draft", false) }
            assertNull(handle.get<String>("notesDraft"))
        }

    @Test
    fun `non-pro editor can edit previously created notes`() =
        runTest {
            every { proManager.hasFeature(ProFeature.NOTES) } returns false
            val viewModel = vm(notes = "Base")
            advanceUntilIdle()

            viewModel.onNotesChanged("Local edit")
            viewModel.saveImmediately()
            advanceUntilIdle()

            assertEquals("Local edit", viewModel.uiState.value.notes)
            coVerify(exactly = 1) { repository.saveProjectNotes(1L, "Base", "Local edit", false) }
        }

    @Test
    fun `non-pro editor cannot create first notes without authorization`() =
        runTest {
            every { proManager.hasFeature(ProFeature.NOTES) } returns false
            val viewModel = vm(notesCreated = false)
            advanceUntilIdle()

            viewModel.onNotesChanged("First notes")
            viewModel.saveImmediately()
            advanceUntilIdle()

            assertEquals("", viewModel.uiState.value.notes)
            assertFalse(viewModel.uiState.value.canEditNotes)
            coVerify(exactly = 0) { repository.saveProjectNotes(any(), any(), any(), any()) }
        }

    @Test
    fun `production trial access allows first notes without a reader entitlement gate`() =
        runTest {
            val entitlement = ProState(status = ProStatus.TRIAL_ACTIVE)
            every { proManager.hasFeature(ProFeature.NOTES) } returns
                entitlement.hasFeature(ProFeature.NOTES, debugUnlockAllFeatures = false)
            val viewModel = vm()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.canEditNotes)
            viewModel.onNotesChanged("First project notes")
            viewModel.saveImmediately()
            advanceUntilIdle()

            coVerify(exactly = 1) { repository.saveProjectNotes(1L, "", "First project notes", false) }
        }

    @Test
    fun `expired access preserves a previously used but now empty notes surface`() =
        runTest {
            val entitlement = ProState(status = ProStatus.TRIAL_EXPIRED)
            every { proManager.hasFeature(ProFeature.NOTES) } returns
                entitlement.hasFeature(ProFeature.NOTES, debugUnlockAllFeatures = false)
            val viewModel = vm(notes = "", notesCreated = true)
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.canEditNotes)
            assertTrue(viewModel.uiState.value.notesCreated)
            viewModel.onNotesChanged("New text in existing notes")
            viewModel.saveImmediately()
            advanceUntilIdle()

            coVerify(exactly = 1) { repository.saveProjectNotes(1L, "", "New text in existing notes", false) }
        }

    @Test
    fun `contextual authorization and cancellation stay with the initiating project`() =
        runTest {
            every { proManager.hasFeature(ProFeature.NOTES) } returns false
            val first = vm(notesCreated = false)
            every { repository.observeProject(2L) } returns
                flowOf(CounterProject(id = 2L, name = "Other project"))
            val other = createEditor(SavedStateHandle(mapOf("projectId" to 2L)))
            advanceUntilIdle()

            first.cancelFirstNotesCreation()
            assertFalse(first.uiState.value.canEditNotes)
            first.authorizeFirstNotesCreation()
            first.cancelFirstNotesCreation()
            assertFalse(first.uiState.value.canEditNotes)
            first.authorizeFirstNotesCreation()
            first.authorizeFirstNotesCreation()
            first.onNotesChanged("Authorized first notes")
            first.saveImmediately()
            advanceUntilIdle()

            assertFalse(other.uiState.value.canEditNotes)
            coVerify(exactly = 1) { repository.saveProjectNotes(1L, "", "Authorized first notes", true) }
            coVerify(exactly = 0) { repository.saveProjectNotes(2L, any(), any(), any()) }
        }
}
