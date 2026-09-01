package com.finnvek.knittools.ui.screens.pattern

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.finnvek.knittools.domain.model.PatternBookmark
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PatternReadingAssistanceComposeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun bookmarkSheetShowsEmptyStateAndValidatesAddDialog() {
        var addedName: String? = null
        composeRule.setContent {
            MaterialTheme {
                PatternBookmarkSheet(
                    state = PatternBookmarkUiState(documentKey = DOCUMENT_KEY, isLoading = false),
                    totalPages = 4,
                    actions = actions(onAdd = { addedName = it }),
                )
            }
        }

        composeRule.onNodeWithText("No bookmarks yet.").assertIsDisplayed()
        composeRule
            .onNodeWithText("Add bookmark")
            .assertIsEnabled()
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        composeRule.onNodeWithText("Bookmark name").performTextInput("Body")
        composeRule.onNodeWithText("Save").assertIsEnabled().performClick()

        composeRule.runOnIdle { assertEquals("Body", addedName) }
    }

    @Test
    fun bookmarkSheetExposesListNavigationRenameDeleteAndStaleError() {
        val bookmarks = listOf(bookmark(1, "Body", page = 0), bookmark(2, "Sleeve", page = 2))
        var jumpedId: Long? = null
        var nextCalls = 0
        composeRule.setContent {
            MaterialTheme {
                PatternBookmarkSheet(
                    state =
                        PatternBookmarkUiState(
                            documentKey = DOCUMENT_KEY,
                            bookmarks = bookmarks,
                            selectedBookmarkId = 1,
                            isLoading = false,
                            error = PatternBookmarkError.STALE_DOCUMENT,
                        ),
                    totalPages = 4,
                    actions =
                        actions(
                            onJump = { jumpedId = it },
                            onNext = { nextCalls += 1 },
                        ),
                )
            }
        }

        composeRule.onNodeWithText("This bookmark no longer belongs to this pattern.").assertIsDisplayed()
        composeRule.onNodeWithText("Previous bookmark").performScrollTo().assertIsNotEnabled()
        composeRule
            .onNodeWithText("Next bookmark")
            .performScrollTo()
            .assertIsEnabled()
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        composeRule.onNodeWithContentDescription("Jump to bookmark Sleeve, page 3").performClick()
        composeRule.onNodeWithContentDescription("More options: Body").performClick()
        composeRule.onNodeWithContentDescription("Rename bookmark: Body").performClick()
        composeRule.onNodeWithText("Rename bookmark").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.onNodeWithContentDescription("More options: Body").performClick()
        composeRule.onNodeWithContentDescription("Delete: Body").performClick()
        composeRule.onNodeWithText("Delete bookmark?").assertIsDisplayed()

        composeRule.runOnIdle {
            assertEquals(2L, jumpedId)
            assertEquals(1, nextCalls)
        }
    }

    @Test
    fun bookmarkSheetSupportsNarrowWidthLargeFontAndLongNames() {
        val longName = "Long localized bookmark name that remains readable"
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                MaterialTheme {
                    Box(Modifier.width(320.dp).height(640.dp)) {
                        PatternBookmarkSheet(
                            state =
                                PatternBookmarkUiState(
                                    documentKey = DOCUMENT_KEY,
                                    bookmarks = listOf(bookmark(1, longName, page = 9)),
                                    selectedBookmarkId = 1,
                                    isLoading = false,
                                ),
                            totalPages = 10,
                            actions = actions(),
                        )
                    }
                }
            }
        }

        composeRule.onNodeWithText(longName).assertIsDisplayed()
        composeRule.onNodeWithText("Page 10 of 10").assertIsDisplayed()
        composeRule
            .onNodeWithText("Previous bookmark")
            .performScrollTo()
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
        composeRule
            .onNodeWithText("Next bookmark")
            .performScrollTo()
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
    }

    // CPD-OFF: Kahden lukuohjaimen Compose-fixture pidetaan skenaarion yhteydessa.
    @Test
    fun simultaneousGuidesExposeFortyEightDpHandlesAndCustomActions() {
        var horizontal = 0.5f
        var vertical = 0.5f
        composeRule.setContent {
            MaterialTheme {
                Box(Modifier.width(320.dp).height(640.dp)) {
                    ReadingLineOverlay(
                        yFraction = horizontal,
                        currentRow = 12,
                        followingCurrentRow = false,
                        scale = 1f,
                        actions =
                            ReadingLineOverlayActions(
                                onDragStart = {},
                                onYFractionChange = { horizontal = it },
                                onYFractionCommit = { horizontal = it },
                                onDragCancel = {},
                            ),
                        modifier = Modifier.fillMaxSize(),
                    )
                    VerticalReadingGuideOverlay(
                        xFraction = vertical,
                        scale = 1f,
                        actions =
                            VerticalGuideOverlayActions(
                                onDragStart = {},
                                onXFractionChange = { vertical = it },
                                onXFractionCommit = { vertical = it },
                                onDragCancel = {},
                            ),
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        val horizontalNode = composeRule.onNodeWithContentDescription("Horizontal reading line")
        horizontalNode
            .assertWidthIsEqualTo(48.dp)
            .assertHeightIsAtLeast(48.dp)
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    "50% from the top. Row following paused.",
                ),
            )
        val verticalNode = composeRule.onNodeWithContentDescription("Vertical guide")
        verticalNode
            .assertWidthIsEqualTo(48.dp)
            .assertHeightIsAtLeast(48.dp)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "50% from the left"))

        val horizontalActions =
            horizontalNode.fetchSemanticsNode().config[SemanticsActions.CustomActions]
        val verticalActions =
            verticalNode.fetchSemanticsNode().config[SemanticsActions.CustomActions]
        composeRule.runOnIdle {
            horizontalActions.first { it.label == "Move reading line down" }.action()
            verticalActions.first { it.label == "Move vertical guide left" }.action()
        }

        composeRule.runOnIdle {
            assertEquals(0.52f, horizontal, 0.0001f)
            assertEquals(0.48f, vertical, 0.0001f)
        }
    }

    // CPD-ON

    @Test
    fun guideDragsCommitAfterPreviewRecomposition() {
        var horizontalCommit: Float? = null
        var verticalCommit: Float? = null
        composeRule.setContent {
            var horizontal by remember { mutableFloatStateOf(0.5f) }
            var vertical by remember { mutableFloatStateOf(0.5f) }
            MaterialTheme {
                Box(Modifier.width(320.dp).height(640.dp)) {
                    ReadingLineOverlay(
                        yFraction = horizontal,
                        currentRow = null,
                        followingCurrentRow = false,
                        scale = 1f,
                        actions =
                            ReadingLineOverlayActions(
                                onDragStart = {},
                                onYFractionChange = { horizontal = it },
                                onYFractionCommit = {
                                    horizontal = it
                                    horizontalCommit = it
                                },
                                onDragCancel = {},
                            ),
                        modifier = Modifier.fillMaxSize(),
                    )
                    VerticalReadingGuideOverlay(
                        xFraction = vertical,
                        scale = 1f,
                        actions =
                            VerticalGuideOverlayActions(
                                onDragStart = {},
                                onXFractionChange = { vertical = it },
                                onXFractionCommit = {
                                    vertical = it
                                    verticalCommit = it
                                },
                                onDragCancel = {},
                            ),
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        composeRule.onNodeWithContentDescription("Horizontal reading line").performTouchInput {
            down(center)
            moveTo(center + Offset(0f, 120f))
            up()
        }
        composeRule.onNodeWithContentDescription("Vertical guide").performTouchInput {
            down(center)
            moveTo(center + Offset(120f, 0f))
            up()
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            horizontalCommit != null && verticalCommit != null
        }
        composeRule.runOnIdle {
            assertTrue(horizontalCommit!! > 0.5f)
            assertTrue(verticalCommit!! > 0.5f)
        }
    }

    @Test
    fun viewportConsumesFocusOnlyAfterMatchingPageIsRendered() {
        var consumedRequestId: Long? = null
        composeRule.setContent {
            MaterialTheme {
                PatternDocumentViewport(
                    renderedBitmapProvider = { ImageBitmap(width = 100, height = 400) },
                    contentDescription = "Pattern page",
                    currentPage = 2,
                    focusRequest = PatternViewportFocusRequest(requestId = 17, pageIndex = 2, yFraction = 0.8f),
                    onFocusRequestConsumed = { consumedRequestId = it },
                    modifier = Modifier.width(320.dp).height(400.dp),
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) { consumedRequestId == 17L }
        composeRule.runOnIdle { assertTrue(consumedRequestId == 17L) }
    }

    private fun actions(
        onAdd: (String) -> Unit = {},
        onJump: (Long) -> Unit = {},
        onNext: () -> Unit = {},
    ) = PatternBookmarkSheetActions(
        onDismiss = {},
        onAdd = onAdd,
        onJump = onJump,
        onPrevious = {},
        onNext = onNext,
        onRename = { _, _ -> },
        onDelete = {},
        onClearError = {},
    )

    private fun bookmark(
        id: Long,
        name: String,
        page: Int,
    ) = PatternBookmark(
        id = id,
        projectId = 7,
        documentKey = DOCUMENT_KEY,
        name = name,
        pageIndex = page,
        yFraction = 0.5f,
        createdAt = id,
    )

    private companion object {
        const val DOCUMENT_KEY = "saved:91:v1"
    }
}
