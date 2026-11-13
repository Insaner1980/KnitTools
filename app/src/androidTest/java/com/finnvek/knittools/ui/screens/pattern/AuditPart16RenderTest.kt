package com.finnvek.knittools.ui.screens.pattern

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class AuditPart16RenderTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun pageJumpNeverPublishesOldBitmapEvenWhenCancelledRenderFinishesLate() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        check(context.packageName.endsWith(".activepolicytest"))
        val source = File(context.cacheDir, "audit16-render.pdf")
        val document = PdfDocument()
        try {
            repeat(3) { index ->
                val page = document.startPage(PdfDocument.PageInfo.Builder(100, 200, index + 1).create())
                page.canvas.drawColor(listOf(Color.RED, Color.GREEN, Color.BLUE)[index])
                document.finishPage(page)
            }
            source.outputStream().use(document::writeTo)
        } finally {
            document.close()
        }
        val page = mutableIntStateOf(0)
        val frames = CopyOnWriteArrayList<Pair<Int, Bitmap?>>()
        val colors = CopyOnWriteArrayList<Pair<Int, Int>>()
        val consumed = CopyOnWriteArrayList<Long>()
        var state: PatternRenderState? = null
        composeRule.setContent {
            val rendered = rememberPatternRenderState(source.toUri().toString(), page.intValue) {}
            rendered.renderedBitmap?.let { bitmap ->
                PatternDocumentViewport(
                    renderedBitmapProvider = { bitmap.asImageBitmap() },
                    contentDescription = "Synthetic PDF",
                    modifier = Modifier.size(300.dp, 300.dp),
                    currentPage = page.intValue,
                    focusRequest = if (page.intValue == 2) PatternViewportFocusRequest(16, 2, 0.8f) else null,
                    onFocusRequestConsumed = { consumed += it },
                )
            }
            SideEffect {
                state = rendered
                frames += page.intValue to rendered.renderedBitmap
                rendered.renderedBitmap?.let { bitmap -> colors += page.intValue to bitmap.getPixel(0, 0) }
            }
        }
        composeRule.waitUntil(15_000) { state?.renderedBitmap != null }
        val oldBitmap = requireNotNull(state?.renderedBitmap)
        val renderer = requireNotNull(state?.renderer)
        val locked = CountDownLatch(1)
        val release = CountDownLatch(1)
        val blocker =
            thread(name = "audit16-render-barrier") {
                synchronized(renderer) {
                    locked.countDown()
                    check(release.await(20, TimeUnit.SECONDS))
                }
            }
        try {
            assertTrue(locked.await(5, TimeUnit.SECONDS))
            composeRule.runOnIdle { page.intValue = 1 }
            composeRule.waitForIdle()
            composeRule.waitUntil(5_000) {
                Thread.getAllStackTraces().any { (thread, stack) ->
                    thread.state == Thread.State.BLOCKED &&
                        stack.any {
                            it.className == "com.finnvek.knittools.data.storage.PdfPageRenderer" &&
                                it.methodName.startsWith("renderPage")
                        }
                }
            }
            composeRule.runOnIdle { page.intValue = 2 }
            composeRule.waitForIdle()
            assertTrue("Bookmark focus must wait for its rendered page", consumed.isEmpty())
            release.countDown()
            blocker.join(5_000)
            composeRule.waitUntil(15_000) {
                state?.renderedBitmap?.let { !it.isRecycled && it.getPixel(0, 0) == Color.BLUE } == true
            }
            assertFalse(
                "A new page must not expose the previous page bitmap to bookmark focus",
                frames.any {
                    it.first != 0 && it.second === oldBitmap
                },
            )
            composeRule.waitUntil(5_000) { consumed.isNotEmpty() }
            org.junit.Assert.assertEquals(listOf(16L), consumed.toList())
            colors.forEach { (requestedPage, color) ->
                org.junit.Assert.assertEquals(listOf(Color.RED, Color.GREEN, Color.BLUE)[requestedPage], color)
            }
            assertTrue(oldBitmap.isRecycled)
        } finally {
            release.countDown()
            blocker.join(5_000)
            source.delete()
        }
    }
}
