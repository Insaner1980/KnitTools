package com.finnvek.knittools.ui

import android.graphics.Bitmap
import android.os.Build
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.ProjectYarnNote
import com.finnvek.knittools.domain.model.ProjectYarnUsageItem
import com.finnvek.knittools.domain.model.SavedPattern
import com.finnvek.knittools.domain.model.SavedPatternSource
import com.finnvek.knittools.domain.model.YarnUsageSource
import com.finnvek.knittools.pro.ProState
import com.finnvek.knittools.pro.ProStatus
import com.finnvek.knittools.ui.screens.counter.YarnManagementSheet
import com.finnvek.knittools.ui.screens.counter.YarnManagementSheetActions
import com.finnvek.knittools.ui.screens.library.SavedPatternDetailScreen
import com.finnvek.knittools.ui.screens.library.SavedPatternsActions
import com.finnvek.knittools.ui.screens.library.SavedPatternsScreen
import com.finnvek.knittools.ui.screens.library.SavedPatternsState
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
class MentalModelClarityRuntimeTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val arguments = InstrumentationRegistry.getArguments()
    private val language = arguments.getString("clarityLocale", "en")
    private val dark = arguments.getString("clarityDark", "false").toBoolean()
    private val context by lazy { instrumentation.targetContext.localizedTestContext(language) }
    private val title = "Cable cardigan with a long descriptive pattern title for winter evenings"

    private fun render(content: @Composable () -> Unit) {
        rule.setContent {
            CompositionLocalProvider(
                LocalContext provides context,
                LocalConfiguration provides context.resources.configuration,
                LocalResources provides context.resources,
            ) {
                KnitToolsTheme(isDarkTheme = dark, content = content)
            }
        }
    }

    private fun text(id: Int) = context.getString(id)

    private fun pattern(source: SavedPatternSource = SavedPatternSource.Ravelry) =
        SavedPattern(
            id = 7,
            source = source,
            name = title,
            designerName = "Synthetic designer",
            originalUrl = if (source == SavedPatternSource.WebLink) "https://example.invalid/pattern" else "",
            canonicalUrl =
                if (source ==
                    SavedPatternSource.Ravelry
                ) {
                    "https://www.ravelry.com/patterns/library/test"
                } else {
                    ""
                },
        )

    @Test fun singleDeletion() = deletion(SavedPatternSource.Ravelry)

    @Test fun webDeletion() = deletion(SavedPatternSource.WebLink)

    private fun deletion(source: SavedPatternSource) {
        var deleted = 0
        render {
            SavedPatternDetailScreen(
                pattern = pattern(source),
                onBack = {},
                onOpenPattern = {},
                onAttachToProject = { _, _ -> },
                onRemove = { deleted++ },
            )
        }
        val action =
            text(
                if (source ==
                    SavedPatternSource.WebLink
                ) {
                    R.string.web_pattern_delete
                } else {
                    R.string.remove_pattern
                },
            )
        val message =
            if (source == SavedPatternSource.WebLink) {
                context.getString(R.string.web_pattern_delete_confirm_message, title)
            } else {
                text(R.string.saved_pattern_detail_remove_confirm)
            }
        val moreOptions = text(R.string.more_options)
        // Poisto on kaavan sivun ylivuotovalikossa.
        rule.onNodeWithContentDescription(moreOptions).performClick()
        rule.onNodeWithText(action).performClick()
        capture("delete-${source.name}")
        assertReadable(message)
        rule.onNodeWithText(message).performTouchInput { swipeUp() }
        capture("delete-${source.name}-scrolled")
        rule.onNodeWithText(text(R.string.cancel)).assertIsDisplayed().performClick()
        rule.runOnIdle { assertEquals(0, deleted) }
        rule.onNodeWithContentDescription(moreOptions).performClick()
        rule.onNodeWithText(action).performClick()
        // Ravelry-ohjeen dialogin otsikko ja vahvistus ovat samaa tekstiä; vahvistus on viimeinen.
        rule
            .onAllNodesWithText(action)
            .onLast()
            .assertIsDisplayed()
            .performClick()
        rule.runOnIdle { assertEquals(1, deleted) }
    }

    @Test fun bulkDeletion() {
        var deleted = 0
        val patterns = listOf(pattern(), pattern(SavedPatternSource.WebLink).copy(id = 8))
        render {
            SavedPatternsScreen(
                SavedPatternsState(patterns, true, setOf(7, 8), 0),
                SavedPatternsActions({}, {}, {}, {}, {}, { deleted++ }, {}, {}),
            )
        }
        rule.onNodeWithText(text(R.string.delete)).performClick()
        capture("delete-bulk")
        assertReadable(context.resources.getQuantityString(R.plurals.delete_patterns_confirm, 2, 2))
        rule
            .onNodeWithText(context.resources.getQuantityString(R.plurals.delete_patterns_confirm, 2, 2))
            .performTouchInput { swipeUp() }
        capture("delete-bulk-scrolled")
        rule.onNodeWithText(text(R.string.cancel)).assertIsDisplayed().performClick()
        rule.runOnIdle { assertEquals(0, deleted) }
        rule.onNodeWithText(text(R.string.delete)).performClick()
        rule
            .onAllNodesWithText(text(R.string.delete))
            .onLast()
            .assertIsDisplayed()
            .performClick()
        rule.runOnIdle { assertEquals(1, deleted) }
    }

    @Test fun savedPdfStates() {
        val current = mutableStateOf(pattern())
        var openedPdf = false
        render {
            SavedPatternDetailScreen(current.value, {}, { openedPdf = true }, { _, _ -> }, {})
        }
        val explanation = text(R.string.saved_pattern_detail_no_pdf_explanation)
        rule.onNodeWithText(explanation).performScrollTo()
        capture("ravelry-no-pdf")
        assertReadable(explanation)
        rule
            .onNode(hasText(text(R.string.open_in_ravelry)) and hasClickAction())
            .performScrollTo()
            .assertIsDisplayed()
            .assertIsEnabled()
        rule.runOnIdle { current.value = pattern().copy(localPdfUri = "content://synthetic/unreadable.pdf") }
        rule.onAllNodesWithText(explanation).assertCountEquals(0)
        val openPdf = rule.onNodeWithText(text(R.string.saved_pattern_detail_open_pattern))
        openPdf.performScrollTo().assertIsDisplayed().assertIsEnabled()
        rule.onNodeWithText(text(R.string.saved_pattern_detail_pdf_attached)).assertIsDisplayed()
        capture("ravelry-attached-pdf")
        openPdf.performClick()
        rule.runOnIdle { assertTrue(openedPdf) }
    }

    @Test fun readabilityCheckRejectsClippingAndEllipsis() {
        val explanation = text(R.string.saved_pattern_detail_no_pdf_explanation)
        render {
            Column {
                Text(
                    explanation,
                    modifier = Modifier.width(120.dp).testTag("clipped-width"),
                    style = MaterialTheme.typography.bodyMedium,
                    softWrap = false,
                    // Keep this no-wrap control on the measured-layout path, without semantics rewrapping.
                    onTextLayout = {},
                )
                Text(
                    explanation,
                    modifier = Modifier.height(4.dp).testTag("clipped-height"),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    explanation,
                    modifier = Modifier.width(120.dp).testTag("ellipsized"),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        for ((tag, expectedFailure) in listOf(
            "clipped-width" to "Text exceeds horizontal bounds",
            "clipped-height" to "Text exceeds vertical bounds",
            "ellipsized" to "Text is ellipsized",
        )) {
            val node = rule.onNodeWithTag(tag).assertIsDisplayed()
            val failure = assertThrows(AssertionError::class.java) { assertReadable(node) }
            assertTrue(failure.message, failure.message.orEmpty().startsWith(expectedFailure))
        }
    }

    @Test fun projectYarnEntries() {
        val notes =
            mutableStateOf(
                listOf(
                    ProjectYarnNote(
                        1,
                        1,
                        "Long named wool and alpaca yarn for the winter cardigan",
                        notes = "Private project notes",
                    ),
                    ProjectYarnNote(2, 1, "Previously saved contrasting yarn", savedYarnCardId = 20),
                    ProjectYarnNote(3, 1, "Third project yarn"),
                ),
            )
        var saved = 0L
        var usageOpened = false
        render {
            YarnManagementSheet(
                linkedYarns = emptyList(),
                projectYarnNotes = notes.value,
                proState = ProState(status = ProStatus.PRO_PURCHASED),
                proAccessReady = true,
                actions =
                    YarnManagementSheetActions({
                    }, {
                    }, { _, _, _, _ ->
                        kotlinx.coroutines.CompletableDeferred(
                            com.finnvek.knittools.repository.ProjectYarnNoteEditResult.Saved,
                        )
                    }, {
                    }, {
                        saved =
                            it
                    }, {}),
                usageItems =
                    notes.value.map {
                        ProjectYarnUsageItem(
                            YarnUsageSource(projectYarnNoteId = it.id),
                            it.name,
                        )
                    },
                onUsage = { usageOpened = true },
            )
        }
        rule.onNode(isDialog()).performTouchInput { swipeUp() }
        rule.onAllNodesWithText(text(R.string.save_to_my_yarn_explanation))[0].performScrollTo()
        capture("yarn-unsaved")
        rule
            .onAllNodesWithText(text(R.string.save_to_my_yarn))[0]
            .performScrollTo()
            .assertIsEnabled()
            .performClick()
        rule.runOnIdle { assertEquals(1L, saved) }
        rule.onNodeWithText(text(R.string.saved_to_my_yarn_explanation)).performScrollTo()
        capture("yarn-saved")
        assertReadable(text(R.string.saved_to_my_yarn_explanation))
        val usage = context.getString(R.string.yarn_usage_track_named, notes.value[1].name)
        rule
            .onNodeWithContentDescription(usage)
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        rule.runOnIdle { assertTrue(usageOpened) }
        rule.onNodeWithText(notes.value.last().name).performScrollTo().assertIsDisplayed()
        capture("yarn-third-entry")
    }

    private fun assertReadable(value: String) = assertReadable(rule.onNodeWithText(value, useUnmergedTree = true))

    private fun assertReadable(textNode: SemanticsNodeInteraction) {
        val layouts = mutableListOf<TextLayoutResult>()
        textNode
            .assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertTrue(layouts.isNotEmpty())
        val node = textNode.fetchSemanticsNode()
        val details =
            buildString {
                appendLine("nodeSize=${node.size}; boundsInRoot=${node.boundsInRoot}")
                appendLine("boundsInWindow=${node.boundsInWindow}; positionInRoot=${node.positionInRoot}")
                layouts.forEach { layout ->
                    val input = layout.layoutInput
                    appendLine("text=${input.text.text}")
                    appendLine("constraints=${input.constraints}; size=${layout.size}")
                    appendLine("paragraph=${layout.multiParagraph.width}x${layout.multiParagraph.height}")
                    appendLine("density=${input.density.density}; fontScale=${input.density.fontScale}")
                    appendLine("fontSize=${input.style.fontSize}; lineHeight=${input.style.lineHeight}")
                    appendLine("overflow=${input.overflow}; maxLines=${input.maxLines}; softWrap=${input.softWrap}")
                    appendLine(
                        "didOverflowWidth=${layout.didOverflowWidth}; didOverflowHeight=${layout.didOverflowHeight}",
                    )
                    repeat(layout.lineCount) { line ->
                        appendLine(
                            "line=$line; left=${layout.getLineLeft(line)}; right=${layout.getLineRight(line)}; " +
                                "top=${layout.getLineTop(line)}; bottom=${layout.getLineBottom(line)}; " +
                                "start=${layout.getLineStart(line)}; end=${layout.getLineEnd(line)}; " +
                                "visibleEnd=${layout.getLineEnd(
                                    line,
                                    visibleEnd = true,
                                )}; ellipsized=${layout.isLineEllipsized(line)}",
                        )
                    }
                }
            }
        android.util.Log.i("ClarityTextLayout", details)
        File(context.getExternalFilesDir(null), "clarity-layout.txt").appendText(details)
        layouts.forEach { layout ->
            // Compose 1.12.1 recreates semantics at maxWidth but retains the smaller measured size.
            // Compare actual line bounds, not that reconstructed paragraph's empty width.
            val width = minOf(layout.size.width, node.size.width).toFloat()
            val height = minOf(layout.size.height, node.size.height).toFloat()
            repeat(layout.lineCount) { line ->
                assertFalse("Text is ellipsized: $details", layout.isLineEllipsized(line))
                assertTrue(
                    "Text exceeds horizontal bounds: $details",
                    layout.getLineLeft(line) >= -1f && layout.getLineRight(line) <= width + 1f,
                )
                assertTrue(
                    "Text exceeds vertical bounds: $details",
                    layout.getLineTop(line) >= -1f && layout.getLineBottom(line) <= height + 1f,
                )
            }
            assertFalse("Text exceeds maxLines: $details", layout.multiParagraph.didExceedMaxLines)
            assertTrue("Text has no lines: $details", layout.lineCount > 0)
            assertEquals(
                "Text ends early: $details",
                layout.layoutInput.text.length,
                layout.getLineEnd(
                    layout.lineCount - 1,
                ),
            )
            assertTrue("Text is horizontally clipped by its parent: $details", node.boundsInRoot.width >= width - 1f)
            assertTrue("Text is vertically clipped by its parent: $details", node.boundsInRoot.height >= height - 1f)
        }
    }

    private fun capture(name: String) {
        rule.waitForIdle()
        SystemClock.sleep(350)
        val directory = File(context.getExternalFilesDir(null), "clarity-qa/$language-$dark").apply { mkdirs() }
        val screenshot = instrumentation.uiAutomation.takeScreenshot()
        File(directory, "api-${Build.VERSION.SDK_INT}-$name.png").outputStream().use {
            screenshot.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        screenshot.recycle()
    }
}
