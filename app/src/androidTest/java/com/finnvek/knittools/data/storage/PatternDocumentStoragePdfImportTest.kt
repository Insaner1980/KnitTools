package com.finnvek.knittools.data.storage

import android.content.ContentProvider
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.content.res.AssetFileDescriptor
import android.database.Cursor
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
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

@RunWith(AndroidJUnit4::class)
class PatternDocumentStoragePdfImportTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val storage = PatternDocumentStorage(Dispatchers.IO)
    private val sourceDirectory = File(context.cacheDir, "pattern-pdf-import-test")
    private val targetDirectory = File(context.filesDir, "pattern_pdfs/$PROJECT_ID")

    @Before
    fun setUp() {
        sourceDirectory.deleteRecursively()
        targetDirectory.deleteRecursively()
        sourceDirectory.mkdirs()
    }

    @After
    fun tearDown() {
        sourceDirectory.deleteRecursively()
        targetDirectory.deleteRecursively()
    }

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
                    object : ContentProvider() {
                        override fun onCreate() = true

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

                        override fun getType(uri: Uri) = "application/pdf"

                        override fun query(
                            uri: Uri,
                            projection: Array<out String>?,
                            selection: String?,
                            selectionArgs: Array<out String>?,
                            sortOrder: String?,
                        ): Cursor? = null

                        override fun insert(
                            uri: Uri,
                            values: ContentValues?,
                        ): Uri? = null

                        override fun update(
                            uri: Uri,
                            values: ContentValues?,
                            selection: String?,
                            selectionArgs: Array<out String>?,
                        ) = 0

                        override fun delete(
                            uri: Uri,
                            selection: String?,
                            selectionArgs: Array<out String>?,
                        ) = 0
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
