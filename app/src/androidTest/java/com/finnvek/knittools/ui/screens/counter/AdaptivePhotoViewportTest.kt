package com.finnvek.knittools.ui.screens.counter

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.finnvek.knittools.ui.pinchZoomIn
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

class AdaptivePhotoViewportTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun pinchPanResetAndRestorationKeepTheImageVisibleAfterResizing() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val file = File(context.cacheDir, "adaptive-photo-checker.png")
        val bitmap = Bitmap.createBitmap(600, 400, Bitmap.Config.ARGB_8888)
        val paint = Paint()
        val canvas = Canvas(bitmap)
        for (x in 0..5) {
            for (y in 0..3) {
                paint.color = if ((x + y) % 2 == 0) android.graphics.Color.RED else android.graphics.Color.BLUE
                canvas.drawRect(x * 100f, y * 100f, (x + 1) * 100f, (y + 1) * 100f, paint)
            }
        }
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        var size by mutableStateOf(360 to 700)
        val restoration = StateRestorationTester(rule)
        restoration.setContent {
            KnitToolsTheme(isDarkTheme = true) {
                PhotoViewport(
                    file.toURI().toString(),
                    "Synthetic photo",
                    Modifier.requiredSize(size.first.dp, size.second.dp),
                )
            }
        }
        try {
            val image = rule.onNodeWithTag("photo-viewport")

            fun samples() = sample(image.captureToImage())
            rule.waitUntil(10_000) { samples().any { it.red > 0.9f || it.blue > 0.9f } }
            val fitted = samples()
            image.pinchZoomIn()
            val zoomed = samples()
            assertTrue("Pinch visibly zooms the image", fitted != zoomed)
            image.performTouchInput {
                down(center)
                moveTo(center + Offset(70f, 0f))
                up()
            }
            val panned = samples()
            assertTrue("Pan visibly moves the zoomed image", zoomed != panned)
            restoration.emulateSavedInstanceStateRestore()
            rule.waitUntil(10_000) { samples() == panned }
            for (next in listOf(800 to 270, 800 to 1100, 1280 to 670, 360 to 700)) {
                rule.runOnIdle { size = next }
                rule.waitForIdle()
                assertTrue("Image stays visible at $next", samples().any { it.red > 0.9f || it.blue > 0.9f })
            }
            assertEquals(panned, samples())
            image.performTouchInput { doubleClick() }
            assertEquals(fitted, samples())
        } finally {
            file.delete()
        }
    }

    private fun sample(image: ImageBitmap): List<androidx.compose.ui.graphics.Color> {
        val pixels = image.toPixelMap()
        return (1..9).flatMap { x -> (1..9).map { y -> pixels[image.width * x / 10, image.height * y / 10] } }
    }
}
