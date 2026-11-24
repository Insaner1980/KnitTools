package com.finnvek.knittools.data.storage

import android.content.ContentResolver
import android.content.Context
import android.content.ContextWrapper
import android.content.res.AssetFileDescriptor
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

@RunWith(AndroidJUnit4::class)
class PatternDocumentStoragePdfImportTest {
    @get:Rule val fixture = TemporaryFolder(ApplicationProvider.getApplicationContext<Context>().cacheDir)

    private val context =
        object : ContextWrapper(ApplicationProvider.getApplicationContext<Context>()) {
            override fun getFilesDir(): File = File(fixture.root, "files").apply { mkdirs() }
        }
    private val storage = PatternDocumentStorage(Dispatchers.IO)
    private val sourceDirectory get() = fixture.root
    private val targetDirectory get() = File(context.filesDir, "pattern_pdfs/$PROJECT_ID")

    @Test
    fun validPdfIsPublishedOnlyAfterItCanBeOpened() =
        runTest {
            val source = File(sourceDirectory, "valid.pdf")
            writeOnePagePdf(source)

            val imported = storage.copyPdfToInternal(context, PROJECT_ID, source.toUri(), "chart.pdf")

            assertNotNull(imported)
            PdfPageRenderer(context, requireNotNull(imported).toUri()).use { renderer ->
                assertEquals(1, renderer.pageCount)
            }
            assertTrue(source.isFile)
        }

    @Test
    fun invalidPdfIsNotPublishedAndSourceIsPreserved() =
        runTest {
            val source = File(sourceDirectory, "invalid.pdf").apply { writeText("not a PDF") }

            val imported = storage.copyPdfToInternal(context, PROJECT_ID, source.toUri(), "invalid.pdf")

            assertNull(imported)
            assertTrue(targetDirectory.listFiles().orEmpty().isEmpty())
            assertEquals("not a PDF", source.readText())
        }

    @Test
    fun discardedIncomingCopyDoesNotReuseItsUriWhenTheSameNameIsSharedAgain() =
        runTest {
            val source = File(sourceDirectory, "shared-chart.pdf")
            writeOnePagePdf(source)
            val first = requireNotNull(storage.copyPdfToInternal(context, 0L, source.toUri(), source.name))
            val discarded = File(requireNotNull(first.toUri().path))
            assertTrue(discarded.delete())

            val second = requireNotNull(storage.copyPdfToInternal(context, 0L, source.toUri(), source.name))
            val replacement = File(requireNotNull(second.toUri().path))
            try {
                assertNotEquals(first, second)
                assertTrue(replacement.isFile)
            } finally {
                replacement.delete()
            }
        }

    @Test
    fun providerCloseFailureDoesNotLeaveAnUnreturnedCopy() =
        runTest {
            val source = File(sourceDirectory, "provider-source.pdf")
            writeOnePagePdf(source)
            val resolver =
                ContentResolver.wrap(
                    object : PdfTestContentProvider() {
                        override fun openAssetFile(
                            uri: Uri,
                            mode: String,
                        ): AssetFileDescriptor =
                            object : AssetFileDescriptor(
                                ParcelFileDescriptor.open(source, ParcelFileDescriptor.MODE_READ_ONLY),
                                0L,
                                UNKNOWN_LENGTH,
                            ) {
                                override fun createInputStream() =
                                    object : AutoCloseInputStream(this) {
                                        override fun close() {
                                            super.close()
                                            throw IOException("Source failed to close")
                                        }
                                    }
                            }
                    },
                )
            val sourceContext =
                object : ContextWrapper(context) {
                    override fun getContentResolver(): ContentResolver = resolver
                }
            targetDirectory.mkdirs()
            val existing = File(targetDirectory, "chart.pdf").apply { writeText("existing file") }

            val imported =
                storage.copyPdfToInternal(
                    sourceContext,
                    PROJECT_ID,
                    "content://knittools.test/chart.pdf".toUri(),
                    "chart.pdf",
                )

            assertNull(imported)
            assertEquals(listOf(existing), targetDirectory.listFiles().orEmpty().toList())
            assertEquals("existing file", existing.readText())
            assertTrue(source.isFile)
        }

    @Test
    fun cancelledReturnAfterNativeValidationRemovesOnlyUndeliveredCopy() =
        runTest {
            val source = File(sourceDirectory, "cancelled-source.pdf")
            writeOnePagePdf(source)
            val sourceBytes = source.readBytes()
            targetDirectory.mkdirs()
            val existing = File(targetDirectory, "chart.pdf").apply { writeText("existing file") }
            val copyFinished = CountDownLatch(1)
            val dispatcher =
                object : CoroutineDispatcher() {
                    override fun dispatch(
                        context: CoroutineContext,
                        block: Runnable,
                    ) {
                        Dispatchers.IO.dispatch(context) {
                            try {
                                block.run()
                            } finally {
                                copyFinished.countDown()
                            }
                        }
                    }
                }
            val copy =
                async(start = CoroutineStart.UNDISPATCHED) {
                    PatternDocumentStorage(dispatcher).copyPdfToInternal(
                        context = context,
                        projectId = PROJECT_ID,
                        sourceUri = source.toUri(),
                        fileName = "chart.pdf",
                    )
                }
            assertTrue("Native PDF copy must finish", copyFinished.await(15, TimeUnit.SECONDS))
            val published = File(targetDirectory, "chart-1.pdf")
            assertTrue(published.isFile)
            PdfPageRenderer(context, published.toUri()).use { assertEquals(1, it.pageCount) }
            copy.cancelAndJoin()

            assertTrue(copy.isCancelled)
            assertEquals(listOf(existing), targetDirectory.listFiles().orEmpty().toList())
            assertEquals("existing file", existing.readText())
            assertArrayEquals(sourceBytes, source.readBytes())
        }

    private companion object {
        const val PROJECT_ID = 93_400L
    }
}
