package com.finnvek.knittools.ui.screens.library

import android.content.Intent
import android.os.SystemClock
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.test.espresso.Espresso
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.finnvek.knittools.App
import com.finnvek.knittools.MainActivity
import com.finnvek.knittools.R
import com.finnvek.knittools.data.local.RoomDatabaseTransactionRunner
import com.finnvek.knittools.repository.SavedPatternRepository
import com.finnvek.knittools.ui.navigation.Screen
import com.finnvek.knittools.ui.navigation.WebPatternEditorOrigin
import com.finnvek.knittools.ui.navigation.WebPatternEditorRoute
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import com.finnvek.knittools.widget.WidgetEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import java.util.UUID

class WebPatternEditorScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun completionWaitsForStartedOwnerAndNavigatesOnce() {
        assumeTrue(context.packageName == "com.finnvek.knittools.activepolicytest")
        val app = context.applicationContext as App
        val database = app.database.get()
        val patterns =
            SavedPatternRepository(
                database.savedPatternDao(),
                app,
                database.counterProjectDao(),
                RoomDatabaseTransactionRunner(database),
                Dispatchers.IO,
            )
        val projects = EntryPointAccessors.fromApplication(app, WidgetEntryPoint::class.java).counterRepository()
        val owner =
            composeRule.runOnIdle {
                object : LifecycleOwner {
                    override val lifecycle = LifecycleRegistry(this).apply { currentState = Lifecycle.State.CREATED }
                }
            }
        val model =
            composeRule.runOnIdle {
                WebPatternEditorViewModel(
                    patterns,
                    projects,
                    SavedStateHandle(
                        mapOf(
                            Screen.WebPatternEditor.ARG_ORIGIN to WebPatternEditorOrigin.Manual.persistedValue,
                        ),
                    ),
                ).apply {
                    updateTitle("Lifecycle test pattern")
                    updateUrl("https://example.com/pattern-${UUID.randomUUID()}")
                    save()
                }
            }
        composeRule.waitUntil(5_000) { model.uiState.value.completion is WebPatternEditorCompletion.OpenDetail }
        val completion = model.uiState.value.completion as WebPatternEditorCompletion.OpenDetail
        val opened = mutableListOf<Long>()
        try {
            composeRule.setContent {
                CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                    KnitToolsTheme(isDarkTheme = false) {
                        WebPatternEditorScreen(
                            request = null,
                            onRequestStored = {},
                            onBack = {},
                            onOpenDetail = { opened.add(it) },
                            onOpenProject = { error("Unexpected project navigation") },
                            onOpenRavelry = { error("Unexpected Ravelry navigation") },
                            viewModelProvider = { model },
                        )
                    }
                }
            }
            composeRule.runOnIdle {
                assertTrue(opened.isEmpty())
                assertEquals(completion, model.uiState.value.completion)
                owner.lifecycle.currentState = Lifecycle.State.STARTED
            }
            composeRule.waitUntil(5_000) { model.uiState.value.completion == null }
            composeRule.runOnIdle {
                assertEquals(listOf(completion.patternId), opened)
                owner.lifecycle.currentState = Lifecycle.State.CREATED
            }
            composeRule.runOnIdle { owner.lifecycle.currentState = Lifecycle.State.STARTED }
            composeRule.runOnIdle { assertEquals(listOf(completion.patternId), opened) }
        } finally {
            runBlocking(Dispatchers.IO) { database.savedPatternDao().deleteById(completion.patternId) }
        }
    }

    // CPD-OFF: Compose-testien skenaariokohtainen asetelma pidetaan testien yhteydessa.
    // Tallenna on harmaa kunnes nimi ja osoite on annettu, kuten muissa lisäyslomakkeissa.
    @Test
    fun saveIsDisabledUntilRequiredFieldsAreFilled() {
        val state =
            mutableStateOf(WebPatternEditorUiState(route = WebPatternEditorRoute(WebPatternEditorOrigin.Manual)))
        composeRule.setContent {
            KnitToolsTheme(isDarkTheme = false) {
                WebPatternEditorContent(
                    state = state.value,
                    onBack = {},
                    onTitleChange = { state.value = state.value.copy(title = it) },
                    onDesignerChange = {},
                    onUrlChange = { state.value = state.value.copy(url = it) },
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
        composeRule.onNodeWithText(context.getString(R.string.save)).assertIsNotEnabled()
        composeRule.onNodeWithTag(WEB_PATTERN_TITLE_FIELD_TAG).performTextInput("Cable cardigan")
        composeRule.onNodeWithText(context.getString(R.string.save)).assertIsNotEnabled()
        composeRule.onNodeWithTag(WEB_PATTERN_URL_FIELD_TAG).performTextInput("https://example.com/pattern")
        composeRule.onNodeWithText(context.getString(R.string.save)).assertIsEnabled()
    }

    @Test
    fun correctingValidationKeepsFocusUntilAnotherSaveAttempt() {
        val state =
            mutableStateOf(
                WebPatternEditorUiState(
                    route = WebPatternEditorRoute(WebPatternEditorOrigin.Manual),
                    title = "Cable cardigan",
                    url = "ftp://example.com/pattern",
                ),
            )
        var saves = 0
        composeRule.setContent {
            KnitToolsTheme(isDarkTheme = false) {
                WebPatternEditorContent(
                    state = state.value,
                    onBack = {},
                    onTitleChange = { state.value = state.value.copy(title = it) },
                    onDesignerChange = { state.value = state.value.copy(designer = it) },
                    onUrlChange = { state.value = state.value.copy(url = it) },
                    onSave = { saves += 1 },
                    onKeepDraft = {},
                    onUseSharedLink = {},
                    onDismissReplacement = {},
                    onConfirmReplacement = {},
                )
            }
        }
        // Virheellinen osoite tarkistetaan tallennusyrityksellä: virhe näkyy ja fokus siirtyy osoitteeseen.
        composeRule.onNodeWithText(context.getString(R.string.save)).performClick()
        composeRule.onNodeWithText(context.getString(R.string.web_pattern_error_url_web_only)).assertIsDisplayed()
        composeRule.onNodeWithTag(WEB_PATTERN_URL_FIELD_TAG).assertIsFocused().performTextClearance()
        for (part in listOf("https://", "example.com", "/pattern")) {
            composeRule.onNodeWithTag(WEB_PATTERN_URL_FIELD_TAG).assertIsFocused().performTextInput(part)
            composeRule.onNodeWithTag(WEB_PATTERN_URL_FIELD_TAG).assertIsFocused()
        }
        composeRule.runOnIdle {
            assertEquals("https://example.com/pattern", state.value.url)
            assertEquals(0, saves)
        }
        composeRule.onNodeWithTag(WEB_PATTERN_URL_FIELD_TAG).assertIsFocused().performImeAction()
        composeRule.onNodeWithTag(WEB_PATTERN_DESIGNER_FIELD_TAG).assertIsFocused().performImeAction()
        composeRule.runOnIdle { assertEquals(1, saves) }
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
    fun backOnlyDismissesAfterSaveStopsBlocking() {
        val saving = mutableStateOf(false)
        var dismissals = 0
        composeRule.setContent {
            KnitToolsTheme {
                WebPatternEditorContent(
                    state =
                        WebPatternEditorUiState(
                            route = WebPatternEditorRoute(WebPatternEditorOrigin.Manual),
                            title = "Unsaved cardigan",
                            url = "https://example.com/unsaved",
                            isSaving = saving.value,
                        ),
                    onBack = { dismissals++ },
                    onTitleChange = {},
                    onDesignerChange = {},
                    onUrlChange = {},
                    onSave = { saving.value = true },
                    onKeepDraft = {},
                    onUseSharedLink = {},
                    onDismissReplacement = {},
                    onConfirmReplacement = {},
                )
            }
        }
        composeRule.onNodeWithText(context.getString(R.string.save)).performClick()
        composeRule.onNodeWithText(context.getString(R.string.cancel)).assertIsNotEnabled()
        Espresso.pressBack()
        composeRule.onNodeWithTag(WEB_PATTERN_TITLE_FIELD_TAG).assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals(0, dismissals)
            saving.value = false
        }
        Espresso.pressBack()
        composeRule.runOnIdle { assertEquals(1, dismissals) }
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

        composeRule.onNodeWithText(context.getString(R.string.web_pattern_edit_title)).assertIsDisplayed()
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

        // Pitkä nimi rivittyy. Tarkistetaan rivimäärä eikä dp-korkeutta, joka riippuu fontin leveydestä.
        val titleLayouts = mutableListOf<TextLayoutResult>()
        composeRule
            .onNodeWithTag(WEB_PATTERN_TITLE_FIELD_TAG)
            .assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action -> action(titleLayouts) }
        assertTrue(titleLayouts.single().lineCount > 1)
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
