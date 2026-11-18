package com.finnvek.knittools.ui.screens.pattern

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.res.Configuration
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.NormalizedPatternPoint
import com.finnvek.knittools.domain.model.TextBoxPayload
import com.finnvek.knittools.ui.localeThemeCases
import com.finnvek.knittools.ui.navigation.awaitKeyboard
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.Locale

@RunWith(Parameterized::class)
class PatternAnnotationTextUiTest(
    private val language: String,
    private val dark: Boolean,
) {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val fixture = AnnotationTextTestFixture()
    private lateinit var localized: Context

    @Before fun setUp() =
        runBlocking {
            fixture.start()
            val base = InstrumentationRegistry.getInstrumentation().targetContext
            val arguments = InstrumentationRegistry.getArguments()
            arguments
                .getString(
                    "textWidth",
                )?.let { assertEquals(it.toInt(), base.resources.configuration.screenWidthDp) }
            arguments.getString("textFontScale")?.let {
                assertEquals(it.toFloat(), base.resources.configuration.fontScale, 0.01f)
            }
            val configuration =
                Configuration(
                    base.resources.configuration,
                ).apply { setLocale(Locale.forLanguageTag(language)) }
            localized = base.createConfigurationContext(configuration)
            compose.setContent {
                CompositionLocalProvider(
                    LocalContext provides localized,
                    LocalResources provides localized.resources,
                    LocalConfiguration provides configuration,
                ) {
                    KnitToolsTheme(isDarkTheme = dark) {
                        val state by fixture.vm.uiState.collectAsState()
                        PatternAnnotationToolbar(state, fixture.vm.patternToolbarActions())
                    }
                }
            }
            main { fixture.vm.selectAnnotationAt(NormalizedPatternPoint(0.5f, 0.3f)) }
            fixture.awaitState { it.selectedAnnotationIsEditable && it.selectedAnnotationIsText }
            Unit
        }

    @After fun tearDown() = runBlocking { fixture.close() }

    @Test fun editCancelSaveUndoRedoAndReadOnlyMaster() {
        val before = runBlocking { fixture.row() }
        val editAction = node(R.string.pattern_annotation_edit_text).performScrollTo()
        val editBounds = editAction.fetchSemanticsNode().touchBoundsInRoot
        val density = localized.resources.displayMetrics.density
        assertTrue(
            "Edit text touch target",
            editBounds.width / density >= 47.5f && editBounds.height / density >= 47.5f,
        )
        editAction.performClick()
        val input = compose.onNode(hasSetTextAction())
        input.assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("Original text")))
        input.performTextReplacement("Cancelled draft")
        click(R.string.cancel)
        assertEquals(before, runBlocking { fixture.row() })

        click(R.string.pattern_annotation_edit_text)
        input.assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("Original text")))
        input.performScrollTo().performClick().assertIsFocused()
        compose.awaitKeyboard(true)
        input.performTextReplacement(" \n")
        node(R.string.save).performScrollTo().assertIsNotEnabled()
        val edited = (1..18).joinToString("\n") { "$it: Wiederholungen und Strickanweisungen – äö 日本語" }
        input.performTextReplacement(edited)
        input.performScrollTo().assertIsFocused()
        assertTrue(input.fetchSemanticsNode().config.contains(SemanticsActions.SetText))
        for (control in listOf(input, node(R.string.cancel), node(R.string.save))) {
            control.performScrollTo().assertIsDisplayed()
            val bounds = control.fetchSemanticsNode().touchBoundsInRoot
            assertTrue("Touch height", bounds.height / density >= 47.5f)
            assertTrue("Touch width", bounds.width / density >= 47.5f)
        }
        assertAccessibilityFocusOnSave()
        assertLabelsFit()
        click(R.string.save)
        runBlocking { fixture.awaitState { !it.isSaving && it.canUndo && it.textEdit == null } }
        assertEquals(edited, runBlocking { (fixture.annotation().payload as TextBoxPayload).text })
        click(R.string.pattern_annotation_undo)
        runBlocking { fixture.awaitState { !it.isSaving && it.canRedo } }
        assertEquals("Original text", runBlocking { (fixture.annotation().payload as TextBoxPayload).text })
        click(R.string.pattern_annotation_redo)
        runBlocking { fixture.awaitState { !it.isSaving && !it.canRedo } }
        click(R.string.pattern_annotation_edit_text)
        input.assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString(edited)))
        click(R.string.cancel)

        runBlocking {
            main { fixture.vm.setProjectLayerVisible(false) }
            fixture.awaitState { !it.projectLayerVisible }
            main { fixture.vm.selectAnnotationAt(NormalizedPatternPoint(0.5f, 0.3f)) }
            fixture.awaitState { it.selectedAnnotationId != null && !it.selectedAnnotationIsEditable }
        }
        node(R.string.pattern_annotation_edit_text).assertDoesNotExist()
        assertEquals(
            "Original text",
            runBlocking {
                (
                    fixture.repository
                        .getForLayers(listOf(fixture.master))
                        .single()
                        .payload as TextBoxPayload
                ).text
            },
        )
    }

    @Test fun staleDialogClosesAndCompetingChangeShowsFailureWithoutLosingDraft() {
        click(R.string.pattern_annotation_edit_text)
        compose.onNode(hasSetTextAction()).performTextReplacement("Unsaved draft")
        runBlocking { main { fixture.vm.setCurrentPage(1) } }
        node(R.string.save).assertDoesNotExist()
        runBlocking {
            main { fixture.vm.setCurrentPage(0) }
            fixture.awaitState { it.currentPage == 0 && it.projectAnnotations.isNotEmpty() }
            main { fixture.vm.selectAnnotationAt(NormalizedPatternPoint(0.5f, 0.3f)) }
            fixture.awaitState { it.selectedAnnotationIsEditable }
        }
        click(R.string.pattern_annotation_edit_text)
        compose.onNode(hasSetTextAction()).performTextReplacement("My draft")
        runBlocking { fixture.repository.updateText(fixture.owner, fixture.annotation(), "Competitor") }
        click(R.string.save)
        runBlocking {
            fixture.awaitState { !it.isSaving && it.writeError == PatternAnnotationWriteError.TEXT_CONFLICT }
        }
        node(R.string.save).assertIsNotEnabled()
        node(R.string.pattern_annotation_write_failed).performScrollTo().assertIsDisplayed()
        assertLabelsFit()
        compose
            .onNode(
                hasSetTextAction(),
            ).assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("My draft")))
        assertEquals("Competitor", runBlocking { (fixture.annotation().payload as TextBoxPayload).text })
        click(R.string.cancel)
    }

    private fun node(id: Int) =
        if (id == R.string.cancel) {
            compose.onNode(hasText(localized.getString(id)) and hasAnyAncestor(isDialog()))
        } else {
            compose.onNodeWithText(localized.getString(id))
        }

    private fun assertLabelsFit() {
        val texts =
            compose.onAllNodes(
                SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult) and hasAnyAncestor(isDialog()),
                useUnmergedTree = true,
            )
        val nodes = texts.fetchSemanticsNodes()
        assertTrue("The text editor must expose text layout results", nodes.isNotEmpty())
        nodes.forEachIndexed { index, node ->
            val layouts = mutableListOf<TextLayoutResult>()
            texts[index].performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            layouts.forEach { layout ->
                repeat(layout.lineCount) { line ->
                    assertFalse(layout.layoutInput.text.text, layout.isLineEllipsized(line))
                    val right = layout.getLineRight(line)
                    val width = minOf(layout.size.width, node.size.width)
                    assertTrue("${layout.layoutInput.text.text}: right=$right width=$width", right <= width + 1f)
                }
            }
        }
    }

    private fun click(id: Int) {
        node(id).performScrollTo().performClick()
    }

    private fun assertAccessibilityFocusOnSave() {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val originalFlags = automation.serviceInfo.flags
        try {
            automation.serviceInfo =
                automation.serviceInfo.apply {
                    flags = flags or AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE
                }
            val manager = localized.getSystemService(AccessibilityManager::class.java)
            compose.waitUntil(5_000) { manager.isTouchExplorationEnabled }
            focusAccessibleSave()
        } finally {
            automation.serviceInfo = automation.serviceInfo.apply { flags = originalFlags }
        }
    }

    private fun focusAccessibleSave() {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        var save: AccessibilityNodeInfo? = null
        compose.waitUntil(5_000) {
            save = automation.rootInActiveWindow?.let { findText(it, localized.getString(R.string.save)) }
            save != null
        }
        val target = checkNotNull(save)
        assertTrue(target.isVisibleToUser)
        assertTrue(target.isClickable)
        assertTrue(target.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS))
        target.refresh()
        compose.waitUntil(5_000) { target.refresh() && target.isAccessibilityFocused }
        target.performAction(AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS)
    }

    private fun findText(
        node: AccessibilityNodeInfo,
        text: String,
    ): AccessibilityNodeInfo? {
        if (node.text?.toString() == text || node.contentDescription?.toString() == text) {
            return generateSequence(node) { it.parent }.firstOrNull { it.isClickable && it.isVisibleToUser }
        }
        repeat(node.childCount) { index ->
            node.getChild(index)?.let { child -> findText(child, text)?.let { return it } }
        }
        return null
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}-dark={1}")
        fun parameters(): List<Array<Any>> = localeThemeCases()
    }
}
