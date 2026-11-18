package com.finnvek.knittools.ui.screens.counter

import android.content.Context
import android.net.Uri
import com.finnvek.knittools.data.storage.AppFileStorage
import com.finnvek.knittools.data.storage.PatternDocumentStorage
import com.finnvek.knittools.repository.ProjectDocumentMutationResult
import com.finnvek.knittools.repository.SavedPatternRepository
import com.finnvek.knittools.ui.screens.pattern.ProjectDocumentError
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class CounterPatternAttachmentFailureTest : CounterViewModelFixture() {
    @Test
    fun savedPdfAttachmentReportsRepositoryFailure() =
        runTest {
            val model = viewModel()
            advanceUntilIdle()
            val errors = collectErrors(model.projectContentErrors)
            coEvery { repository.attachSavedPattern(7L, 15L) } throws IOException("Synthetic attachment failure")
            model.attachSavedPattern(
                com.finnvek.knittools.domain.model.SavedPattern(
                    id = 15L,
                    source = com.finnvek.knittools.domain.model.SavedPatternSource.Other,
                    name = "PDF",
                    designerName = "",
                    localPdfUri = "file:///pattern_pdfs/fixture.pdf",
                ),
            )
            advanceUntilIdle()
            assertEquals(1, errors.size)
        }

    @Test
    fun `unknown attachment outcome retains the imported copy`() = verifyCleanup(null)

    @Test
    fun `unattached copy cleanup rechecks shared file references`() = verifyCleanup(false)

    @Test
    fun `committed attachment retains the imported copy after cancellation`() = verifyCleanup(true)

    @Test
    fun `failed detachment retains the displayed pattern and reports an error`() = verifyDetachFailure(throws = true)

    @Test
    fun `typed detachment failure retains the displayed pattern and reports an error`() =
        verifyDetachFailure(throws = false)

    private fun verifyDetachFailure(throws: Boolean) =
        runTest {
            val project = observedProject.value.copy(patternUri = "content://pattern/41", patternName = "Pattern")
            observedProject.value = project
            val model = viewModel()
            model.selectProject(project)
            advanceUntilIdle()
            val before = model.uiState.value
            val errors = collectErrors(model.projectContentErrors)
            if (throws) {
                coEvery { repository.detachPattern(7L) } throws IOException("Synthetic write failure")
            } else {
                coEvery { repository.detachPattern(7L) } returns ProjectDocumentMutationResult.PersistenceFailure
            }

            model.detachPattern()
            advanceUntilIdle()

            assertEquals(before.patternUri, model.uiState.value.patternUri)
            assertEquals(before.patternName, model.uiState.value.patternName)
            assertEquals(before.currentPatternPage, model.uiState.value.currentPatternPage)
            assertEquals(before.patternRowMapping, model.uiState.value.patternRowMapping)
            assertEquals(1, errors.size)
        }

    private fun verifyCleanup(attached: Boolean?) =
        runTest {
            val source = "content://synthetic/incoming.pdf"
            val copied = "file:///pattern_pdfs/7/imported.pdf"
            val uri = mockk<Uri>()
            val context = mockk<Context>(relaxed = true)
            val storage = mockk<PatternDocumentStorage>()
            val patterns = mockk<SavedPatternRepository>(relaxed = true)
            mockkStatic(Uri::class)
            mockkObject(AppFileStorage)
            try {
                every { Uri.parse(source) } returns uri
                every { context.contentResolver.persistedUriPermissions } returns emptyList()
                every { AppFileStorage.isAppOwnedUri(context, uri) } returns false
                every { AppFileStorage.deleteIfAppOwned(context, copied) } returns Unit
                coEvery { storage.copyPdfToInternal(context, 7L, uri, "pattern.pdf") } returns copied
                coEvery { patterns.findReusableImportedPatternUrl(copied, "pattern.pdf") } returns null
                coEvery { repository.attachPattern(7L, copied, any(), 0, null) } throws
                    CancellationException("Synthetic cancellation while returning attachment result")
                if (attached == null) {
                    coEvery { repository.isPatternDocumentAttached(7L, copied) } throws
                        IOException("Synthetic read failure")
                } else {
                    coEvery { repository.isPatternDocumentAttached(7L, copied) } returns attached
                }
                val viewModel =
                    viewModel(
                        savedPatternsOverride = patterns,
                        patternStorageOverride = storage,
                        contextOverride = context,
                    )
                advanceUntilIdle()

                val feedback = mutableListOf<ProjectDocumentError?>()
                attachProjectPatternWithFeedback(viewModel, source, "pattern.pdf", 7L) { feedback.add(it) }
                advanceUntilIdle()

                assertTrue(feedback.isEmpty())
                coVerify(exactly = 1) { repository.attachPattern(7L, copied, any(), 0, null) }
                coVerify(exactly = 1) { repository.isPatternDocumentAttached(7L, copied) }
                verify(exactly = 0) { AppFileStorage.deleteIfAppOwned(context, copied) }
                coVerify(exactly = if (attached == false) 1 else 0) {
                    patterns.deleteLocalPatternFileIfUnused(copied)
                }
            } finally {
                unmockkObject(AppFileStorage)
                unmockkStatic(Uri::class)
            }
        }
}
