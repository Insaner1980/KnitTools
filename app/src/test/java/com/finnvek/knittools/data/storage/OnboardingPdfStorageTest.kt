package com.finnvek.knittools.data.storage

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OnboardingPdfStorageTest {
    private val context = mockk<Context>()
    private val resolver = mockk<ContentResolver>(relaxed = true)
    private val documents = mockk<PatternDocumentStorage>()
    private val uri = mockk<Uri>()

    @Before fun setup() {
        mockkStatic(Uri::class)
        every { Uri.parse("content://provider/pattern") } returns uri
        every { uri.scheme } returns "content"
        every { context.contentResolver } returns resolver
        every { resolver.persistedUriPermissions } returns emptyList()
        every { resolver.query(uri, any(), null, null, null) } returns null
    }

    @After fun teardown() = unmockkStatic(Uri::class)

    @Test fun `temporary read grant allows a copy when persistence is denied`() =
        runTest {
            every { resolver.takePersistableUriPermission(uri, any()) } throws SecurityException("temporary only")
            coEvery { documents.copyPdfToInternal(context, 0L, uri, "pattern.pdf") } returns "file:///copy.pdf"
            val storage = OnboardingPdfStorage(context, documents, StandardTestDispatcher(testScheduler))
            assertEquals("file:///copy.pdf" to "pattern.pdf", storage.copy("content://provider/pattern"))
            verify(exactly = 0) { resolver.releasePersistableUriPermission(any(), any()) }
        }

    @Test fun `acquired grant is released when copy is cancelled`() =
        runTest {
            coEvery { documents.copyPdfToInternal(any(), any(), any(), any()) } throws
                CancellationException("cancelled")
            val storage = OnboardingPdfStorage(context, documents, StandardTestDispatcher(testScheduler))
            assertTrue(
                runCatching { storage.copy("content://provider/pattern") }.exceptionOrNull() is CancellationException,
            )
            verify(exactly = 1) { resolver.releasePersistableUriPermission(uri, any()) }
        }

    @Test fun `non-content sources never reach the provider or copier`() =
        runTest {
            every { uri.scheme } returns "https"
            val storage = OnboardingPdfStorage(context, documents, StandardTestDispatcher(testScheduler))
            assertNull(storage.copy("content://provider/pattern"))
            verify(exactly = 0) { resolver.query(any(), any(), any(), any(), any()) }
            coVerify(exactly = 0) { documents.copyPdfToInternal(any(), any(), any(), any()) }
        }
}
