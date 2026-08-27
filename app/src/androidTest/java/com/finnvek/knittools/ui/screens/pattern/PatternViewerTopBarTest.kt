package com.finnvek.knittools.ui.screens.pattern

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.io.FileOutputStream

class PatternViewerTopBarTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun longDocumentLabelKeepsBackAndOverflowActionsReachableAtNarrowLargeFont() {
        var documentsOpened = false
        val longLabel = "Main cardigan chart with a deliberately long label"
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                MaterialTheme {
                    Box(Modifier.width(320.dp)) {
                        PatternViewerTopBar(
                            state = topBarState(longLabel),
                            actions = topBarActions { documentsOpened = true },
                        )
                    }
                }
            }
        }

        composeRule.onNodeWithText(longLabel).assertIsDisplayed().assertIsFocused()
        composeRule.onNodeWithContentDescription("Back").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("More options").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Documents").assertIsDisplayed().performClick()
        composeRule.runOnIdle { assertEquals(true, documentsOpened) }
        captureScreenshot("viewer-top-bar-long-label")
    }

    private fun topBarState(label: String) =
        TopBarState(
            patternName = label,
            totalPages = 3,
            currentPage = 0,
            currentRow = 1,
            canDetachPattern = false,
            canManageDocuments = true,
            readingLineEnabled = false,
            readingLineFollowCurrentRow = true,
            verticalReadingGuideEnabled = false,
            canManageBookmarks = true,
            hasCurrentRowMarker = false,
            hasPageRowMarkers = false,
        )

    private fun topBarActions(onOpenDocuments: () -> Unit) =
        TopBarActions(
            onBack = {},
            onJumpToPage = {},
            onReadingLineToggle = {},
            onReadingLineFollowToggle = {},
            onReturnToCurrentRow = {},
            onVerticalReadingGuideToggle = {},
            onCenterVerticalReadingGuide = {},
            onOpenBookmarks = {},
            onOpenDocuments = onOpenDocuments,
            onSaveReadingLineAsCurrentRow = {},
            onClearReadingLineRowMarker = {},
            onClearReadingLinePageMarkers = {},
            onStartRowCalibration = {},
            onDetachPattern = {},
        )

    private fun captureScreenshot(name: String) {
        composeRule.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        val targetContext = instrumentation.targetContext
        val outputDir = File(targetContext.cacheDir, SCREENSHOT_DIRECTORY).apply { mkdirs() }
        val output = File(outputDir, "$name.png")
        FileOutputStream(output).use { stream ->
            assertEquals(true, bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream))
        }
        assertArrayEquals(PNG_SIGNATURE, output.inputStream().use { it.readNBytes(PNG_SIGNATURE.size) })
    }

    private companion object {
        private const val SCREENSHOT_DIRECTORY = "project-document-screenshots"
        private val PNG_SIGNATURE = byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10)
    }
}
