package com.finnvek.knittools.ui.screens.pattern

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.domain.model.ProjectDocument
import com.finnvek.knittools.domain.model.SavedPattern
import com.finnvek.knittools.domain.model.SavedPatternSource
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.io.FileOutputStream

class ProjectDocumentsSheetTest {
    @get:Rule
    val composeRule = createComposeRule()

    // CPD-OFF: Compose-testien skenaariokohtainen asetelma pidetaan testien yhteydessa.
    @Test
    fun webPatternInformationKeepsWebsiteActionsSeparateFromDocuments() {
        var opened = 0
        var edited = 0
        composeRule.setContent {
            MaterialTheme {
                ProjectDocumentsSheet(
                    state = state(),
                    metadataPattern = webPattern(),
                    onOpenPatternWebsite = { opened += 1 },
                    onEditPatternInformation = { edited += 1 },
                    onUnlinkPatternInformation = {},
                    onDismiss = {},
                    onSelect = {},
                    onRename = { _, _ -> },
                    onMoveEarlier = {},
                    onMoveLater = {},
                    onSetPrimary = {},
                    onRemove = {},
                    onAdd = {},
                    onClearError = {},
                )
            }
        }

        composeRule.onNodeWithText("Web cardigan").assertIsDisplayed()
        composeRule.onNodeWithText("Pattern designer").assertIsDisplayed()
        composeRule.onNodeWithText("example.com").assertIsDisplayed()
        composeRule.onNodeWithText("Open website").performClick()
        composeRule.onNodeWithText("Edit web pattern").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) { edited == 1 }
        composeRule.runOnIdle {
            assertEquals(1, opened)
            assertEquals(1, edited)
        }
    }

    @Test
    fun metadataOnlyWebPatternRequiresConfirmationBeforeUnlinkAndShowsNoDocumentActions() {
        var unlinked = 0
        composeRule.setContent {
            MaterialTheme {
                ProjectDocumentsSheet(
                    state = ProjectDocumentUiState(isLoading = false),
                    metadataPattern = webPattern(),
                    onUnlinkPatternInformation = { unlinked += 1 },
                    onDismiss = {},
                    onSelect = {},
                    onRename = { _, _ -> },
                    onMoveEarlier = {},
                    onMoveLater = {},
                    onSetPrimary = {},
                    onRemove = {},
                    onAdd = {},
                    onClearError = {},
                )
            }
        }

        composeRule.onNodeWithText("Set as primary").assertDoesNotExist()
        composeRule.onNodeWithText("Open PDF").assertDoesNotExist()
        composeRule.onNodeWithText("Unlink pattern information").performClick()
        val confirmationText = "Unlink pattern information for Web cardigan"
        composeRule.onNodeWithText(confirmationText).assertIsDisplayed()
        composeRule
            .onNode(
                hasText("Unlink pattern information") and
                    hasClickAction() and
                    hasAnyAncestor(isDialog() and hasAnyDescendant(hasText(confirmationText))) and
                    hasContentDescription(confirmationText).not(),
            ).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) { unlinked == 1 }
        composeRule.runOnIdle { assertEquals(1, unlinked) }
    }

    @Test
    fun addWaitsForDocumentSheetToHideBeforeOpeningNextSurface() {
        var addInvoked = false
        composeRule.setContent {
            MaterialTheme {
                ProjectDocumentsSheet(
                    state = state(),
                    onDismiss = {},
                    onSelect = {},
                    onRename = { _, _ -> },
                    onMoveEarlier = {},
                    onMoveLater = {},
                    onSetPrimary = {},
                    onRemove = {},
                    onAdd = { addInvoked = true },
                    onClearError = {},
                )
            }
        }

        composeRule.onNodeWithText("Add").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) { addInvoked }
        composeRule.runOnIdle { assertTrue(addInvoked) }
    }

    @Test
    fun rowsExposeSelectionAvailabilityAndOrderedActions() {
        var selectedId: Long? = null
        composeRule.setContent {
            MaterialTheme {
                ProjectDocumentsSheet(
                    state = state(),
                    metadataPatternName = "Ravelry cardigan",
                    onOpenPatternInformation = {},
                    onDismiss = {},
                    onSelect = { selectedId = it },
                    onRename = { _, _ -> },
                    onMoveEarlier = {},
                    onMoveLater = {},
                    onSetPrimary = {},
                    onRemove = {},
                    onAdd = {},
                    onClearError = {},
                )
            }
        }

        composeRule.onNodeWithText("Documents").assertIsDisplayed()
        composeRule.onAllNodesWithText("Pattern information")[0].assertIsDisplayed()
        composeRule.onNodeWithText("Ravelry cardigan").assertIsDisplayed()
        composeRule
            .onNodeWithText("Chart A")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeRule.runOnIdle { assertEquals(1L, selectedId) }
        composeRule.onNode(hasScrollToIndexAction()).performScrollToIndex(1)
        composeRule.onNodeWithText("PDF unavailable").assertIsDisplayed()
        composeRule.onNodeWithText("Chart B").performClick()
        composeRule.runOnIdle { assertEquals(1L, selectedId) }

        composeRule.onNode(hasScrollToIndexAction()).performScrollToIndex(0)
        composeRule.onNodeWithContentDescription("Actions for Chart A").performClick()
        composeRule
            .onNodeWithContentDescription("Actions for Chart A")
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(48.dp)
        composeRule.onNodeWithText("Move earlier").assertIsNotEnabled()
        composeRule.onNodeWithText("Move later").assertIsEnabled()
        composeRule.onNodeWithText("Rename").assertIsEnabled()
        composeRule.onNodeWithText("Remove").assertIsEnabled()
        captureScreenshot("many-documents-light")
    }

    @Test
    fun narrowLargeFontLayoutKeepsDocumentActionsReachable() {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                MaterialTheme(colorScheme = darkColorScheme()) {
                    Box(Modifier.width(320.dp).height(640.dp)) {
                        ProjectDocumentsSheet(
                            state =
                                state().copy(
                                    documents =
                                        listOf(
                                            document(
                                                1L,
                                                "Main cardigan chart with a deliberately long label",
                                                true,
                                            ),
                                            document(2L, "Chart B", false),
                                        ),
                                ),
                            onDismiss = {},
                            onSelect = {},
                            onRename = { _, _ -> },
                            onMoveEarlier = {},
                            onMoveLater = {},
                            onSetPrimary = {},
                            onRemove = {},
                            onAdd = {},
                            onClearError = {},
                        )
                    }
                }
            }
        }

        composeRule.onNodeWithText("Documents").assertIsDisplayed()
        composeRule.onNodeWithText("Add").assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription("Actions for Main cardigan chart with a deliberately long label")
            .assertIsDisplayed()
            .performClick()
        composeRule.onNodeWithText("Move later").assertIsDisplayed()
        captureScreenshot("narrow-large-font-dark-long-label")
    }

    @Test
    fun emptyStateKeepsAddDocumentAvailable() {
        composeRule.setContent {
            MaterialTheme {
                ProjectDocumentsSheet(
                    state = ProjectDocumentUiState(isLoading = false),
                    onDismiss = {},
                    onSelect = {},
                    onRename = { _, _ -> },
                    onMoveEarlier = {},
                    onMoveLater = {},
                    onSetPrimary = {},
                    onRemove = {},
                    onAdd = {},
                    onClearError = {},
                )
            }
        }

        composeRule.onNodeWithText("No documents").assertIsDisplayed()
        composeRule.onNodeWithText("Add").assertIsEnabled()
        captureScreenshot("no-documents-light")
    }

    @Test
    fun removingFinalDocumentExplainsReadablePatternWillBeGone() {
        composeRule.setContent {
            MaterialTheme {
                ProjectDocumentsSheet(
                    state =
                        ProjectDocumentUiState(
                            documents = listOf(document(1L, "Chart A", true)),
                            selectedDocumentId = 1L,
                            availability = mapOf(1L to true),
                            isLoading = false,
                        ),
                    onDismiss = {},
                    onSelect = {},
                    onRename = { _, _ -> },
                    onMoveEarlier = {},
                    onMoveLater = {},
                    onSetPrimary = {},
                    onRemove = {},
                    onAdd = {},
                    onClearError = {},
                )
            }
        }

        captureScreenshot("one-document-light")
        composeRule.onNodeWithContentDescription("Actions for Chart A").performClick()
        composeRule.onNodeWithText("Remove").performClick()
        composeRule
            .onNodeWithText("Remove Chart A? This project will no longer have an attached readable pattern.")
            .assertIsDisplayed()
        captureScreenshot("final-document-removal")
    }

    @Test
    fun renameDialogUsesSpecificLocalizedCopy() {
        composeRule.setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                ProjectDocumentsSheet(
                    state = state(),
                    onDismiss = {},
                    onSelect = {},
                    onRename = { _, _ -> },
                    onMoveEarlier = {},
                    onMoveLater = {},
                    onSetPrimary = {},
                    onRemove = {},
                    onAdd = {},
                    onClearError = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Actions for Chart A").performClick()
        composeRule.onNodeWithText("Rename").performClick()
        composeRule.onNodeWithText("Rename document").assertIsDisplayed()
        captureScreenshot("rename-dialog")
    }

    @Test
    fun primaryRemovalConfirmationNamesDeterministicPromotion() {
        composeRule.setContent {
            MaterialTheme {
                ProjectDocumentsSheet(
                    state = state(),
                    onDismiss = {},
                    onSelect = {},
                    onRename = { _, _ -> },
                    onMoveEarlier = {},
                    onMoveLater = {},
                    onSetPrimary = {},
                    onRemove = {},
                    onAdd = {},
                    onClearError = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Actions for Chart A").performClick()
        composeRule.onNodeWithText("Remove").performClick()
        composeRule
            .onNodeWithText("Remove Chart A? Another document will become primary.")
            .assertIsDisplayed()
        captureScreenshot("primary-document-removal")
    }

    @Test
    fun duplicateLabelsRemainSeparateRowsAndSecondaryCanBecomePrimary() {
        var primaryId: Long? = null
        composeRule.setContent {
            MaterialTheme {
                ProjectDocumentsSheet(
                    state =
                        state().copy(
                            documents =
                                listOf(
                                    document(1L, "Chart", true),
                                    document(2L, "Chart", false),
                                ),
                        ),
                    onDismiss = {},
                    onSelect = {},
                    onRename = { _, _ -> },
                    onMoveEarlier = {},
                    onMoveLater = {},
                    onSetPrimary = { primaryId = it },
                    onRemove = {},
                    onAdd = {},
                    onClearError = {},
                )
            }
        }

        assertEquals(2, composeRule.onAllNodesWithText("Chart").fetchSemanticsNodes().size)
        composeRule.onAllNodesWithContentDescription("Actions for Chart")[1].performClick()
        composeRule.onNodeWithText("Set as primary").performClick()
        composeRule.runOnIdle { assertEquals(2L, primaryId) }
    }

    // CPD-ON

    // CPD-OFF: Kuvakaappaus- ja testidata-apurit pidetaan testiluokan yhteydessa.
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

    private fun state() =
        ProjectDocumentUiState(
            documents =
                listOf(
                    document(id = 1L, label = "Chart A", primary = true),
                    document(id = 2L, label = "Chart B", primary = false),
                ),
            selectedDocumentId = 1L,
            availability = mapOf(1L to true, 2L to false),
            isLoading = false,
        )

    private fun webPattern() =
        SavedPattern(
            id = 9L,
            source = SavedPatternSource.WebLink,
            name = "Web cardigan",
            designerName = "Pattern designer",
            originalUrl = "https://example.com/pattern",
            canonicalUrl = "https://example.com/pattern",
        )

    private fun document(
        id: Long,
        label: String,
        primary: Boolean,
    ) = ProjectDocument(
        id = id,
        projectId = 7L,
        savedPatternId = null,
        documentKey = "document:$id",
        label = label,
        localPdfUri = "content://pattern/$id",
        sortOrder = id.toInt() - 1,
        isPrimary = primary,
        currentPage = 0,
        rowMapping = null,
        readingLineEnabled = false,
        readingLineYFraction = 0.5f,
        readingLineFollowCurrentRow = true,
        verticalReadingGuideEnabled = false,
        verticalReadingGuideXFraction = 0.5f,
        createdAt = id,
        updatedAt = id,
    )

    private companion object {
        private const val SCREENSHOT_DIRECTORY = "project-document-screenshots"
        private val PNG_SIGNATURE = byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10)
    }
    // CPD-ON
}
