package com.finnvek.knittools.ui.screens.pattern

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.finnvek.knittools.R
import com.finnvek.knittools.data.storage.PatternDocumentStorage
import com.finnvek.knittools.data.storage.PatternImageInfo
import com.finnvek.knittools.data.storage.PatternImageStageBatch
import com.finnvek.knittools.data.storage.PatternImageStageException
import com.finnvek.knittools.data.storage.PatternImageStageFailure
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.repository.ProjectDocumentMutationResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class PatternImageImportOwnershipTest {
    private val dispatcher = StandardTestDispatcher()
    private val applicationScope = CoroutineScope(dispatcher)
    private val proManager = mockk<ProManager>(relaxed = true)
    private val models = ViewModelStore()
    private val handles = mutableMapOf<Long, SavedStateHandle>()
    private val storage = mockk<PatternDocumentStorage>(relaxed = true)
    private val repository = mockk<CounterRepository>(relaxed = true)
    private val context = mockk<Context>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { proManager.initialStateReady } returns MutableStateFlow(false)
        every { storage.hasCreationSpace(any(), any()) } returns true
        every { storage.restoreStagedSelection(any(), any(), any(), any()) } answers { arg(3) }
        coEvery { storage.stageSelectedImages(any(), any(), any(), any(), any()) } coAnswers {
            val owner = arg<Long>(1)
            val uris = arg<List<Uri>>(4)
            PatternImageStageBatch(uris.map { page(owner, it.toString()) }, 0)
        }
        coEvery { storage.convertImagesToPdf(any(), any(), any(), any(), any()) } coAnswers {
            output(arg(1))
        }
        coEvery { repository.getProject(any()) } returns null
        every { context.getString(R.string.pattern_images_document_fallback) } returns "Pattern photos"
        coEvery { repository.attachPattern(any(), any(), any(), any(), any(), true) } returns
            ProjectDocumentMutationResult.AlreadyAttached
    }

    @After
    fun tearDown() {
        applicationScope.cancel()
        models.clear()
        Dispatchers.resetMain()
    }

    @Test
    fun `page moves preserve order persistence and reject stale or busy edits`() =
        runTest {
            val vm = model(7)
            import(vm, 7, "first", "second")
            advanceUntilIdle()
            val initial = vm.uiState.value
            vm.moveEarlier("second", "obsolete-session")
            vm.moveLater("first", "obsolete-session")
            vm.moveEarlier("missing")
            vm.moveLater("missing")
            vm.moveEarlier("first")
            vm.moveLater("second")
            assertEquals(initial, vm.uiState.value)

            vm.moveLater("first")
            val reordered = vm.uiState.value.selection
            assertEquals(listOf("second", "first"), reordered.pages.map { it.id })
            val restored = newModel(copyHandle(handles.getValue(7)))
            advanceUntilIdle()
            assertEquals(reordered, restored.uiState.value.selection)
            vm.moveEarlier("first")
            assertEquals(initial.selection, vm.uiState.value.selection)

            val staged = CompletableDeferred<PatternImageStageBatch>()
            coEvery { storage.stageSelectedImages(any(), any(), any(), any(), any()) } coAnswers { staged.await() }
            import(vm, 7, "third")
            runCurrent()
            val busy = vm.uiState.value
            assertTrue(busy.isBusy)
            vm.moveEarlier("second")
            vm.moveLater("first")
            assertEquals(busy, vm.uiState.value)
            staged.complete(PatternImageStageBatch(emptyList(), 0))
            advanceUntilIdle()
        }

    @Test
    fun `one route stores A and B separately and returning to A preserves its ordered draft`() =
        runTest {
            val a = model(7)
            import(a, 7, "first", "second", "first")
            advanceUntilIdle()
            a.moveEarlier("second")
            val draftA = a.uiState.value

            val b = model(8)
            assertTrue(
                b.uiState.value.selection.pages
                    .isEmpty(),
            )
            import(b, 8, "b")
            advanceUntilIdle()
            b.createPatternPdf()
            advanceUntilIdle()

            assertSame(a, model(7))
            assertEquals(draftA, model(7).uiState.value)
            assertEquals(listOf("second", "first"), draftA.selection.pages.map { it.sourceUri })
            assertEquals(1, draftA.duplicatesIgnored)
            coVerify(exactly = 1) { repository.attachPattern(8, output(8).first, any(), 0, null, true) }
            coVerify(exactly = 0) { repository.attachPattern(7, any(), any(), any(), any(), true) }
            verify(exactly = 0) { storage.deleteImportSession(any(), 7, any()) }
        }

    @Test
    fun `same owner pending launch restores once and empty result stays consumed`() =
        runTest {
            val a = model(7)
            import(a, 7, "first")
            advanceUntilIdle()
            val request = requireNotNull(a.authorizeGalleryPicker(7))
            assertTrue(a.markGalleryPickerLaunched(request))
            val saved = copyHandle(handles.getValue(7))
            val restored = newModel(saved)

            assertFalse(restored.markGalleryPickerLaunched(request))
            restored.onGalleryPickerResult(request, emptyList())
            assertEquals(null, restored.uiState.value.pickerRequestId)
            restored.onGalleryPickerResult(request, listOf(uri("late")))
            val restoredAgain = newModel(copyHandle(saved))
            restoredAgain.onGalleryPickerResult(request, listOf(uri("late-again")))
            advanceUntilIdle()

            assertEquals(
                listOf("first"),
                restored.uiState.value.selection.pages
                    .map { it.sourceUri },
            )
            assertEquals(null, restoredAgain.uiState.value.pickerRequestId)
            coVerify(exactly = 1) { storage.stageSelectedImages(any(), any(), any(), any(), any()) }
        }

    @Test
    fun `pending gallery result survives an unrestorable staging directory`() =
        runTest {
            val a = model(7)
            import(a, 7, "first")
            advanceUntilIdle()
            val oldSession = a.uiState.value.sessionId
            val request = requireNotNull(a.authorizeGalleryPicker(7))
            assertTrue(a.markGalleryPickerLaunched(request))
            every { storage.restoreStagedSelection(any(), any(), any(), any()) } returns null
            val restored = newModel(copyHandle(handles.getValue(7)))

            restored.onGalleryPickerResult(request, listOf(uri("new")))
            advanceUntilIdle()

            val restoredState = restored.uiState.value
            assertNotEquals(oldSession, restoredState.sessionId)
            assertEquals(null, restoredState.pickerRequestId)
            assertEquals(listOf("new"), restoredState.selection.pages.map { it.sourceUri })
        }

    @Test
    fun `unlaunched saved request can launch once and obsolete launch failure preserves newer request`() =
        runTest {
            val a = model(7)
            val old = requireNotNull(a.authorizeGalleryPicker(7))
            val restored = newModel(copyHandle(handles.getValue(7)))
            assertTrue(restored.markGalleryPickerLaunched(old))
            assertFalse(restored.markGalleryPickerLaunched(old))
            val next = requireNotNull(restored.authorizeGalleryPicker(7))
            val state = restored.uiState.value
            restored.onGalleryPickerLaunchFailed(old)
            restored.onGalleryPickerResult(old, listOf(uri("old")))
            assertEquals(state, restored.uiState.value)
            restored.onGalleryPickerLaunchFailed(next)
            assertEquals(PatternImageImportError.UNREADABLE, restored.uiState.value.error)
            assertEquals(null, restored.uiState.value.pickerRequestId)
            coVerify(exactly = 0) { storage.stageSelectedImages(any(), any(), any(), any(), any()) }
        }

    @Test
    fun `delayed staging keeps initiating owner while B is edited and A is revisited`() =
        runTest {
            val staged = CompletableDeferred<PatternImageStageBatch>()
            coEvery { storage.stageSelectedImages(any(), 7, any(), any(), any()) } coAnswers { staged.await() }
            val a = model(7)
            import(a, 7, "a")
            runCurrent()
            val sessionA = a.uiState.value.sessionId
            val b = model(8)
            import(b, 8, "b")
            runCurrent()
            val draftB = b.uiState.value
            assertSame(a, model(7))
            assertEquals(PatternImageImportPhase.STAGING, a.uiState.value.phase)
            assertEquals(null, a.authorizeGalleryPicker(7))
            staged.complete(PatternImageStageBatch(listOf(page(7, "a")), 0))
            advanceUntilIdle()

            assertEquals(sessionA, a.uiState.value.sessionId)
            assertEquals(
                listOf("a"),
                a.uiState.value.selection.pages
                    .map { it.sourceUri },
            )
            assertEquals(draftB, b.uiState.value)
            assertEquals(8L, handles.getValue(8).get<Long>("pattern_image_project_id"))
        }

    @Test
    fun `delayed stage failure remains recoverable on A without changing B`() =
        runTest {
            val stage = CompletableDeferred<Unit>()
            coEvery { storage.stageSelectedImages(any(), 7, any(), any(), any()) } coAnswers {
                stage.await()
                throw PatternImageStageException(PatternImageStageFailure.UNREADABLE)
            }
            val a = model(7)
            import(a, 7, "broken")
            runCurrent()
            val b = model(8)
            import(b, 8, "b")
            runCurrent()
            val draftB = b.uiState.value
            stage.complete(Unit)
            advanceUntilIdle()

            assertEquals(PatternImageImportError.UNREADABLE, model(7).uiState.value.error)
            assertEquals(draftB, model(8).uiState.value)
            assertFalse(b.uiState.value.closeReady)
        }

    @Test
    fun `delayed conversion attaches to A and old progress cannot change fresh A or B`() =
        runTest {
            val conversion = CompletableDeferred<Pair<String, String>>()
            lateinit var oldProgress: (Int, Int) -> Unit
            coEvery { storage.convertImagesToPdf(any(), 7, any(), any(), any()) } coAnswers {
                oldProgress = arg(4)
                conversion.await()
            }
            val a = model(7)
            import(a, 7, "a")
            advanceUntilIdle()
            val oldSession = a.uiState.value.sessionId
            a.createPatternPdf()
            runCurrent()
            val b = model(8)
            import(b, 8, "b")
            runCurrent()
            val draftB = b.uiState.value
            oldProgress(1, 2)
            assertEquals(PatternImageProgress(1, 2), model(7).uiState.value.progress)
            assertEquals(draftB, b.uiState.value)
            conversion.complete(output(7))
            advanceUntilIdle()
            assertTrue(a.consumeCloseRequest(oldSession))
            assertFalse(a.consumeCloseRequest(oldSession))
            import(a, 7, "new-a")
            advanceUntilIdle()
            val newA = a.uiState.value
            oldProgress(2, 2)
            a.cancelImport(oldSession)
            a.createPatternPdf(oldSession)
            a.removePage("new-a", oldSession)
            a.markPreviewFailed("new-a", oldSession)

            assertFalse(a.consumeCloseRequest(oldSession))
            assertNotEquals(oldSession, newA.sessionId)
            assertEquals(newA, a.uiState.value)
            assertEquals(draftB, b.uiState.value)
            coVerify(exactly = 1) { repository.attachPattern(7, output(7).first, any(), 0, null, true) }
            coVerify(exactly = 0) { repository.attachPattern(8, any(), any(), any(), any(), true) }
        }

    @Test
    fun `failed conversion progress cannot overwrite a deliberate retry in the same session`() =
        runTest {
            val retry = CompletableDeferred<Pair<String, String>>()
            val callbacks = mutableListOf<(Int, Int) -> Unit>()
            coEvery { storage.convertImagesToPdf(any(), 7, any(), any(), any()) } coAnswers {
                callbacks += arg<(Int, Int) -> Unit>(4)
                if (callbacks.size == 1) throw IOException("conversion failed")
                retry.await()
            }
            val a = model(7)
            import(a, 7, "a")
            advanceUntilIdle()
            a.createPatternPdf()
            advanceUntilIdle()
            assertEquals(PatternImageImportError.CONVERSION, a.uiState.value.error)
            val session = a.uiState.value.sessionId
            a.createPatternPdf()
            runCurrent()
            callbacks.first()(9, 9)
            assertEquals(null, a.uiState.value.progress)
            callbacks.last()(1, 2)
            assertEquals(PatternImageProgress(1, 2), a.uiState.value.progress)
            assertEquals(session, a.uiState.value.sessionId)
            retry.complete(output(7))
            advanceUntilIdle()
            assertEquals(PatternImageImportPhase.SUCCESS, a.uiState.value.phase)
        }

    @Test
    fun `cancelled delayed stage and cleanup cannot erase B or admit a new A too early`() =
        runTest {
            val stage = CompletableDeferred<PatternImageStageBatch>()
            coEvery { storage.stageSelectedImages(any(), 7, any(), any(), any()) } coAnswers {
                withContext(NonCancellable) { stage.await() }
            }
            val a = model(7)
            val oldRequest = requireNotNull(a.authorizeGalleryPicker(7))
            a.onGalleryPickerResult(oldRequest, listOf(uri("old")))
            runCurrent()
            val oldSession = a.uiState.value.sessionId
            val b = model(8)
            import(b, 8, "b")
            runCurrent()
            val draftB = b.uiState.value
            a.cancelImport(oldSession)
            a.cancelImport(oldSession)
            assertEquals(null, a.authorizeGalleryPicker(7))
            every { storage.deleteImportSession(context, 7, requireNotNull(oldSession)) } answers {
                assertEquals(null, a.authorizeGalleryPicker(7))
                assertEquals(draftB, b.uiState.value)
            }
            stage.complete(PatternImageStageBatch(listOf(page(7, "old")), 0))
            advanceUntilIdle()
            assertEquals(PatternImageImportPhase.CANCELLED, a.uiState.value.phase)
            assertTrue(
                a.uiState.value.selection.pages
                    .isEmpty(),
            )
            val nextRequest = requireNotNull(a.authorizeGalleryPicker(7))
            val nextState = a.uiState.value
            a.onGalleryPickerResult(oldRequest, listOf(uri("late")))
            assertEquals(nextRequest, a.uiState.value.pickerRequestId)
            a.onGalleryPickerResult(nextRequest, emptyList())
            assertEquals(null, a.uiState.value.pickerRequestId)
            assertEquals(nextState.sessionId, a.uiState.value.sessionId)
            assertEquals(draftB, b.uiState.value)
            verify(exactly = 1) { storage.deleteImportSession(context, 7, requireNotNull(oldSession)) }
            coVerify(exactly = 0) { repository.attachPattern(any(), any(), any(), any(), any(), true) }
        }

    @Test
    fun `cancelled conversion handoff discards the unattached PDF`() =
        runTest {
            coEvery { storage.convertImagesToPdf(any(), 7, any(), any(), any()) } coAnswers {
                currentCoroutineContext().cancel()
                output(7)
            }
            val vm = newModel(SavedStateHandle(), StandardTestDispatcher(dispatcher.scheduler, "image IO"))
            import(vm, 7, "a")
            advanceUntilIdle()

            vm.createPatternPdf()
            advanceUntilIdle()

            assertEquals(PatternImageImportPhase.CANCELLED, vm.uiState.value.phase)
            verify(exactly = 1) { storage.deleteGeneratedPdf(context, output(7).first) }
            coVerify(exactly = 0) { repository.attachPattern(any(), any(), any(), any(), any(), true) }
        }

    @Test
    fun `cancelled delayed attachment with unknown readback preserves drafts and output`() =
        runTest {
            val attachment = CompletableDeferred<ProjectDocumentMutationResult>()
            coEvery { repository.attachPattern(7, any(), any(), any(), any(), true) } coAnswers { attachment.await() }
            coEvery { repository.isPatternDocumentAttached(7, output(7).first) } throws
                IOException("readback unavailable")
            val a = model(7)
            import(a, 7, "a")
            advanceUntilIdle()
            a.createPatternPdf()
            runCurrent()
            assertEquals(PatternImageImportPhase.ATTACHING, a.uiState.value.phase)
            val b = model(8)
            import(b, 8, "b")
            runCurrent()
            val draftB = b.uiState.value
            a.cancelImport()
            advanceUntilIdle()

            assertEquals(PatternImageImportError.ATTACHMENT, a.uiState.value.error)
            assertEquals(
                listOf("a"),
                a.uiState.value.selection.pages
                    .map { it.sourceUri },
            )
            assertEquals(draftB, b.uiState.value)
            verify(exactly = 0) { storage.deleteGeneratedPdf(any(), any()) }
            verify(exactly = 0) { storage.deleteImportSession(any(), 7, any()) }
        }

    @Test
    fun `late attachment success clears only its initiating session and leaves B open`() =
        runTest {
            val attachment = CompletableDeferred<ProjectDocumentMutationResult>()
            coEvery { repository.attachPattern(7, any(), any(), any(), any(), true) } coAnswers { attachment.await() }
            val a = model(7)
            import(a, 7, "a")
            advanceUntilIdle()
            val sessionA = a.uiState.value.sessionId
            a.createPatternPdf()
            runCurrent()
            val b = model(8)
            import(b, 8, "b")
            runCurrent()
            val draftB = b.uiState.value
            attachment.complete(ProjectDocumentMutationResult.AlreadyAttached)
            advanceUntilIdle()

            assertEquals(PatternImageImportPhase.SUCCESS, model(7).uiState.value.phase)
            assertEquals(sessionA, a.uiState.value.sessionId)
            assertEquals(draftB, b.uiState.value)
            assertEquals(null, handles.getValue(7).get<String>("pattern_image_session_id"))
            assertEquals(draftB.sessionId, handles.getValue(8).get<String>("pattern_image_session_id"))
            verify(exactly = 1) { storage.deleteImportSession(context, 7, requireNotNull(sessionA)) }
            verify(exactly = 0) { storage.deleteImportSession(any(), 8, any()) }
        }

    @Test
    fun `attachment cancellation after confirmed commit completes without deleting output`() =
        runTest {
            coEvery { repository.attachPattern(7, any(), any(), any(), any(), true) } throws
                CancellationException("late cancellation")
            coEvery { repository.isPatternDocumentAttached(7, output(7).first) } returns true
            val a = model(7)
            import(a, 7, "a")
            advanceUntilIdle()
            a.createPatternPdf()
            advanceUntilIdle()
            assertEquals(PatternImageImportPhase.SUCCESS, a.uiState.value.phase)
            verify(exactly = 0) { storage.deleteGeneratedPdf(any(), any()) }
        }

    @Test
    fun `camera request restores and is consumed once while old capture cleanup preserves newer session`() =
        runTest {
            val a = model(7)
            val oldSession = a.authorizeCameraCapture(7)
            val oldFile = File("build/test-pattern-import/old-camera.jpg")
            a.discardCameraCapture(uri("old-camera"), oldFile, oldSession)
            advanceUntilIdle()
            val nextSession = a.authorizeCameraCapture(7)
            assertNotEquals(oldSession, nextSession)
            val restored = newModel(copyHandle(handles.getValue(7)))
            val newFile = File("build/test-pattern-import/new-camera.jpg")
            every { storage.inspectCameraCapture(newFile) } returns PatternImageInfo(100, 200)
            restored.discardCameraCapture(uri("old-camera"), oldFile, oldSession)
            restored.acceptCameraCapture(7, uri("old-camera"), oldFile, oldSession)
            assertTrue(restored.uiState.value.cameraCapturePending)
            restored.acceptCameraCapture(7, uri("new-camera"), newFile, nextSession)
            restored.acceptCameraCapture(7, uri("new-camera"), newFile, nextSession)
            advanceUntilIdle()
            assertEquals(nextSession, restored.uiState.value.sessionId)
            assertEquals(
                listOf("new-camera"),
                restored.uiState.value.selection.pages
                    .map { it.sourceUri },
            )
            verify(exactly = 1) { storage.inspectCameraCapture(newFile) }
            verify(exactly = 0) { storage.inspectCameraCapture(oldFile) }
            verify(exactly = 0) { storage.deleteStagedPage(match { it.stagedPath == newFile.absolutePath }) }
        }

    @Test
    fun `pending gallery result blocks creation until it is consumed`() =
        runTest {
            val a = model(7)
            import(a, 7, "a")
            advanceUntilIdle()
            val request = a.authorizeGalleryPicker(7)
            a.createPatternPdf()
            advanceUntilIdle()
            coVerify(exactly = 0) { storage.convertImagesToPdf(any(), any(), any(), any(), any()) }
            a.onGalleryPickerResult(request, emptyList())
            a.createPatternPdf()
            advanceUntilIdle()
            coVerify(exactly = 1) { storage.convertImagesToPdf(any(), 7, any(), any(), any()) }
        }

    @Test
    fun `missing camera caller state requires explicit cancel and never retargets its session`() =
        runTest {
            val a = model(7)
            val session = a.authorizeCameraCapture(7)
            val restored = newModel(copyHandle(handles.getValue(7)))
            restored.onCameraCaptureStateLost(session)
            assertEquals(session, restored.uiState.value.sessionId)
            assertEquals(PatternImageImportError.UNREADABLE, restored.uiState.value.error)
            assertEquals(null, restored.authorizeGalleryPicker(7))
            verify(exactly = 0) { storage.deleteImportSession(any(), any(), any()) }

            restored.cancelImport(session)
            advanceUntilIdle()
            val next = restored.authorizeGalleryPicker(7)
            val nextState = restored.uiState.value
            restored.onCameraCaptureStateLost(session)
            assertTrue(next != null)
            assertNotEquals(session, nextState.sessionId)
            assertEquals(nextState, restored.uiState.value)
        }

    private fun model(owner: Long): PatternImageImportViewModel {
        val key = patternImageImportViewModelKey(owner)
        return models[key] as? PatternImageImportViewModel
            ?: newModel(handles.getOrPut(owner) { SavedStateHandle() }).also {
                models.put(key, it)
            }
    }

    private fun copyHandle(handle: SavedStateHandle): SavedStateHandle =
        SavedStateHandle(handle.keys().associateWith { handle.get<Any?>(it) })

    private fun newModel(
        handle: SavedStateHandle,
        ioDispatcher: CoroutineDispatcher = dispatcher,
    ) = PatternImageImportViewModel(
        storage,
        repository,
        context,
        ioDispatcher,
        handle,
        proManager,
        mockk(relaxed = true),
        applicationScope,
    )

    private fun import(
        vm: PatternImageImportViewModel,
        owner: Long,
        vararg sources: String,
    ) {
        val request = requireNotNull(vm.authorizeGalleryPicker(owner))
        vm.onGalleryPickerResult(request, sources.map(::uri))
    }

    private fun page(
        owner: Long,
        source: String,
    ) = StagedPatternPage(
        id = source,
        sourceUri = source,
        stagedPath = "build/test-pattern-import/$owner/$source.img",
        byteCount = 10,
        width = 100,
        height = 200,
    )

    private fun output(owner: Long) = "file:///pattern_pdfs/$owner/generated.pdf" to "generated.pdf"

    private fun uri(value: String): Uri = mockk<Uri>().also { uri -> every { uri.toString() } returns value }
}
