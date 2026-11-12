package com.finnvek.knittools.data.storage

import android.content.Context
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
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
