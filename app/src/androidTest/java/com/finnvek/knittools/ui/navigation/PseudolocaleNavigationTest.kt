package com.finnvek.knittools.ui.navigation

import android.app.LocaleConfig
import android.app.LocaleManager
import android.os.LocaleList
import android.view.View
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.App
import com.finnvek.knittools.MainActivity
import com.finnvek.knittools.R
import com.finnvek.knittools.data.datastore.AppLanguage
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.SavedPatternEntity
import com.finnvek.knittools.data.local.SessionEntity
import com.finnvek.knittools.pro.ProPolicyTestControl
import com.finnvek.knittools.ui.TestContentSplashRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/** Uses generated resources and the platform locale picker contract, never a fake Compose locale. */
@SdkSuppress(minSdkVersion = 33)
class PseudolocaleNavigationTest {
    @get:Rule(order = 0)
    val splash = TestContentSplashRule()

    @get:Rule(order = 1)
    val rule = createAndroidComposeRule<MainActivity>()

    private val app: App = ApplicationProvider.getApplicationContext()
    private var originalSize: String? = null
    private var originalLocales: LocaleList? = null
    private val projectName = "Neule ABC 123 — وشاح ٤٥٦"
    private val patternName = "Pattern 42 — وشاح ABC"
    private val patternUrl = "https://example.com/Pattern/ABC-123?size=4.5&lang=FI"

    @After
    fun restoreEnvironment() {
        originalLocales?.let { locales ->
            rule.runOnUiThread { app.getSystemService(LocaleManager::class.java).applicationLocales = locales }
            shell("wm size ${originalSize ?: "reset"}")
            ProPolicyTestControl.setPurchased(false)
        }
    }

    @Test
    fun expandedEnglishKeepsNavigationFormsListsAndChartUsable() = exerciseLocale("en-XA")

    @Test
    fun rtlKeepsNavigationFormsListsAndMixedDirectionValuesUsable() = exerciseLocale("ar-XB")

    private fun exerciseLocale(tag: String) {
        check(app.packageName == "com.finnvek.knittools.activepolicytest")
        check(InstrumentationRegistry.getArguments().getString("pseudoResizeAllowed") == "true")
        assertEquals(1f, rule.activity.resources.configuration.fontScale)
        val manager = app.getSystemService(LocaleManager::class.java)
        originalSize =
            shell("wm size")
                .lineSequence()
                .firstOrNull { it.startsWith("Override size:") }
                ?.substringAfter(":")
                ?.trim()
        originalLocales = manager.applicationLocales
        ProPolicyTestControl.setPurchased(true)
        runBlocking(Dispatchers.Main) { app.preferencesManager.get().setAppLanguage(AppLanguage.ENGLISH) }
        seed()
        val supported = checkNotNull(LocaleConfig(app).supportedLocales)
        assertTrue((0 until supported.size()).any { supported[it].toLanguageTag() == tag })
        assertFalse(AppLanguage.entries.any { it.languageTag == tag })
        rule.runOnUiThread { manager.applicationLocales = LocaleList.forLanguageTags(tag) }
        rule.waitUntil(15_000) {
            rule.activity.resources.configuration.locales[0]
                .toLanguageTag() == tag
        }
        assertEquals(tag, manager.applicationLocales.toLanguageTags())
        val expanded = rule.activity.getString(R.string.new_project)
        if (tag == "en-XA") {
            assertTrue(expanded, expanded.startsWith("[") && expanded.length > "New project".length)
        } else {
            assertEquals(View.LAYOUT_DIRECTION_RTL, rule.activity.resources.configuration.layoutDirection)
            assertTrue(expanded, expanded.contains('\u202e'))
        }
        for ((width, height) in listOf(360 to 800, 1280 to 800)) {
            resize(width, height)
            for (destination in TopLevelDestination.entries) {
                tab(destination).assertIsDisplayed().performClick()
                assertLabelFits(label(destination.labelRes).uppercase(rule.activity.resources.configuration.locales[0]))
            }
            assertNavigationDirection(tag, width)
            tab(TopLevelDestination.Projects).performClick()
            text(projectName).assertIsDisplayed()
            screenshot("$tag-$width-projects")
            text(R.string.new_project).performClick()
            rule.onAllNodes(hasSetTextAction())[0].performTextReplacement(projectName)
            Espresso.closeSoftKeyboard()
            text(R.string.cancel).performScrollTo().assertIsDisplayed()
            screenshot("$tag-$width-project-form")
            text(R.string.cancel).performClick()
            rule
                .onNodeWithContentDescription(
                    rule.activity.getString(R.string.project_continue_content_description, projectName),
                ).performClick()
            rule
                .onNodeWithContentDescription(label(R.string.counter_add_row), useUnmergedTree = true)
                .performScrollTo()
                .assertIsDisplayed()
                .performClick()
            rule.waitUntil(10_000) {
                runBlocking(Dispatchers.IO) {
                    app.database
                        .get()
                        .counterProjectDao()
                        .getProject(1)
                        ?.count == 1235
                }
            }
            screenshot("$tag-$width-counter")
            rule
                .onNodeWithContentDescription(label(R.string.counter_undo_last_change), useUnmergedTree = true)
                .performScrollTo()
                .performClick()
            back()

            tab(TopLevelDestination.Tools).performClick()
            text(R.string.tool_cast_on_calculator).performClick()
            rule.onAllNodes(hasSetTextAction())[0].performTextReplacement("12.5")
            Espresso.closeSoftKeyboard()
            tab(TopLevelDestination.Library).performClick()
            tab(TopLevelDestination.Tools).performClick()
            rule.onAllNodes(hasSetTextAction())[0].assertTextContains("12.5")
            tab(TopLevelDestination.Tools).performClick()
            rule.onAllNodes(hasSetTextAction())[0].assertTextContains("12.5")
            screenshot("$tag-$width-calculator")
            back()

            tab(TopLevelDestination.Library).performClick()
            text(R.string.saved_patterns_title).performClick()
            text(patternName).performClick()
            text(R.string.web_pattern_open_website).performScrollTo().assertIsDisplayed()
            screenshot("$tag-$width-pattern")
            back()
            text(R.string.web_pattern_add).performClick()
            rule.onNodeWithTag("web_pattern_title_field").performTextReplacement(patternName)
            rule.onNodeWithTag("web_pattern_url_field").performTextReplacement(patternUrl)
            Espresso.closeSoftKeyboard()
            rule.onNodeWithTag("web_pattern_url_field").assertTextContains(patternUrl)
            text(R.string.save).performScrollTo().assertIsDisplayed()
            screenshot("$tag-$width-web-form")
            text(R.string.cancel).performClick()
            back()

            tab(TopLevelDestination.Insights).performClick()
            rule.waitUntil(10_000) {
                rule.onAllNodes(hasTestTag("insights-list")).fetchSemanticsNodes().isNotEmpty()
            }
            rule.onNodeWithTag("insights-list").performScrollToKey("chart")
            rule.onNodeWithTag("insights-chart-plot", useUnmergedTree = true).assertIsDisplayed()
            screenshot("$tag-$width-chart")
            rule.activityRule.scenario.recreate()
            assertEquals(
                tag,
                rule.activity.resources.configuration.locales[0]
                    .toLanguageTag(),
            )
            tab(TopLevelDestination.Projects).performClick()
        }
        runBlocking(Dispatchers.IO) {
            assertEquals(
                1234,
                app.database
                    .get()
                    .counterProjectDao()
                    .getProject(1)
                    ?.count,
            )
            assertEquals(
                projectName,
                app.database
                    .get()
                    .counterProjectDao()
                    .getProject(1)
                    ?.name,
            )
        }
    }

    private fun seed() =
        runBlocking(Dispatchers.IO) {
            val db = app.database.get()
            db.clearAllTables()
            db.counterProjectDao().insert(CounterProjectEntity(id = 1, name = projectName, count = 1234))
            db.savedPatternDao().insert(
                SavedPatternEntity(
                    id = 1,
                    source = "WEB_LINK",
                    name = patternName,
                    designerName = "Synthetic designer",
                    originalUrl = patternUrl,
                    canonicalUrl = patternUrl,
                ),
            )
            val zone = ZoneId.systemDefault()
            for (daysAgo in 1L..3L) {
                val start =
                    LocalDate
                        .now(zone)
                        .minusDays(daysAgo)
                        .atTime(10, 0)
                        .atZone(zone)
                        .toInstant()
                        .toEpochMilli()
                db.sessionDao().insert(
                    SessionEntity(
                        projectId = 1,
                        startedAt = start,
                        endedAt = start + daysAgo * 600_000,
                        startRow = 1222,
                        endRow = 1234,
                        durationMinutes = (daysAgo * 10).toInt(),
                        zoneId = zone.id,
                    ),
                )
            }
        }

    private fun assertNavigationDirection(
        tag: String,
        width: Int,
    ) {
        val first = tab(TopLevelDestination.Projects).fetchSemanticsNode().boundsInRoot
        val last = tab(TopLevelDestination.Settings).fetchSemanticsNode().boundsInRoot
        if (width < 600) {
            assertEquals(tag == "ar-XB", first.center.x > last.center.x)
        } else {
            val rail = rule.onNodeWithTag("navigation-rail").fetchSemanticsNode().boundsInRoot
            assertEquals(tag == "ar-XB", rail.center.x > width * rule.density.density / 2)
        }
    }

    private fun assertLabelFits(value: String) {
        val layouts = mutableListOf<TextLayoutResult>()
        rule
            .onNode(hasText(value), useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertTrue(value, layouts.isNotEmpty())
        layouts.forEach {
            // Semantics retains paragraph coordinates, including the RTL origin offset.
            // Check the rendered line extent and ellipsis rather than unused paragraph space.
            val clipped =
                (0 until it.lineCount).any { line ->
                    it.isLineEllipsized(line) ||
                        it.getLineRight(line) - it.getLineLeft(line) > it.size.width + 1f ||
                        it.getLineBottom(line) > it.size.height + 1f
                }
            if (clipped) {
                screenshot("overflow-${rule.activity.resources.configuration.locales[0].toLanguageTag()}")
            }
            assertFalse(
                "Clipped navigation label: $value size=${it.size} constraints=${it.layoutInput.constraints} " +
                    "width=${it.didOverflowWidth} height=${it.didOverflowHeight} lines=${it.lineCount} " +
                    "ellipsis=${it.isLineEllipsized(0)} font=${it.layoutInput.style.fontSize} " +
                    "left=${it.getLineLeft(0)} right=${it.getLineRight(0)} bottom=${it.getLineBottom(0)}",
                clipped,
            )
        }
    }

    private fun resize(
        width: Int,
        height: Int,
    ) = rule.resizeWindow(width, height)

    private fun tab(destination: TopLevelDestination) =
        rule.onNode(
            isSelectable() and hasAnyDescendant(hasContentDescription(label(destination.labelRes))),
            useUnmergedTree = true,
        )

    private fun label(resource: Int) = rule.activity.getString(resource)

    private fun text(resource: Int) = text(label(resource))

    private fun text(value: String) = rule.findText(value)

    private fun back() = rule.onNodeWithContentDescription(label(R.string.back)).performClick()

    private fun screenshot(name: String) {
        rule.waitForIdle()
        captureScreenshot(app, "pseudolocale-screens", name)
    }
}
