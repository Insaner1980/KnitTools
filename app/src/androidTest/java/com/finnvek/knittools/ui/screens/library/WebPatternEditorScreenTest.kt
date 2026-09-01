package com.finnvek.knittools.ui.screens.library

import android.content.Intent
import android.os.SystemClock
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.finnvek.knittools.MainActivity
import com.finnvek.knittools.R
import com.finnvek.knittools.ui.navigation.WebPatternEditorOrigin
import com.finnvek.knittools.ui.navigation.WebPatternEditorRoute
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class WebPatternEditorScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    // CPD-OFF: Compose-testien skenaariokohtainen asetelma pidetaan testien yhteydessa.
    @Test
    fun saveAttemptShowsPersistentErrorsAndFocusesFirstInvalidField() {
        composeRule.setContent {
            KnitToolsTheme(isDarkTheme = false) {
                WebPatternEditorContent(
                    state = WebPatternEditorUiState(route = WebPatternEditorRoute(WebPatternEditorOrigin.Manual)),
                    onBack = {},
                    onTitleChange = {},
                    onDesignerChange = {},
                    onUrlChange = {},
                    onSave = {},
                    onKeepDraft = {},
                    onUseSharedLink = {},
                    onDismissReplacement = {},
                    onConfirmReplacement = {},
                )
            }
        }

        composeRule.onNodeWithText(context.getString(R.string.web_pattern_title_label)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.web_pattern_url_label)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.web_pattern_designer_label)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.save)).performClick()
        composeRule.onNodeWithText(context.getString(R.string.web_pattern_error_title_required)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.web_pattern_error_url_required)).assertIsDisplayed()
        composeRule.onNodeWithTag(WEB_PATTERN_TITLE_FIELD_TAG).assertIsFocused()
    }

    @Test
    fun imeNextMovesAcrossFieldsAndDoneSavesValidDraft() {
        var saves = 0
        composeRule.setContent {
            KnitToolsTheme(isDarkTheme = false) {
                WebPatternEditorContent(
                    state =
                        WebPatternEditorUiState(
                            route = WebPatternEditorRoute(WebPatternEditorOrigin.Manual),
                            title = "Cable cardigan",
                            url = "https://example.com/pattern",
                        ),
                    onBack = {},
                    onTitleChange = {},
                    onDesignerChange = {},
                    onUrlChange = {},
                    onSave = { saves += 1 },
                    onKeepDraft = {},
                    onUseSharedLink = {},
                    onDismissReplacement = {},
                    onConfirmReplacement = {},
                )
            }
        }

        composeRule.onNodeWithTag(WEB_PATTERN_TITLE_FIELD_TAG).performClick().performImeAction()
        composeRule.onNodeWithTag(WEB_PATTERN_URL_FIELD_TAG).assertIsFocused().performImeAction()
        composeRule.onNodeWithTag(WEB_PATTERN_DESIGNER_FIELD_TAG).assertIsFocused().performImeAction()
        composeRule.runOnIdle { assertEquals(1, saves) }
    }

    @Test
    fun unsupportedUrlShowsValidationAndDoesNotSave() {
        var saves = 0
        composeRule.setContent {
            KnitToolsTheme(isDarkTheme = false) {
                WebPatternEditorContent(
                    state =
                        WebPatternEditorUiState(
                            route = WebPatternEditorRoute(WebPatternEditorOrigin.Manual),
                            title = "Cable cardigan",
                            url = "ftp://example.com/pattern",
                        ),
                    onBack = {},
                    onTitleChange = {},
                    onDesignerChange = {},
                    onUrlChange = {},
                    onSave = { saves += 1 },
                    onKeepDraft = {},
                    onUseSharedLink = {},
                    onDismissReplacement = {},
                    onConfirmReplacement = {},
                )
            }
        }

        composeRule.onNodeWithText(context.getString(R.string.save)).performClick()

        composeRule.onNodeWithText(context.getString(R.string.web_pattern_error_url_web_only)).assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(0, saves) }
    }

    @Test
    fun cancelLeavesValidDraftWithoutSaving() {
        var cancellations = 0
        var saves = 0
        composeRule.setContent {
            KnitToolsTheme(isDarkTheme = false) {
                WebPatternEditorContent(
                    state =
                        WebPatternEditorUiState(
                            route = WebPatternEditorRoute(WebPatternEditorOrigin.Manual),
                            title = "Unsaved cardigan",
                            url = "https://example.com/unsaved",
                        ),
                    onBack = { cancellations += 1 },
                    onTitleChange = {},
                    onDesignerChange = {},
                    onUrlChange = {},
                    onSave = { saves += 1 },
                    onKeepDraft = {},
                    onUseSharedLink = {},
                    onDismissReplacement = {},
                    onConfirmReplacement = {},
                )
            }
        }

        composeRule.onNodeWithText(context.getString(R.string.cancel)).performClick()

        composeRule.runOnIdle {
            assertEquals(1, cancellations)
            assertEquals(0, saves)
        }
    }

    @Test
    fun validHttpDraftShowsSourceAndSavesOnlyAfterConfirmation() {
        var saves = 0
        composeRule.setContent {
            KnitToolsTheme {
                WebPatternEditorContent(
                    state =
                        WebPatternEditorUiState(
                            route = WebPatternEditorRoute(WebPatternEditorOrigin.Share),
                            title = "Cable cardigan",
                            designer = "Designer",
                            url = "http://example.com/pattern",
                        ),
                    onBack = {},
                    onTitleChange = {},
                    onDesignerChange = {},
                    onUrlChange = {},
                    onSave = { saves += 1 },
                    onKeepDraft = {},
                    onUseSharedLink = {},
                    onDismissReplacement = {},
                    onConfirmReplacement = {},
                )
            }
        }

        composeRule.onNodeWithText("Cable cardigan").assertIsDisplayed()
        composeRule.onNodeWithText("example.com").assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.web_pattern_http_warning)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.web_pattern_source_controlled)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.save)).performClick()

        composeRule.runOnIdle { assertEquals(1, saves) }
    }

    @Test
    fun editEditorShowsExistingDraftAndDuplicateFeedback() {
        composeRule.setContent {
            KnitToolsTheme(isDarkTheme = false) {
                WebPatternEditorContent(
                    state =
                        WebPatternEditorUiState(
                            route = WebPatternEditorRoute(WebPatternEditorOrigin.Edit, patternId = 7L),
                            title = "Existing cardigan",
                            url = "https://example.com/existing",
                            error = WebPatternEditorError.AlreadySaved,
                        ),
                    onBack = {},
                    onTitleChange = {},
                    onDesignerChange = {},
                    onUrlChange = {},
                    onSave = {},
                    onKeepDraft = {},
                    onUseSharedLink = {},
                    onDismissReplacement = {},
                    onConfirmReplacement = {},
                )
            }
        }

        composeRule.onNodeWithText(context.getString(R.string.web_pattern_edit)).assertIsDisplayed()
        composeRule.onNodeWithTag(WEB_PATTERN_TITLE_FIELD_TAG).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.web_pattern_already_saved)).assertIsDisplayed()
    }

    @Test
    fun sharedLinkWithoutTitleFocusesTitleBeforeAnySaveAttempt() {
        composeRule.setContent {
            KnitToolsTheme {
                WebPatternEditorContent(
                    state =
                        WebPatternEditorUiState(
                            route = WebPatternEditorRoute(WebPatternEditorOrigin.Share),
                            url = "https://example.com/pattern",
                        ),
                    onBack = {},
                    onTitleChange = {},
                    onDesignerChange = {},
                    onUrlChange = {},
                    onSave = {},
                    onKeepDraft = {},
                    onUseSharedLink = {},
                    onDismissReplacement = {},
                    onConfirmReplacement = {},
                )
            }
        }

        composeRule.onNodeWithTag(WEB_PATTERN_TITLE_FIELD_TAG).assertIsFocused()
    }

    @Test
    fun incomingDraftRequiresExplicitChoice() {
        var usedShared = 0
        composeRule.setContent {
            KnitToolsTheme {
                WebPatternEditorContent(
                    state =
                        WebPatternEditorUiState(
                            route = WebPatternEditorRoute(WebPatternEditorOrigin.Project, projectId = 42L),
                            title = "Replacement",
                            url = "https://example.com/replacement",
                            pendingIncomingShare =
                                PendingIncomingWebPatternShare(
                                    requestId = 1L,
                                    url = "https://example.com/shared",
                                    titleSuggestion = "Shared",
                                ),
                        ),
                    onBack = {},
                    onTitleChange = {},
                    onDesignerChange = {},
                    onUrlChange = {},
                    onSave = {},
                    onKeepDraft = {},
                    onUseSharedLink = { usedShared += 1 },
                    onDismissReplacement = {},
                    onConfirmReplacement = {},
                )
            }
        }

        composeRule.onNodeWithText(context.getString(R.string.web_pattern_use_shared_link)).performClick()
        composeRule.runOnIdle { assertEquals(1, usedShared) }
    }

    @Test
    fun incomingDraftCanKeepTheCurrentDraft() {
        var keptCurrent = 0
        composeRule.setContent {
            KnitToolsTheme(isDarkTheme = false) {
                WebPatternEditorContent(
                    state =
                        WebPatternEditorUiState(
                            route = WebPatternEditorRoute(WebPatternEditorOrigin.Manual),
                            title = "Current draft",
                            url = "https://example.com/current",
                            pendingIncomingShare =
                                PendingIncomingWebPatternShare(
                                    requestId = 2L,
                                    url = "https://example.com/shared",
                                    titleSuggestion = "Shared",
                                ),
                        ),
                    onBack = {},
                    onTitleChange = {},
                    onDesignerChange = {},
                    onUrlChange = {},
                    onSave = {},
                    onKeepDraft = { keptCurrent += 1 },
                    onUseSharedLink = {},
                    onDismissReplacement = {},
                    onConfirmReplacement = {},
                )
            }
        }

        composeRule.onNodeWithText(context.getString(R.string.web_pattern_keep_current_draft)).performClick()
        composeRule.runOnIdle { assertEquals(1, keptCurrent) }
    }

    @Test
    fun projectReplacementRequiresExplicitConfirmation() {
        var replaced = 0
        composeRule.setContent {
            KnitToolsTheme {
                WebPatternEditorContent(
                    state =
                        WebPatternEditorUiState(
                            route = WebPatternEditorRoute(WebPatternEditorOrigin.Project, projectId = 42L),
                            title = "Replacement",
                            url = "https://example.com/replacement",
                            didPersist = true,
                            pendingReplacement = PendingWebPatternReplacement(42L, 15L, 88L),
                        ),
                    onBack = {},
                    onTitleChange = {},
                    onDesignerChange = {},
                    onUrlChange = {},
                    onSave = {},
                    onKeepDraft = {},
                    onUseSharedLink = {},
                    onDismissReplacement = {},
                    onConfirmReplacement = { replaced += 1 },
                )
            }
        }

        composeRule.onNodeWithText(context.getString(R.string.web_pattern_replace_confirm_title)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.web_pattern_attach)).performClick()
        composeRule.runOnIdle { assertEquals(1, replaced) }
    }

    @Test
    fun narrowLargeFontDarkEditorKeepsLongSourceAndActionsReachable() {
        val longHost = "very-long-pattern-host-name.example.com"
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                KnitToolsTheme(isDarkTheme = true) {
                    Box(Modifier.width(320.dp).height(640.dp)) {
                        WebPatternEditorContent(
                            state =
                                WebPatternEditorUiState(
                                    route = WebPatternEditorRoute(WebPatternEditorOrigin.Edit, patternId = 7L),
                                    title = "A".repeat(120),
                                    url = "https://$longHost/patterns/${"section".repeat(12)}",
                                ),
                            onBack = {},
                            onTitleChange = {},
                            onDesignerChange = {},
                            onUrlChange = {},
                            onSave = {},
                            onKeepDraft = {},
                            onUseSharedLink = {},
                            onDismissReplacement = {},
                            onConfirmReplacement = {},
                        )
                    }
                }
            }
        }

        composeRule
            .onNodeWithTag(WEB_PATTERN_TITLE_FIELD_TAG)
            .assertIsDisplayed()
            .assertHeightIsAtLeast(120.dp)
        composeRule.onNodeWithText(longHost).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.save)).performScrollTo().assertIsDisplayed()
    }

    // CPD-ON
}

class PatternShareActivityLifecycleTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun coldRecreatedAndWarmShareIntentsAreConsumedOnce() {
        var activity: MainActivity? = null
        try {
            activity =
                instrumentation.startActivitySync(
                    shareIntent("https://example.com/cold").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                ) as MainActivity
            checkNotNull(activity).awaitClearedShareIntent()

            val previousActivity = checkNotNull(activity)
            instrumentation.runOnMainSync { previousActivity.recreate() }
            activity = awaitRecreatedActivity(previousActivity)
            val currentActivity = checkNotNull(activity)
            currentActivity.awaitClearedShareIntent()

            val warmIntent = shareIntent("https://example.com/warm")
            instrumentation.runOnMainSync {
                instrumentation.callActivityOnNewIntent(currentActivity, warmIntent)
            }
            currentActivity.awaitClearedShareIntent()
        } finally {
            activity?.let { current ->
                instrumentation.runOnMainSync { current.finishAndRemoveTask() }
                instrumentation.waitForIdleSync()
            }
        }
    }

    private fun shareIntent(url: String) =
        Intent(context, MainActivity::class.java)
            .setAction(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, url)

    private fun awaitRecreatedActivity(previousActivity: MainActivity): MainActivity {
        val deadline = SystemClock.uptimeMillis() + 5_000L
        var recreated: MainActivity? = null
        do {
            instrumentation.runOnMainSync {
                recreated =
                    ActivityLifecycleMonitorRegistry
                        .getInstance()
                        .getActivitiesInStage(Stage.RESUMED)
                        .filterIsInstance<MainActivity>()
                        .firstOrNull { it !== previousActivity }
            }
            if (recreated != null) break
            SystemClock.sleep(50L)
        } while (SystemClock.uptimeMillis() < deadline)
        return checkNotNull(recreated) { "Recreated MainActivity did not resume" }
    }

    private fun MainActivity.awaitClearedShareIntent() {
        val deadline = SystemClock.uptimeMillis() + 5_000L
        var current = Intent()
        do {
            instrumentation.runOnMainSync { current = Intent(intent) }
            if (current.action == Intent.ACTION_MAIN) break
            SystemClock.sleep(50L)
        } while (SystemClock.uptimeMillis() < deadline)

        assertEquals(Intent.ACTION_MAIN, current.action)
        assertNull(current.type)
        assertNull(current.getStringExtra(Intent.EXTRA_TEXT))
        assertNull(current.getStringExtra(Intent.EXTRA_SUBJECT))
    }
}
