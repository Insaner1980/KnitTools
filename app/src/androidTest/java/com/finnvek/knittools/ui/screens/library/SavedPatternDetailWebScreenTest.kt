package com.finnvek.knittools.ui.screens.library

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.SavedPattern
import com.finnvek.knittools.domain.model.SavedPatternSource
import com.finnvek.knittools.repository.SavedPatternMetadataMutationResult
import com.finnvek.knittools.ui.platform.ExternalWebLinkOpenResult
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SavedPatternDetailWebScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun webDetailShowsWebActionsAndHidesPdfAndRavelryMetadata() {
        val opened = mutableListOf<String>()
        var edited = 0
        var attached = 0
        var removed = 0
        composeRule.setContent {
            KnitToolsTheme {
                SavedPatternDetailScreen(
                    pattern = webPattern(),
                    onBack = {},
                    onOpenPattern = {},
                    onOpenWebsite = {
                        opened += it
                        ExternalWebLinkOpenResult.Opened
                    },
                    onEditWebPattern = { edited += 1 },
                    onAttachToProject = { attached += 1 },
                    onAttachWebPattern = { _, onResult ->
                        onResult(SavedPatternMetadataMutationResult.Attached(7L))
                    },
                    onRemove = { removed += 1 },
                )
            }
        }

        composeRule.onAllNodesWithText("Cable cardigan")[0].assertIsDisplayed()
        composeRule.onNodeWithText("example.com").assertIsDisplayed()
        composeRule.onNodeWithText("https://example.com/Pattern?Size=XL#Notes").assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.web_pattern_open_website)).performClick()
        composeRule.onNodeWithText(context.getString(R.string.web_pattern_edit)).performClick()
        composeRule.onNodeWithText(context.getString(R.string.web_pattern_attach)).performClick()
        composeRule.runOnIdle {
            assertEquals(listOf("https://example.com/Pattern?Size=XL#Notes"), opened)
            assertEquals(1, edited)
            assertEquals(1, attached)
        }

        composeRule.onAllNodesWithText(context.getString(R.string.availability_unknown)).assertCountEquals(0)
        composeRule.onAllNodesWithText(context.getString(R.string.open_in_ravelry)).assertCountEquals(0)
        composeRule
            .onAllNodesWithText(context.getString(R.string.saved_pattern_detail_open_pattern))
            .assertCountEquals(0)

        composeRule.onNodeWithText(context.getString(R.string.web_pattern_delete)).performClick()
        composeRule.onNodeWithText(context.getString(R.string.web_pattern_delete_confirm_title)).assertIsDisplayed()
        composeRule
            .onNodeWithText(context.getString(R.string.web_pattern_delete_confirm_message, "Cable cardigan"))
            .assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(0, removed) }
        composeRule.onAllNodesWithText(context.getString(R.string.web_pattern_delete))[1].performClick()
        composeRule.runOnIdle { assertEquals(1, removed) }
    }

    @Test
    fun webAttachNavigatesOnlyAfterMatchingReplacementConfirmation() {
        val expectedIds = mutableListOf<Long?>()
        var attached = 0
        composeRule.setContent {
            KnitToolsTheme {
                SavedPatternDetailScreen(
                    pattern = webPattern(),
                    onBack = {},
                    onOpenPattern = {},
                    onEditWebPattern = {},
                    onAttachToProject = { attached += 1 },
                    onAttachWebPattern = { expectedId, onResult ->
                        expectedIds += expectedId
                        onResult(
                            if (expectedId == null) {
                                SavedPatternMetadataMutationResult.ReplacementRequired(88L)
                            } else {
                                SavedPatternMetadataMutationResult.Attached(7L)
                            },
                        )
                    },
                    onRemove = {},
                )
            }
        }

        composeRule.onNodeWithText(context.getString(R.string.web_pattern_attach)).performClick()
        composeRule.onNodeWithText(context.getString(R.string.web_pattern_replace_confirm_title)).assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(0, attached) }

        composeRule.onAllNodesWithText(context.getString(R.string.web_pattern_attach))[1].performClick()
        composeRule.runOnIdle {
            assertEquals(listOf(null, 88L), expectedIds)
            assertEquals(1, attached)
        }
    }

    private fun webPattern() =
        SavedPattern(
            id = 7L,
            source = SavedPatternSource.WebLink,
            name = "Cable cardigan",
            designerName = "Pattern designer",
            originalUrl = "https://example.com/Pattern?Size=XL#Notes",
            canonicalUrl = "https://example.com/Pattern?Size=XL#Notes",
        )
}
