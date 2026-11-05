package com.finnvek.knittools.ui.navigation

import android.graphics.Rect
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import androidx.test.espresso.Espresso
import androidx.window.layout.FoldingFeature
import androidx.window.testing.layout.FoldingFeature
import androidx.window.testing.layout.TestWindowLayoutInfo
import androidx.window.testing.layout.WindowLayoutInfoPublisherRule
import com.finnvek.knittools.ui.components.ToolScreenScaffold
import com.finnvek.knittools.ui.layout.AdaptiveLayoutSize
import com.finnvek.knittools.ui.layout.AdaptiveWidth
import com.finnvek.knittools.ui.layout.ContentWidthPolicy
import com.finnvek.knittools.ui.layout.LocalAdaptiveContentSize
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Real Compose/Navigation with synthetic destinations and WindowManager posture emissions. */
class AdaptiveNavigationTest {
    @get:Rule(order = 0)
    val windowInfo = WindowLayoutInfoPublisherRule()

    @get:Rule(order = 1)
    val rule = createAndroidComposeRule<ComponentActivity>()

    private var width by mutableStateOf(599)
    private var height by mutableStateOf(700)
    private var density by mutableStateOf(1f)
    private var fixedPixelSize by mutableStateOf(false)
    private var inset by mutableStateOf(0)
    private var rtl by mutableStateOf(false)
    private var dark by mutableStateOf(false)
    private lateinit var nav: NavHostController
    private lateinit var contentSize: AdaptiveLayoutSize
    private lateinit var projectOwner: ProjectOwner

    class ProjectOwner : ViewModel()

    @Composable
    private fun TestWindow(content: @Composable () -> Unit) {
        CompositionLocalProvider(
            LocalDensity provides Density(density),
            LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
        ) {
            KnitToolsTheme(isDarkTheme = dark) {
                Box(Modifier.fillMaxSize().consumeWindowInsets(WindowInsets.safeDrawing), Alignment.TopStart) {
                    Box(
                        Modifier
                            .requiredSize(
                                (if (fixedPixelSize) width / density else width.toFloat()).dp,
                                (if (fixedPixelSize) height / density else height.toFloat()).dp,
                            ).padding(inset.dp)
                            .testTag("test-window"),
                    ) { content() }
                }
            }
        }
    }

    private fun launchShell() {
        rule.setContent {
            TestWindow {
                nav = rememberNavController()
                val entry by nav.currentBackStackEntryAsState()
                KnitToolsNavigationScaffold({ nav }, showsTopLevelNavigation(entry?.destination?.route)) {
                    val size = LocalAdaptiveContentSize.current
                    SideEffect { contentSize = size }
                    NavHost(nav, startDestination = TopLevelDestination.Projects.route) {
                        TopLevelDestination.entries.forEach { destination ->
                            navigation(destination.startRoute, destination.route) {
                                composable(destination.startRoute) { Text(destination.route) }
                                if (destination == TopLevelDestination.Projects) {
                                    composable("adaptive-project-detail") {
                                        val owner =
                                            remember(it) { nav.getBackStackEntry(TopLevelDestination.Projects.route) }
                                        projectOwner = ViewModelProvider(owner)[ProjectOwner::class.java]
                                        var draft by rememberSaveable { mutableStateOf("") }
                                        Column {
                                            TextField(draft, { draft = it }, Modifier.testTag("draft"))
                                        }
                                    }
                                }
                            }
                        }
                        composable(Screen.RavelryBrowser.ROUTE) { Text("Media") }
                    }
                }
            }
        }
        rule.waitForIdle()
    }

    @Test
    fun breakpointsPreserveEntryDraftGraphOwnerAndRestoredTabStack() {
        launchShell()
        val controller = nav
        rule.runOnIdle { nav.navigate("adaptive-project-detail") }
        rule.onNodeWithTag("draft").performTextInput("Preserved draft")
        Espresso.closeSoftKeyboard()
        val entryId = rule.runOnIdle { nav.currentBackStackEntry?.id }
        val owner = projectOwner
        for (size in listOf(599, 600, 601, 839, 840, 841, 360, 1000)) {
            rule.runOnIdle { width = size }
            assertPresentation(rail = size >= 600)
            rule.onNodeWithTag("draft").assertTextContains("Preserved draft")
            tab(TopLevelDestination.Projects).assertIsSelected()
            rule.runOnIdle {
                assertSame(controller, nav)
                assertSame(owner, projectOwner)
                assertEquals(entryId, nav.currentBackStackEntry?.id)
                assertEquals(Screen.ProjectList.route, nav.previousBackStackEntry?.destination?.route)
                assertEquals(size >= 600, contentSize.width.value < size)
            }
        }
        selectTab(TopLevelDestination.Tools)
        rule.runOnIdle { width = 360 }
        selectTab(TopLevelDestination.Projects)
        rule.onNodeWithTag("draft").assertTextContains("Preserved draft")
        rule.runOnIdle {
            assertSame(owner, projectOwner)
            assertEquals(entryId, nav.currentBackStackEntry?.id)
            assertTrue(nav.popBackStack())
            assertEquals(Screen.ProjectList.route, nav.currentDestination?.route)
        }
    }

    @Test
    fun compactHeightInsetsDensityAndRtlKeepAllDestinationsReachable() {
        width = 900
        height = 480
        launchShell()
        for (isDark in listOf(false, true)) {
            rule.runOnIdle { dark = isDark }
            assertPresentation(rail = true)
            rule.runOnIdle { height = 479 }
            assertPresentation(rail = false)
            TopLevelDestination.entries.forEach(::selectTab)
            rule.runOnIdle {
                height = 700
                width = 600
                inset = 1
            }
            assertPresentation(rail = false)
            rule.runOnIdle { inset = 0 }
            assertPresentation(rail = true)
            rule.runOnIdle {
                fixedPixelSize = true
                density = 1.25f
            }
            assertPresentation(rail = false)
            rule.runOnIdle {
                assertEquals(480.dp, contentSize.width)
                fixedPixelSize = false
            }
            assertPresentation(rail = true)
            rule.runOnIdle {
                rtl = true
            }
            assertPresentation(rail = true)
            val rail = rule.onNodeWithTag("navigation-rail").fetchSemanticsNode().boundsInWindow
            val content = rule.onNodeWithTag("adaptive-content").fetchSemanticsNode().boundsInWindow
            assertTrue("Rail is at physical right in RTL", rail.left >= content.right)
            rule.runOnIdle {
                assertEquals(content.width / density, contentSize.width.value, 1f)
                density = 1f
                rtl = false
                height = 480
                width = 900
            }
        }
    }

    @Test
    fun foldingEmissionsConstrainOneHostAndUnfoldWithoutLosingItsEntry() {
        width = 1200
        height = 800
        launchShell()
        rule.runOnIdle { nav.navigate("adaptive-project-detail") }
        rule.onNodeWithTag("draft").performTextInput("Fold draft")
        Espresso.closeSoftKeyboard()
        val entryId = rule.runOnIdle { nav.currentBackStackEntry?.id }
        val window = rule.onNodeWithTag("test-window").fetchSemanticsNode().boundsInWindow
        val bounds = Rect(window.left.toInt(), window.top.toInt(), window.right.toInt(), window.bottom.toInt())
        val vertical = FoldingFeature(bounds, size = 20, orientation = FoldingFeature.Orientation.VERTICAL)
        emitFold(vertical)
        assertPresentation(rail = false)
        assertOutside(vertical)
        rule.onNodeWithTag("draft").assertTextContains("Fold draft")

        val horizontal = FoldingFeature(bounds, orientation = FoldingFeature.Orientation.HORIZONTAL)
        emitFold(horizontal)
        assertPresentation(rail = false)
        assertOutside(horizontal)
        rule.onNodeWithTag("draft").assertTextContains("Fold draft")

        val crease =
            FoldingFeature(
                bounds,
                state = FoldingFeature.State.FLAT,
                orientation = FoldingFeature.Orientation.VERTICAL,
            )
        emitFold(crease)
        assertPresentation(rail = true)
        rule.runOnIdle { assertEquals(entryId, nav.currentBackStackEntry?.id) }
        emitFold(null)
        assertPresentation(rail = true)
        rule.onNodeWithTag("draft").assertTextContains("Fold draft")

        rule.runOnIdle { nav.navigate(Screen.RavelryBrowser.route) }
        rule.onNodeWithTag("navigation-rail").assertDoesNotExist()
        rule.onNodeWithTag("navigation-bottom-bar").assertDoesNotExist()
        rule.runOnIdle { width = 360 }
        rule.onNodeWithTag("navigation-bottom-bar").assertDoesNotExist()
    }

    @Test
    fun toolScaffoldKeepsFormDefaultAndExplicitWideContentPolicies() {
        width = 1600
        height = 600
        var policy by mutableStateOf(ContentWidthPolicy.Form)
        rule.setContent {
            TestWindow {
                ToolScreenScaffold("Test", {}, contentWidthPolicy = policy) {
                    val size = LocalAdaptiveContentSize.current
                    SideEffect { contentSize = size }
                    Box(Modifier.fillMaxSize().testTag("tool-body"))
                }
            }
        }
        for ((nextPolicy, expected) in listOf(
            ContentWidthPolicy.Form to 600,
            ContentWidthPolicy.ListDetail to 840,
            ContentWidthPolicy.Chart to 1200,
            ContentWidthPolicy.Unrestricted to 1600,
        )) {
            rule.runOnIdle { policy = nextPolicy }
            val body = rule.onNodeWithTag("tool-body").fetchSemanticsNode().size
            assertEquals(expected, body.width)
            rule.runOnIdle { assertEquals(expected.dp, contentSize.width) }
        }
        rule.runOnIdle { width = 411 }
        rule.runOnIdle {
            assertEquals(411.dp, contentSize.width)
            assertEquals(AdaptiveWidth.Compact, contentSize.widthClass)
        }
    }

    private fun selectTab(destination: TopLevelDestination) {
        tab(destination).assertIsDisplayed().performClick()
        tab(destination).assertIsSelected()
        rule.runOnIdle { assertEquals(destination.route, nav.currentDestination?.parent?.route) }
    }

    private fun tab(destination: TopLevelDestination) =
        rule.onNode(
            isSelectable() and hasAnyDescendant(hasText(rule.activity.getString(destination.labelRes).uppercase())),
            useUnmergedTree = true,
        )

    private fun assertPresentation(rail: Boolean) {
        println(
            "ADAPTIVE presentation rail=$rail requested=${width}x$height density=$density " +
                "window=${rule.onNodeWithTag("test-window").fetchSemanticsNode().boundsInWindow} " +
                "content=${rule.onNodeWithTag("adaptive-content").fetchSemanticsNode().boundsInWindow}",
        )
        rule.onNodeWithTag(if (rail) "navigation-rail" else "navigation-bottom-bar").assertIsDisplayed()
        rule.onNodeWithTag(if (rail) "navigation-bottom-bar" else "navigation-rail").assertDoesNotExist()
        TopLevelDestination.entries.forEach {
            val item = tab(it)
            println("ADAPTIVE ${it.route} bounds=${item.fetchSemanticsNode().boundsInWindow}")
            item.assertIsDisplayed()
        }
    }

    private fun emitFold(feature: FoldingFeature?) {
        rule.runOnIdle { windowInfo.overrideWindowLayoutInfo(TestWindowLayoutInfo(listOfNotNull(feature))) }
        rule.waitForIdle()
    }

    private fun assertOutside(feature: FoldingFeature) {
        val hinge = feature.bounds
        for (tag in listOf("adaptive-content", "navigation-bottom-bar")) {
            val rect = rule.onNodeWithTag(tag).fetchSemanticsNode().boundsInWindow
            assertTrue(
                "$tag overlaps $hinge: $rect",
                rect.right <= hinge.left ||
                    rect.left >= hinge.right ||
                    rect.bottom <= hinge.top ||
                    rect.top >= hinge.bottom,
            )
        }
    }
}
