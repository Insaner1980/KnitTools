package com.finnvek.knittools.data.storage

import android.content.ContentProvider
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileNotFoundException
import java.security.MessageDigest
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class PatternPdfExportDestinationRuntimeTest {
    @Test
    fun shorterExportTruncatesContentDestination() =
        withFixture(rejectTruncation = false) { fixture ->
            with(fixture) {
                val control = "RW-03J short control".toByteArray()
                requireNotNull(context.contentResolver.openOutputStream(destinationUri, "w")).use {
                    it.write(control)
                }
                assertEquals(listOf("w"), provider.writeModes)
                assertEquals(destinationBefore.size.toLong(), destination.length())
                assertArrayEquals(control, destination.readBytes().copyOfRange(0, control.size))
                assertArrayEquals(
                    destinationBefore.copyOfRange(control.size, destinationBefore.size),
                    destination.readBytes().copyOfRange(control.size, destinationBefore.size),
                )
                emit(
                    "control",
                    mapOf("modes" to provider.writeModes.toList(), "retainedLength" to destination.length()),
                )
                destination.writeBytes(destinationBefore)
                provider.writeModes.clear()

                export()

                assertEquals(listOf("wt"), provider.writeModes)
                assertTrue(destination.length() in 1 until destinationBefore.size.toLong())
                assertFalse(destination.readBytes().toString(Charsets.ISO_8859_1).contains(TAIL))
                PdfPageRenderer(context, destinationUri).use { renderer ->
                    assertEquals(1, renderer.pageCount)
                    val bitmap = renderer.renderPage(0, 100)
                    try {
                        assertEquals(100, bitmap.width)
                        assertEquals(100, bitmap.height)
                    } finally {
                        bitmap.recycle()
                    }
                }
                assertSourceAndTempPreserved()
                emitResult("truncated", mapOf("pageCount" to 1, "renderedSize" to "100x100", "oldTailPresent" to false))
            }
        }

    @Test
    fun rejectedTruncationPreservesDestinationWithoutFallback() =
        withFixture(rejectTruncation = true) { fixture ->
            with(fixture) {
                val failure = runCatching { export() }.exceptionOrNull()

                assertTrue("Expected provider rejection, got $failure", failure is FileNotFoundException)
                assertEquals(REJECTION, failure?.message)
                assertEquals(listOf("wt"), provider.writeModes)
                assertArrayEquals(destinationBefore, destination.readBytes())
                assertSourceAndTempPreserved()
                emitResult("rejected", mapOf("failure" to failure.toString(), "fallbackToW" to false))
            }
        }

    private fun withFixture(
        rejectTruncation: Boolean,
        test: suspend (Fixture) -> Unit,
    ) = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Context>()
        assumeTrue(
            "Requires the activepolicy test profile",
            app.packageName == "com.finnvek.knittools.activepolicytest",
        )
        val root = File(app.cacheDir, "rw-03j-export-${UUID.randomUUID()}")
        assertTrue(root.mkdir())
        try {
            val files = File(root, "files").apply { check(mkdir()) }
            val cache = File(root, "cache").apply { check(mkdir()) }
            val source = File(files, "source.pdf").also(::writeOnePagePdf)
            val destination = File(files, "destination.pdf")
            val original = ByteArray(1024 * 1024) { 'X'.code.toByte() } + TAIL.toByteArray()
            destination.writeBytes(original)
            val uri = "content://${app.packageName}.rw03j.${root.name}/destination.pdf".toUri()
            val provider = DestinationProvider(uri, destination, rejectTruncation)
            provider.attachInfo(app, ProviderInfo().apply { authority = uri.authority })
            val resolver = ContentResolver.wrap(provider)
            val context =
                object : ContextWrapper(app) {
                    override fun getFilesDir(): File = files

                    override fun getCacheDir(): File = cache

                    override fun getContentResolver(): ContentResolver = resolver
                }
            test(Fixture(context, source, destination, uri, provider, source.readBytes(), original))
        } finally {
            assertTrue(root.deleteRecursively())
            assertFalse(root.exists())
            emit("cleanup", mapOf("root" to root.name, "rootRemoved" to !root.exists()))
        }
    }

    private class Fixture(
        val context: Context,
        val source: File,
        val destination: File,
        val destinationUri: Uri,
        val provider: DestinationProvider,
        val sourceBefore: ByteArray,
        val destinationBefore: ByteArray,
    ) {
        suspend fun export() {
            PatternPdfExporter(context, Dispatchers.IO).export(
                sourceUri = source.toUri(),
                destinationUri = destinationUri,
                annotations = emptyList(),
                trackerHighlights = emptyMap(),
                style =
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
                    ),
                onProgress = {},
            )
        }

        fun assertSourceAndTempPreserved() {
            assertArrayEquals(sourceBefore, source.readBytes())
            val exportCache = File(context.cacheDir, "pattern_exports")
            assertTrue(exportCache.isDirectory)
            assertTrue(requireNotNull(exportCache.listFiles()).isEmpty())
            assertEquals(
                setOf(source, destination),
                requireNotNull(source.parentFile).walkTopDown().filter(File::isFile).toSet(),
            )
        }

        fun emitResult(
            case: String,
            extra: Map<String, Any>,
        ) {
            emit(
                case,
                mapOf(
                    "provider" to "in-process ContentProvider via ContentResolver.wrap; real ParcelFileDescriptor",
                    "modes" to provider.writeModes.toList(),
                    "destinationBeforeLength" to destinationBefore.size,
                    "destinationAfterLength" to destination.length(),
                    "destinationBeforeSha256" to sha256(destinationBefore),
                    "destinationAfterSha256" to sha256(destination.readBytes()),
                    "sourceBeforeSha256" to sha256(sourceBefore),
                    "sourceAfterSha256" to sha256(source.readBytes()),
                    "exportTempFiles" to 0,
                ) + extra,
            )
        }
    }

    private class DestinationProvider(
        private val destinationUri: Uri,
        private val destination: File,
        private val rejectTruncation: Boolean,
    ) : ContentProvider() {
        val writeModes = mutableListOf<String>()

        override fun onCreate(): Boolean = true

        override fun getType(uri: Uri): String = "application/pdf"

        override fun openFile(
            uri: Uri,
            mode: String,
        ): ParcelFileDescriptor {
            require(uri == destinationUri)
            if (mode != "r") writeModes += mode
            if (mode == "wt" && rejectTruncation) throw FileNotFoundException(REJECTION)
            val flags =
                when (mode) {
                    "r" -> ParcelFileDescriptor.MODE_READ_ONLY
                    "w" -> ParcelFileDescriptor.MODE_WRITE_ONLY
                    "wt" -> ParcelFileDescriptor.MODE_WRITE_ONLY or ParcelFileDescriptor.MODE_TRUNCATE
                    else -> throw FileNotFoundException("Unsupported fixture mode: $mode")
                }
            return ParcelFileDescriptor.open(destination, flags)
        }

        override fun query(
            uri: Uri,
            projection: Array<out String>?,
            selection: String?,
            selectionArgs: Array<out String>?,
            sortOrder: String?,
        ): Cursor = throw UnsupportedOperationException()

        override fun insert(
            uri: Uri,
            values: ContentValues?,
        ): Uri = throw UnsupportedOperationException()

        override fun delete(
            uri: Uri,
            selection: String?,
            selectionArgs: Array<out String>?,
        ): Int = throw UnsupportedOperationException()

        override fun update(
            uri: Uri,
            values: ContentValues?,
            selection: String?,
            selectionArgs: Array<out String>?,
        ): Int = throw UnsupportedOperationException()
    }

    private companion object {
        const val TAIL = "RW-03J-OLD-DESTINATION-TRAILING-BYTES"
        const val REJECTION = "RW-03J provider rejects wt"

        fun sha256(bytes: ByteArray): String =
            MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

        fun emit(
            case: String,
            values: Map<String, Any>,
        ) {
            val result = JSONObject(values + ("case" to case))
            InstrumentationRegistry.getInstrumentation().sendStatus(
                2,
                Bundle().apply { putString("stream", "RW03J $result\n") },
            )
        }
    }
}
