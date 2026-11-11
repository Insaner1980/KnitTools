package com.finnvek.knittools.ui.screens.pattern

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import androidx.core.net.toUri
import androidx.lifecycle.DEFAULT_ARGS_KEY
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.data.storage.PdfPageRenderer
import com.finnvek.knittools.ui.screens.counter.CounterActivityTestFixture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.util.UUID

class CodeReviewPart09ImageRuntimeTest : CounterActivityTestFixture() {
    @Test
    fun restoredDraftRejectsMissingAndForeignPrivateFiles() {
        check(app.packageName.endsWith(".activepolicytest"))
        val owner = System.nanoTime()
        val session = UUID.randomUUID().toString()
        val root = File(app.filesDir, "pattern_captures/$owner/$session").apply { check(mkdirs()) }
        val foreign = File(app.filesDir, "part09-${UUID.randomUUID()}.png")
        try {
            val valid = File(root, "valid.png")
            writeImage(valid)
            writeImage(foreign)
            val pages =
                listOf(valid, File(root, "missing.png"), foreign).mapIndexed { index, file ->
                    StagedPatternPage("page-$index", "content://part09/$index", file.path, 10, 4, 2)
                }
            lateinit var model: PatternImageImportViewModel
            rule.runOnUiThread {
                val args =
                    Bundle().apply {
                        putLong("pattern_image_project_id", owner)
                        putString("pattern_image_session_id", session)
                        putString("pattern_image_pages", encodeStagedPatternPages(pages))
                    }
                val extras =
                    MutableCreationExtras(rule.activity.defaultViewModelCreationExtras).apply {
                        set(DEFAULT_ARGS_KEY, args)
                    }
                model =
                    ViewModelProvider(
                        rule.activity.viewModelStore,
                        rule.activity.defaultViewModelProviderFactory,
                        extras,
                    ).get("part09-$session", PatternImageImportViewModel::class.java)
                assertTrue(
                    model.uiState.value.selection.pages
                        .isEmpty(),
                )
            }
            rule.waitUntil(10_000) { !model.uiState.value.isBusy }

            val restored = model.uiState.value.selection
            assertEquals(listOf("page-0"), restored.pages.map { it.id })
            assertEquals(valid.canonicalPath, restored.pages.single().stagedPath)
            assertEquals(valid.length(), model.uiState.value.selection.totalBytes)
            assertEquals(PatternImageImportError.UNREADABLE, model.uiState.value.error)
            assertTrue(foreign.isFile)
        } finally {
            foreign.delete()
            root.parentFile?.deleteRecursively()
        }
    }

    @Test
    fun nativeWriterFailureCannotEstablishReadablePdf() {
        check(app.packageName.endsWith(".activepolicytest"))
        val target = File(app.cacheDir, "part09-${UUID.randomUUID()}.pdf")
        try {
            val document = PdfDocument()
            val failure =
                try {
                    val page = document.startPage(PdfDocument.PageInfo.Builder(100, 100, 1).create())
                    page.canvas.drawColor(Color.RED)
                    document.finishPage(page)
                    runCatching {
                        target.outputStream().use { output ->
                            document.writeTo(
                                object : OutputStream() {
                                    override fun write(value: Int) = throw IOException("Synthetic disk failure")

                                    override fun write(
                                        bytes: ByteArray,
                                        offset: Int,
                                        count: Int,
                                    ) {
                                        output.write(bytes, offset, minOf(count, 8))
                                        throw IOException("Synthetic disk failure")
                                    }
                                },
                            )
                        }
                    }.exceptionOrNull()
                } finally {
                    document.close()
                }
            val readable =
                runCatching {
                    PdfPageRenderer(app, target.toUri()).use { it.pageCount > 0 }
                }.getOrDefault(false)
            InstrumentationRegistry.getInstrumentation().sendStatus(
                0,
                Bundle().apply {
                    putString(
                        "stream",
                        "PART09 native write exception=${failure?.javaClass?.simpleName} readable=$readable\n",
                    )
                },
            )
            assertFalse(readable)
        } finally {
            target.delete()
        }
    }

    private fun writeImage(file: File) {
        Bitmap.createBitmap(4, 2, Bitmap.Config.ARGB_8888).also { bitmap ->
            try {
                bitmap.eraseColor(Color.RED)
                file.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            } finally {
                bitmap.recycle()
            }
        }
    }
}
