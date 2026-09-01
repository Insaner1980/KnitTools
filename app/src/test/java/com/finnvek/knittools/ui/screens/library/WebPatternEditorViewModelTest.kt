package com.finnvek.knittools.ui.screens.library

import androidx.lifecycle.SavedStateHandle
import com.finnvek.knittools.domain.model.SavedPattern
import com.finnvek.knittools.domain.model.SavedPatternSource
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.repository.SavedPatternMetadataMutationResult
import com.finnvek.knittools.repository.SavedPatternRepository
import com.finnvek.knittools.repository.WebPatternMutationResult
import com.finnvek.knittools.ui.navigation.PatternShareImportRequest
import com.finnvek.knittools.ui.navigation.PatternSharePayload
import com.finnvek.knittools.ui.navigation.Screen
import com.finnvek.knittools.ui.navigation.WebPatternEditorOrigin
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WebPatternEditorViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val patterns = mockk<SavedPatternRepository>()
    private val projects = mockk<CounterRepository>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `raw invalid draft survives recreation without persistence`() =
        runTest {
            val handle = routeHandle(WebPatternEditorOrigin.Manual)
            val model = model(handle)

            model.updateTitle("  Draft title  ")
            model.updateDesigner("Optional designer")
            model.updateUrl("https://example.com/bad url")

            val restored = model(handle.restoredCopy())

            assertEquals("  Draft title  ", restored.uiState.value.title)
            assertEquals("Optional designer", restored.uiState.value.designer)
            assertEquals("https://example.com/bad url", restored.uiState.value.url)
            assertFalse(restored.uiState.value.canSave)
            coVerify(exactly = 0) { patterns.createWebPattern(any()) }
        }

    // CPD-OFF: Jaon ja korvauksen skenaariokohtainen asetelma pidetaan testien yhteydessa.
    @Test
    fun `incoming share preserves the current draft until the user chooses`() =
        runTest {
            val handle = routeHandle(WebPatternEditorOrigin.Manual)
            val model = model(handle)
            model.updateTitle("Current draft")
            model.updateUrl("https://example.com/current")
            val request =
                PatternShareImportRequest(
                    requestId = 12L,
                    payload = PatternSharePayload.WebLink("https://example.com/incoming", "Incoming"),
                )

            assertEquals(WebPatternShareAcceptResult.Stored, model.offerSharedRequest(request))
            assertEquals("Current draft", model.uiState.value.title)
            assertEquals("https://example.com/current", model.uiState.value.url)
            assertNotNull(model.uiState.value.pendingIncomingShare)

            val restored = model(handle.restoredCopy())
            restored.resolveIncomingShare(useIncoming = true)

            assertEquals("Incoming", restored.uiState.value.title)
            assertEquals("https://example.com/incoming", restored.uiState.value.url)
            assertNull(restored.uiState.value.pendingIncomingShare)
            assertEquals(WebPatternShareAcceptResult.Stored, restored.offerSharedRequest(request))
            assertNull(restored.uiState.value.pendingIncomingShare)
            coVerify(exactly = 0) { patterns.createWebPattern(any()) }
            coVerify(exactly = 0) { patterns.updateWebPattern(any(), any(), any()) }
        }

    @Test
    fun `keeping current draft consumes shared request without mutation or replay`() =
        runTest {
            val model = model(routeHandle(WebPatternEditorOrigin.Manual))
            model.updateTitle("Current draft")
            model.updateUrl("https://example.com/current")
            val request =
                PatternShareImportRequest(
                    requestId = 13L,
                    payload = PatternSharePayload.WebLink("https://example.com/incoming", "Incoming"),
                )
            model.offerSharedRequest(request)

            model.resolveIncomingShare(useIncoming = false)

            assertEquals("Current draft", model.uiState.value.title)
            assertEquals("https://example.com/current", model.uiState.value.url)
            assertNull(model.uiState.value.pendingIncomingShare)
            assertEquals(WebPatternShareAcceptResult.Stored, model.offerSharedRequest(request))
            assertNull(model.uiState.value.pendingIncomingShare)
            coVerify(exactly = 0) { patterns.createWebPattern(any()) }
            coVerify(exactly = 0) { patterns.updateWebPattern(any(), any(), any()) }
        }

    @Test
    fun `manual save emits one completion only after persisted create`() =
        runTest {
            coEvery { patterns.createWebPattern(any()) } returns WebPatternMutationResult.Created(9L)
            val handle = routeHandle(WebPatternEditorOrigin.Manual)
            val model = model(handle)
            model.updateTitle("Cardigan")
            model.updateUrl("https://example.com/cardigan")

            model.save()
            assertTrue(model.uiState.value.isSaving)
            assertNull(model.uiState.value.completion)
            advanceUntilIdle()

            val completion = model.uiState.value.completion as WebPatternEditorCompletion.OpenDetail
            assertEquals(9L, completion.patternId)
            model.consumeCompletion(completion.eventId)
            assertNull(model.uiState.value.completion)

            val restored = model(handle.restoredCopy())
            assertNull(restored.uiState.value.completion)
            restored.save()
            advanceUntilIdle()
            coVerify(exactly = 1) { patterns.createWebPattern(any()) }
        }

    @Test
    fun `create failure retains the complete draft for retry`() =
        runTest {
            coEvery { patterns.createWebPattern(any()) } returns WebPatternMutationResult.PersistenceFailure
            val model = model(routeHandle(WebPatternEditorOrigin.Manual))
            model.updateTitle("Retry cardigan")
            model.updateDesigner("Pattern designer")
            model.updateUrl("https://example.com/retry")

            model.save()
            advanceUntilIdle()

            assertEquals(WebPatternEditorError.SaveFailed, model.uiState.value.error)
            assertEquals("Retry cardigan", model.uiState.value.title)
            assertEquals("Pattern designer", model.uiState.value.designer)
            assertEquals("https://example.com/retry", model.uiState.value.url)
            assertFalse(model.uiState.value.isSaving)
            assertNull(model.uiState.value.completion)
            coVerify(exactly = 1) { patterns.createWebPattern(any()) }
        }

    @Test
    fun `missing project leaves created pattern in Library and never retargets attachment`() =
        runTest {
            coEvery { patterns.createWebPattern(any()) } returns WebPatternMutationResult.Created(15L)
            coEvery { projects.attachSavedPatternMetadata(42L, 15L) } returns
                SavedPatternMetadataMutationResult.ProjectMissing
            val model = model(routeHandle(WebPatternEditorOrigin.Project, projectId = 42L))
            model.updateTitle("Cardigan")
            model.updateUrl("https://example.com/cardigan")

            model.save()
            advanceUntilIdle()

            assertEquals(WebPatternEditorError.ProjectUnavailable, model.uiState.value.error)
            assertNull(model.uiState.value.completion)
            coVerify(exactly = 1) { projects.attachSavedPatternMetadata(42L, 15L) }
            coVerify(exactly = 0) { projects.attachSavedPatternMetadata(match { it != 42L }, any()) }
        }

    @Test
    fun `edit load failure stops loading and preserves draft typed while loading`() =
        runTest {
            coEvery { patterns.getById(9L) } throws IllegalStateException("read failed")
            val model = model(routeHandle(WebPatternEditorOrigin.Edit, patternId = 9L))
            model.updateTitle("Recovered draft")
            model.updateUrl("https://example.com/recovered")

            advanceUntilIdle()

            assertFalse(model.uiState.value.isLoading)
            assertEquals("Recovered draft", model.uiState.value.title)
            assertEquals("https://example.com/recovered", model.uiState.value.url)
            assertEquals(WebPatternEditorError.PatternUnavailable, model.uiState.value.error)
        }

    @Test
    fun `project attach requires matching replacement confirmation`() =
        runTest {
            coEvery { patterns.createWebPattern(any()) } returns WebPatternMutationResult.Created(15L)
            coEvery { projects.attachSavedPatternMetadata(42L, 15L, null) } returns
                SavedPatternMetadataMutationResult.ReplacementRequired(88L)
            coEvery { projects.attachSavedPatternMetadata(42L, 15L, 88L) } returns
                SavedPatternMetadataMutationResult.Attached(15L)
            val handle = routeHandle(WebPatternEditorOrigin.Project, projectId = 42L)
            val model = model(handle)
            model.updateTitle("Replacement")
            model.updateUrl("https://example.com/replacement")

            model.save()
            advanceUntilIdle()

            assertEquals(
                PendingWebPatternReplacement(
                    projectId = 42L,
                    savedPatternId = 15L,
                    expectedExistingSavedPatternId = 88L,
                ),
                model.uiState.value.pendingReplacement,
            )
            assertTrue(model.uiState.value.didPersist)
            assertNull(model.uiState.value.completion)

            val restored = model(handle.restoredCopy())
            restored.confirmReplacement()
            advanceUntilIdle()

            assertEquals(42L, (restored.uiState.value.completion as WebPatternEditorCompletion.OpenProject).projectId)
            coVerify(exactly = 1) { projects.attachSavedPatternMetadata(42L, 15L, null) }
            coVerify(exactly = 1) { projects.attachSavedPatternMetadata(42L, 15L, 88L) }
        }

    @Test
    fun `stale replacement confirmation closes old dialog`() =
        runTest {
            coEvery { patterns.createWebPattern(any()) } returns WebPatternMutationResult.Created(15L)
            coEvery { projects.attachSavedPatternMetadata(42L, 15L, null) } returns
                SavedPatternMetadataMutationResult.ReplacementRequired(88L)
            coEvery { projects.attachSavedPatternMetadata(42L, 15L, 88L) } returns
                SavedPatternMetadataMutationResult.StaleAction
            val model = model(routeHandle(WebPatternEditorOrigin.Project, projectId = 42L))
            model.updateTitle("Replacement")
            model.updateUrl("https://example.com/replacement")
            model.save()
            advanceUntilIdle()

            model.confirmReplacement()
            advanceUntilIdle()

            assertNull(model.uiState.value.pendingReplacement)
            assertEquals(WebPatternEditorError.StaleAction, model.uiState.value.error)
        }

    // CPD-ON

    @Test
    fun `edit duplicate rejects collision and preserves draft`() =
        runTest {
            coEvery { patterns.getById(9L) } returns webPattern(9L)
            coEvery { patterns.updateWebPattern(9L, 5L, any()) } returns WebPatternMutationResult.Duplicate(10L)
            val model = model(routeHandle(WebPatternEditorOrigin.Edit, patternId = 9L))
            advanceUntilIdle()
            model.updateTitle("Edited title")

            model.save()
            advanceUntilIdle()

            assertEquals(WebPatternEditorError.AlreadySaved, model.uiState.value.error)
            assertEquals("Edited title", model.uiState.value.title)
            assertNull(model.uiState.value.completion)
        }

    @Test
    fun `edit success loads existing values and emits detail completion`() =
        runTest {
            coEvery { patterns.getById(9L) } returns webPattern(9L)
            coEvery { patterns.updateWebPattern(9L, 5L, any()) } returns WebPatternMutationResult.Updated(9L)
            val model = model(routeHandle(WebPatternEditorOrigin.Edit, patternId = 9L))
            advanceUntilIdle()

            assertEquals("Pattern", model.uiState.value.title)
            assertEquals("Designer", model.uiState.value.designer)
            assertEquals("https://example.com/pattern", model.uiState.value.url)
            model.updateTitle("Edited pattern")
            model.updateDesigner("Edited designer")
            model.updateUrl("https://example.com/edited")

            model.save()
            advanceUntilIdle()

            val completion = model.uiState.value.completion as WebPatternEditorCompletion.OpenDetail
            assertEquals(9L, completion.patternId)
            coVerify(exactly = 1) {
                patterns.updateWebPattern(
                    9L,
                    5L,
                    match {
                        it.title == "Edited pattern" &&
                            it.designer == "Edited designer" &&
                            it.url == "https://example.com/edited"
                    },
                )
            }
        }

    @Test
    fun `Ravelry handoff uses validated trimmed original url`() =
        runTest {
            coEvery { patterns.createWebPattern(any()) } returns WebPatternMutationResult.RavelryOwnedUrl
            val model = model(routeHandle(WebPatternEditorOrigin.Manual))
            model.updateTitle("Ravelry pattern")
            model.updateUrl("  https://www.ravelry.com/patterns/library/test-pattern  ")

            model.save()
            advanceUntilIdle()

            assertEquals(
                "https://www.ravelry.com/patterns/library/test-pattern",
                (model.uiState.value.completion as WebPatternEditorCompletion.OpenRavelry).url,
            )
        }

    private fun model(handle: SavedStateHandle) = WebPatternEditorViewModel(patterns, projects, handle)

    private fun routeHandle(
        origin: WebPatternEditorOrigin,
        projectId: Long? = null,
        patternId: Long? = null,
    ) = SavedStateHandle(
        buildMap<String, Any?> {
            put(Screen.WebPatternEditor.ARG_ORIGIN, origin.persistedValue)
            projectId?.let { put(Screen.WebPatternEditor.ARG_PROJECT_ID, it) }
            patternId?.let { put(Screen.WebPatternEditor.ARG_PATTERN_ID, it) }
        },
    )

    private fun SavedStateHandle.restoredCopy(): SavedStateHandle =
        SavedStateHandle(keys().associateWith { key -> get<Any?>(key) })

    private fun webPattern(id: Long) =
        SavedPattern(
            id = id,
            source = SavedPatternSource.WebLink,
            name = "Pattern",
            designerName = "Designer",
            originalUrl = "https://example.com/pattern",
            canonicalUrl = "https://example.com/pattern",
            updatedAt = 5L,
        )
}
