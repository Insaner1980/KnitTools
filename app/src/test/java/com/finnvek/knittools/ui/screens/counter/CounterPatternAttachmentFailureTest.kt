package com.finnvek.knittools.ui.screens.counter

import android.content.Context
import android.net.Uri
import com.finnvek.knittools.data.storage.AppFileStorage
import com.finnvek.knittools.data.storage.PatternDocumentStorage
import com.finnvek.knittools.repository.SavedPatternRepository
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
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class CounterPatternAttachmentFailureTest : CounterViewModelFixture() {
    @Test
    fun `unknown attachment outcome retains the imported copy`() = verifyCleanup(null)

    @Test
    fun `unattached copy cleanup rechecks shared file references`() = verifyCleanup(false)

    @Test
    fun `committed attachment retains the imported copy after cancellation`() = verifyCleanup(true)

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

                viewModel.attachPattern(source, "pattern.pdf")
                advanceUntilIdle()

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
