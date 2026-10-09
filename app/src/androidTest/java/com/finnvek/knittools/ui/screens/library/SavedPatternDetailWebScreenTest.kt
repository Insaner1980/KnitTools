package com.finnvek.knittools.ui.screens.library

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.CounterProject
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
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun attachmentSuccessAfterRestorationIsHandledOnce() =
        verifyAttachmentAfterRestoration(SavedPatternMetadataMutationResult.Attached(7L))

    @Test
    fun attachmentFailureAfterRestorationAllowsRetry() =
        verifyAttachmentAfterRestoration(SavedPatternMetadataMutationResult.PersistenceFailure)

    @Test
    fun attachmentReplacementAfterRestorationRequiresConfirmationOnce() =
        verifyAttachmentAfterRestoration(SavedPatternMetadataMutationResult.ReplacementRequired(88L))

    private fun verifyAttachmentAfterRestoration(firstResult: SavedPatternMetadataMutationResult) {
        val expectedIds = mutableListOf<Long?>()
        val callbacks = mutableListOf<(SavedPatternMetadataMutationResult) -> Unit>()
        var attached = 0
        val content: @Composable () -> Unit = {
            KnitToolsTheme {
                SavedPatternDetailScreen(
                    pattern = webPattern(),
                    onBack = {},
                    onOpenPattern = {},
                    onAttachToProject = {},
                    projects = listOf(PROJECT),
                    onAttachWebPattern = { projectId, expectedId, onResult ->
                        assertEquals(PROJECT.id, projectId)
                        expectedIds += expectedId
                        callbacks += onResult
                    },
                    onOpenProject = { attached += 1 },
                    onRemove = {},
                )
            }
        }
        composeRule.runOnUiThread { composeRule.activity.setContent(content = content) }
        val attachLabel = context.getString(R.string.web_pattern_attach)
        composeRule.onNodeWithText(attachLabel).performScrollTo().performClick()
        chooseProject()
        recreate(content)
        composeRule.onNodeWithText(attachLabel).performScrollTo().performClick()
        chooseProject()
        composeRule.runOnIdle {
            assertEquals(listOf<Long?>(null), expectedIds)
            callbacks.single()(firstResult)
        }

        when (firstResult) {
            is SavedPatternMetadataMutationResult.Attached -> {
                composeRule.runOnIdle { assertEquals(1, attached) }
            }

            is SavedPatternMetadataMutationResult.ReplacementRequired -> {
                val title = context.getString(R.string.web_pattern_replace_confirm_title)
                composeRule.onNodeWithText(title).assertIsDisplayed()
                recreate(content)
                composeRule.onNodeWithText(title).assertIsDisplayed()
                composeRule.runOnIdle { assertEquals(0, attached) }
                composeRule.onAllNodesWithText(attachLabel)[1].performClick()
                recreate(content)
                composeRule.onAllNodesWithText(attachLabel)[1].performClick()
                composeRule.runOnIdle {
                    assertEquals(listOf(null, 88L), expectedIds)
                    callbacks.last()(SavedPatternMetadataMutationResult.Attached(7L))
                }
            }

            else -> {
                composeRule.onNodeWithText(context.getString(R.string.web_pattern_save_failed)).assertIsDisplayed()
                composeRule.runOnIdle { assertEquals(0, attached) }
                composeRule.onNodeWithText(attachLabel).performScrollTo().performClick()
                chooseProject()
                composeRule.runOnIdle {
                    assertEquals(listOf(null, null), expectedIds)
                    callbacks.last()(SavedPatternMetadataMutationResult.Attached(7L))
                }
            }
        }
        composeRule.runOnIdle { assertEquals(1, attached) }
        recreate(content)
        composeRule.runOnIdle { assertEquals(1, attached) }
    }

    private fun chooseProject() {
        composeRule.onNodeWithText(PROJECT.name).performClick()
    }

    private fun recreate(content: @Composable () -> Unit) {
        composeRule.activityRule.scenario.recreate()
        composeRule.runOnUiThread { composeRule.activity.setContent(content = content) }
        composeRule.waitForIdle()
    }

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
                    onAttachToProject = {},
                    projects = listOf(PROJECT),
                    onAttachWebPattern = { _, _, onResult ->
                        onResult(SavedPatternMetadataMutationResult.Attached(7L))
                    },
                    onOpenProject = { attached += 1 },
                    onRemove = { removed += 1 },
                )
            }
        }

        composeRule.onAllNodesWithText("Cable cardigan")[0].assertIsDisplayed()
        composeRule.onNodeWithText("example.com").assertIsDisplayed()
        composeRule.onNodeWithText("https://example.com/Pattern?Size=XL#Notes").assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.web_pattern_open_website)).performClick()
        // Muokkaus ja poisto ovat ylivuotovalikossa.
        composeRule.onNodeWithContentDescription(context.getString(R.string.more_options)).performClick()
        composeRule.onNodeWithText(context.getString(R.string.web_pattern_edit)).performClick()
        composeRule.onNodeWithText(context.getString(R.string.web_pattern_attach)).performClick()
        chooseProject()
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

        // Muokkaus ja poisto ovat ylivuotovalikossa.
        composeRule.onNodeWithContentDescription(context.getString(R.string.more_options)).performClick()
        composeRule.onNodeWithText(context.getString(R.string.web_pattern_delete)).performClick()
        composeRule.onNodeWithText(context.getString(R.string.web_pattern_delete_confirm_title)).assertIsDisplayed()
        composeRule
            .onNodeWithText(context.getString(R.string.web_pattern_delete_confirm_message, "Cable cardigan"))
            .assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(0, removed) }
        // Valikko sulkeutui, joten ainoa Delete-teksti on vahvistusdialogin painike.
        composeRule.onNodeWithText(context.getString(R.string.web_pattern_delete)).performClick()
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
                    onAttachToProject = {},
                    projects = listOf(PROJECT),
                    onOpenProject = { attached += 1 },
                    onAttachWebPattern = { _, expectedId, onResult ->
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
        chooseProject()
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

    private companion object {
        val PROJECT = CounterProject(id = 42L, name = "Sukat")
    }
}
