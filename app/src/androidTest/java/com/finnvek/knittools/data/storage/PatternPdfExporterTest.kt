package com.finnvek.knittools.data.storage

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.content.res.AssetFileDescriptor
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.finnvek.knittools.domain.model.NormalizedPatternBounds
import com.finnvek.knittools.domain.model.PatternAnnotation
import com.finnvek.knittools.domain.model.PatternAnnotationKind
import com.finnvek.knittools.domain.model.TextBoxPayload
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class PatternPdfExporterTest {
    @get:Rule val fixture = TemporaryFolder(ApplicationProvider.getApplicationContext<Context>().cacheDir)
    private lateinit var context: Context
    private lateinit var exporter: PatternPdfExporter

    @Before
    fun setUp() {
        context =
            object : ContextWrapper(ApplicationProvider.getApplicationContext<Context>()) {
                override fun getCacheDir(): File = fixture.root
            }
        exporter =
            PatternPdfExporter.createForTest(
                context = context,
                ioDispatcher = Dispatchers.IO,
                cacheSpaceProvider = { Long.MAX_VALUE },
            )
    }

    @Test
    fun exportWritesEveryPageAndUnicodeAnnotation() =
        runBlocking {
            val source = createPdf("source-multi.pdf", pageCount = 2)
            val destination = File(context.cacheDir, "annotated-multi.pdf")
            val progress = mutableListOf<PatternPdfExportProgress>()

            exporter.export(
                sourceUri = source.toUri(),
                destinationUri = destination.toUri(),
                annotations = listOf(unicodeTextAnnotation()),
                trackerHighlights = emptyMap(),
                style = renderStyle(),
                onProgress = progress::add,
            )

            PdfPageRenderer(context, source.toUri()).use { original ->
                PdfPageRenderer(context, destination.toUri()).use { rendered ->
                    assertEquals(2, rendered.pageCount)
                    repeat(
                        rendered.pageCount,
                    ) { page -> assertEquals(original.pageSize(page), rendered.pageSize(page)) }
                    val annotatedPage = rendered.renderPage(0, targetWidth = 300)
                    try {
                        assertTrue(
                            (30..270).any { x ->
                                (40..120).any { y ->
                                    Color.red(annotatedPage.getPixel(x, y)) <
                                        128
                                }
                            },
                        )
                    } finally {
                        annotatedPage.recycle()
                    }
                }
            }
            assertEquals(PatternPdfExportProgress(2, 2), progress.last())
            assertTrue(destination.length() > 0L)
            assertTempDirectoryEmpty()
        }

    @Test
    fun cancellationCleansTemporaryFile() =
        runBlocking {
            val source = createPdf("source-cancel.pdf", pageCount = 2)
            val destination = File(context.cacheDir, "annotated-cancel.pdf")

            val failure =
                runCatching {
                    exporter.export(
                        sourceUri = source.toUri(),
                        destinationUri = destination.toUri(),
                        annotations = emptyList(),
                        trackerHighlights = emptyMap(),
                        style = renderStyle(),
                    ) { progress ->
                        if (progress.completedPages == 1) throw CancellationException("test cancellation")
                    }
                }.exceptionOrNull()

            assertTrue(failure is CancellationException)
            assertFalse(destination.exists())
            assertTempDirectoryEmpty()
        }

    @Test
    fun failedExportDeletesOnlyEmptyMediaStoreDestination() =
        runBlocking {
            val source = createPdf("source-render-failure.pdf", pageCount = 1)
            val resolver = context.contentResolver
            for (input in listOf(source, File(context.cacheDir, "missing-source.pdf"))) {
                for (original in listOf(byteArrayOf(), SENTINEL)) {
                    val values =
                        ContentValues().apply {
                            put(MediaStore.Downloads.DISPLAY_NAME, "knittools-export-test-${UUID.randomUUID()}.pdf")
                            put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                            put(MediaStore.Downloads.IS_PENDING, 1)
                        }
                    val destination = requireNotNull(resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values))
                    try {
                        requireNotNull(resolver.openOutputStream(destination)).use { it.write(original) }
                        val published = ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
                        assertEquals(1, resolver.update(destination, published, null, null))
                        assertEquals(original.isEmpty(), IncompleteDestination.isEmpty(context, destination))

                        val failure =
                            runCatching {
                                exporter.export(
                                    sourceUri = input.toUri(),
                                    destinationUri = destination,
                                    annotations = emptyList(),
                                    trackerHighlights = emptyMap(),
                                    style = renderStyle(),
                                    onProgress = { throw IOException("Synthetic render failure") },
                                )
                            }.exceptionOrNull()

                        assertTrue("Expected an export failure for ${input.name}", failure is IOException)
                        val cursor = requireNotNull(resolver.query(destination, null, null, null, null))
                        val exists = cursor.use { it.moveToFirst() }
                        assertEquals("Destination after failure for ${input.name}", original.isNotEmpty(), exists)
                        if (exists) {
                            val actual = requireNotNull(resolver.openInputStream(destination)).use { it.readBytes() }
                            assertArrayEquals(original, actual)
                        }
                    } finally {
                        resolver.delete(destination, null, null)
                    }
                }
            }
            assertTempDirectoryEmpty()
        }

    @Test
    fun destinationWriteFailureCleansTemporaryFile() =
        runBlocking {
            val source = createPdf("source-error.pdf", pageCount = 1)

            val failure =
                runCatching {
                    exporter.export(
                        sourceUri = source.toUri(),
                        destinationUri = "content://invalid/export.pdf".toUri(),
                        annotations = emptyList(),
                        trackerHighlights = emptyMap(),
                        style = renderStyle(),
                        onProgress = {},
                    )
                }.exceptionOrNull()

            assertTrue(failure != null)
            assertTempDirectoryEmpty()
        }

    @Test
    fun partialDestinationIsDeletedWhenProviderWriteFails() =
        runBlocking {
            val source = createPdf("source-copy-error.pdf", pageCount = 1)
            val partial = fixture.newFile("partial.pdf")
            val provider = FailingWriteProvider(source, partial)
            val resolver = ContentResolver.wrap(provider)
            val wrappedContext =
                object : ContextWrapper(context) {
                    override fun getContentResolver(): ContentResolver = resolver
                }
            val pdfExporter = PatternPdfExporter.createForTest(wrappedContext, Dispatchers.IO)

            val failure =
                runCatching {
                    pdfExporter.export(
                        sourceUri = "content://knittools.test/${source.name}".toUri(),
                        destinationUri = "content://knittools.test/partial.pdf".toUri(),
                        annotations = emptyList(),
                        trackerHighlights = emptyMap(),
                        style = renderStyle(),
                        onProgress = {},
                    )
                }.exceptionOrNull()

            assertEquals("Synthetic destination write failure", failure?.message)
            assertTrue(provider.deletedAfterWrite)
            assertFalse(partial.exists())
            assertTempDirectoryEmpty()
        }

    @Test
    fun directExporterRejectsPageLimitBeforeRenderingOrDestinationWrite() =
        runBlocking {
            val source =
                createPdf(
                    "source-too-many-pages.pdf",
                    pageCount = DEFAULT_PATTERN_PDF_EXPORT_LIMITS.maxPageCount + 1,
                )
            val destination = File(context.cacheDir, "annotated-page-limit.pdf").apply { writeBytes(SENTINEL) }
            val progress = mutableListOf<PatternPdfExportProgress>()

            val failure = exportEmptyPdfFailure(exporter, source, destination, progress)

            assertLimitReason(PatternPdfExportLimitReason.PAGE_COUNT, failure)
            assertTrue(progress.isEmpty())
            assertArrayEquals(SENTINEL, destination.readBytes())
            assertTempDirectoryEmpty()
        }

    @Test
    fun directExporterRejectsAnnotationLimitBeforeDestinationWrite() =
        runBlocking {
            val source = createPdf("source-annotation-limit.pdf", pageCount = 1)
            val destination = File(context.cacheDir, "annotated-annotation-limit.pdf").apply { writeBytes(SENTINEL) }
            val annotations =
                List(PATTERN_PDF_EXPORT_MAX_ANNOTATIONS + 1) { index ->
                    unicodeTextAnnotation().copy(id = index.toLong(), zIndex = index.toLong())
                }

            val failure =
                runCatching {
                    exporter.export(
                        sourceUri = source.toUri(),
                        destinationUri = destination.toUri(),
                        annotations = annotations,
                        trackerHighlights = emptyMap(),
                        style = renderStyle(),
                        onProgress = {},
                    )
                }.exceptionOrNull()

            assertLimitReason(PatternPdfExportLimitReason.ANNOTATIONS, failure)
            assertArrayEquals(SENTINEL, destination.readBytes())
            assertTempDirectoryEmpty()
        }

    @Test
    fun insufficientCacheSpaceRejectsBeforeRendering() =
        runBlocking {
            val limits = DEFAULT_PATTERN_PDF_EXPORT_LIMITS
            val requiredBytes = limits.maxTemporaryOutputBytes + limits.cacheReserveBytes
            val lowSpaceExporter =
                PatternPdfExporter.createForTest(
                    context = context,
                    ioDispatcher = Dispatchers.IO,
                    limits = limits,
                    cacheSpaceProvider = { requiredBytes - 1L },
                )
            val source = createPdf("source-low-cache.pdf", pageCount = 1)
            val destination = File(context.cacheDir, "annotated-low-cache.pdf").apply { writeBytes(SENTINEL) }
            val progress = mutableListOf<PatternPdfExportProgress>()

            val failure = exportEmptyPdfFailure(lowSpaceExporter, source, destination, progress)

            assertLimitReason(PatternPdfExportLimitReason.CACHE_SPACE, failure)
            assertTrue(progress.isEmpty())
            assertArrayEquals(SENTINEL, destination.readBytes())
            assertTempDirectoryEmpty()
        }

    @Test
    fun storageDropDuringExportPreservesReserveAndCleansTemporaryFile() =
        runBlocking {
            val limits = DEFAULT_PATTERN_PDF_EXPORT_LIMITS
            var available = limits.maxTemporaryOutputBytes + limits.cacheReserveBytes
            val exporter =
                PatternPdfExporter.createForTest(
                    context = context,
                    ioDispatcher = Dispatchers.IO,
                    cacheSpaceProvider = { available },
                )
            val source = createPdf("source-storage-drop.pdf", pageCount = 1)
            val original = source.readBytes()
            val destination = File(context.cacheDir, "annotated-storage-drop.pdf").apply { writeBytes(SENTINEL) }

            val failure =
                runCatching {
                    exporter.export(
                        sourceUri = source.toUri(),
                        destinationUri = destination.toUri(),
                        annotations = emptyList(),
                        trackerHighlights = emptyMap(),
                        style = renderStyle(),
                        onProgress = { available = limits.cacheReserveBytes },
                    )
                }.exceptionOrNull()

            assertLimitReason(PatternPdfExportLimitReason.CACHE_SPACE, failure)
            assertArrayEquals(SENTINEL, destination.readBytes())
            assertArrayEquals(original, source.readBytes())
            assertTempDirectoryEmpty()
        }

    @Test
    fun temporaryOutputHardCapRejectsPartialArchiveAndCleansTempFile() =
        runBlocking {
            val cappedExporter =
                PatternPdfExporter.createForTest(
                    context = context,
                    ioDispatcher = Dispatchers.IO,
                    limits =
                        DEFAULT_PATTERN_PDF_EXPORT_LIMITS.copy(
                            maxTemporaryOutputBytes = 1L,
                            cacheReserveBytes = 0L,
                        ),
                    cacheSpaceProvider = { Long.MAX_VALUE },
                )
            val source = createPdf("source-output-cap.pdf", pageCount = 1)
            val destination = File(context.cacheDir, "annotated-output-cap.pdf").apply { writeBytes(SENTINEL) }

            val failure =
                runCatching {
                    cappedExporter.export(
                        sourceUri = source.toUri(),
                        destinationUri = destination.toUri(),
                        annotations = emptyList(),
                        trackerHighlights = emptyMap(),
                        style = renderStyle(),
                        onProgress = {},
                    )
                }.exceptionOrNull()

            assertLimitReason(PatternPdfExportLimitReason.OUTPUT_BYTES, failure)
            assertArrayEquals(SENTINEL, destination.readBytes())
            assertTempDirectoryEmpty()
        }

    @Test
    fun exportCannotOverwriteItsSource() =
        runBlocking {
            val source = createPdf("source-alias.pdf", pageCount = 2)
            val original = source.readBytes()
            val failure = exportEmptyPdfFailure(exporter, source, source, mutableListOf())
            assertArrayEquals("The source PDF must stay byte-identical", original, source.readBytes())
            assertTrue("An aliased destination must be rejected", failure != null)
            assertTempDirectoryEmpty()
        }

    @Test
    fun exportCannotOverwriteSourceThroughFileProviderAlias() =
        runBlocking {
            val captures = File(context.filesDir, "pattern_captures").apply { mkdirs() }
            val source = File.createTempFile("export-alias-", ".pdf", captures)
            try {
                createPdf("alias-input.pdf", 1).copyTo(source, overwrite = true)
                val before = source.readBytes()
                val failure =
                    runCatching {
                        exporter.export(
                            source.toUri(),
                            AppFileStorage.fileProviderUri(context, source),
                            emptyList(),
                            emptyMap(),
                            renderStyle(),
                        ) {
                        }
                    }.exceptionOrNull()
                assertArrayEquals(before, source.readBytes())
                assertTrue(failure != null)
                assertTempDirectoryEmpty()
            } finally {
                source.delete()
            }
        }

    private class FailingWriteProvider(
        private val source: File,
        private val partial: File,
    ) : PdfTestContentProvider() {
        var deletedAfterWrite = false
            private set

        override fun openFile(
            uri: Uri,
            mode: String,
        ): ParcelFileDescriptor {
            check(uri.lastPathSegment == source.name && mode == "r")
            return ParcelFileDescriptor.open(source, ParcelFileDescriptor.MODE_READ_ONLY)
        }

        override fun openAssetFile(
            uri: Uri,
            mode: String,
        ): AssetFileDescriptor =
            object : AssetFileDescriptor(
                ParcelFileDescriptor.open(partial, ParcelFileDescriptor.MODE_WRITE_ONLY),
                0L,
                UNKNOWN_LENGTH,
            ) {
                override fun createOutputStream() =
                    object : ParcelFileDescriptor.AutoCloseOutputStream(parcelFileDescriptor) {
                        override fun write(
                            buffer: ByteArray,
                            offset: Int,
                            count: Int,
                        ) {
                            super.write(buffer, offset, 1)
                            throw IOException("Synthetic destination write failure")
                        }
                    }
            }

        override fun delete(
            uri: Uri,
            selection: String?,
            selectionArgs: Array<out String>?,
        ): Int {
            deletedAfterWrite = partial.length() > 0
            return if (partial.delete()) 1 else 0
        }
    }

    private suspend fun exportEmptyPdfFailure(
        pdfExporter: PatternPdfExporter,
        source: File,
        destination: File,
        progress: MutableList<PatternPdfExportProgress>,
    ): Throwable? =
        runCatching {
            pdfExporter.export(
                sourceUri = source.toUri(),
                destinationUri = destination.toUri(),
                annotations = emptyList(),
                trackerHighlights = emptyMap(),
                style = renderStyle(),
                onProgress = progress::add,
            )
        }.exceptionOrNull()

    private fun createPdf(
        name: String,
        pageCount: Int,
    ): File {
        val file = File(context.cacheDir, name)
        val document = PdfDocument()
        try {
            repeat(pageCount) { index ->
                val page = document.startPage(PdfDocument.PageInfo.Builder(300, 400, index + 1).create())
                page.canvas.drawColor(Color.WHITE)
                document.finishPage(page)
            }
            file.outputStream().use(document::writeTo)
        } finally {
            document.close()
        }
        return file
    }

    private fun unicodeTextAnnotation() =
        PatternAnnotation(
            layerId = 1L,
            page = 0,
            kind = PatternAnnotationKind.TEXT_BOX,
            payload =
                TextBoxPayload(
                    bounds = NormalizedPatternBounds(0.1f, 0.1f, 0.9f, 0.3f),
                    text = "Neulehuomautus: ääkköset ✓",
                    textSizeSp = 24f,
                    textArgb = Color.BLACK,
                ),
            zIndex = 0L,
        )

    private fun renderStyle() =
        PatternAnnotationRenderStyle(
            referencePageSize = 1_000f,
            highlighterDefaultAlpha = 96,
            calloutBackgroundAlpha = 0.16f,
            calloutTextSize = 24f,
            calloutCornerRadius = 12f,
            chartGridArgb = Color.DKGRAY,
            chartGridWidth = 2f,
            arrowHeadMultiplier = 4f,
            arrowHeadAngle = 0.55f,
            minimumPressureScale = 0.15f,
        )

    private fun exportTempDirectory(): File = File(context.cacheDir, "pattern_exports")

    private fun assertTempDirectoryEmpty() {
        assertTrue(exportTempDirectory().listFiles().isNullOrEmpty())
    }

    private fun assertLimitReason(
        reason: PatternPdfExportLimitReason,
        failure: Throwable?,
    ) {
        assertTrue(failure is PatternPdfExportLimitException)
        assertEquals(reason, (failure as PatternPdfExportLimitException).reason)
    }

    private companion object {
        val SENTINEL = byteArrayOf(1, 2, 3, 4)
    }
}
