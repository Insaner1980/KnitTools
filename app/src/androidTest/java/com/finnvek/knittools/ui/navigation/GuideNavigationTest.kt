package com.finnvek.knittools.ui.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.finnvek.knittools.ui.screens.guide.GuideAction
import com.finnvek.knittools.ui.screens.guide.GuideTopic
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class GuideNavigationTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var nav: NavHostController
    private lateinit var scope: CoroutineScope
    private val counterLaunches = mutableListOf<Long>()
    private var visibleTopic: GuideTopic? = null

    @Test
    fun rootAndTopicsKeepTheEntryTabAndRoundTripEveryTopic() {
        render()
        listOf(TopLevelDestination.Projects, TopLevelDestination.Settings).forEach { tab ->
            openGuide(tab)
            compose.runOnIdle {
                assertEquals(tab.route, nav.currentDestination?.parent?.route)
                assertTrue(showsTopLevelNavigation(nav.currentDestination?.route))
            }
            GuideTopic.entries.forEach { topic ->
                compose.runOnIdle { nav.navigate(Screen.GuideTopic(topic.key).route) }
                compose.runOnIdle {
                    assertEquals(topic, visibleTopic)
                    assertEquals(
                        topic.key,
                        nav.currentBackStackEntry?.arguments?.getString(Screen.GuideTopic.ARG_TOPIC),
                    )
                    assertEquals(tab.route, nav.currentDestination?.parent?.route)
                    assertTrue(showsTopLevelNavigation(nav.currentDestination?.route))
                    nav.popBackStack()
                }
            }
        }
    }

    @Test
    fun unknownTopicReturnsToRootWithoutAnotherGuideEntry() {
        render()
        openGuide(TopLevelDestination.Settings)
        val rootId = compose.runOnIdle { nav.currentBackStackEntry!!.id }
        compose.runOnIdle { nav.navigate(Screen.GuideTopic("unknown").route) }
        compose.runOnIdle {
            assertEquals(Screen.Guide.route, nav.currentDestination?.route)
            assertEquals(rootId, nav.currentBackStackEntry?.id)
            assertEquals(Screen.Settings.route, nav.previousBackStackEntry?.destination?.route)
            assertNull(visibleTopic)
        }
    }

    @Test
    fun everyActionOpensItsExistingDestination() {
        render()
        val destinations =
            mapOf(
                GuideAction.OpenCounterForLatestProject to Screen.Guide.route,
                GuideAction.OpenLatestProjectPage to Screen.ProjectOverview.ROUTE,
                GuideAction.OpenTools to Screen.Tools.route,
                GuideAction.OpenLibrary to Screen.Library.route,
                GuideAction.OpenMyYarn to Screen.MyYarn.route,
                GuideAction.OpenSavedPatterns to Screen.SavedPatterns.route,
                GuideAction.OpenInsights to Screen.Insights.route,
                GuideAction.OpenBackup to Screen.Backup.route,
                GuideAction.OpenRavelry to Screen.RavelryBrowser.route,
            )
        assertEquals(GuideAction.entries.toSet(), destinations.keys)
        destinations.forEach { (action, route) ->
            openGuide(TopLevelDestination.Settings)
            act(action, projectId = 7L)
            compose.runOnIdle {
                assertEquals(route, nav.currentDestination?.route)
                if (action == GuideAction.OpenLatestProjectPage) {
                    assertEquals(7L, nav.currentBackStackEntry?.arguments?.getLong("projectId"))
                    assertEquals(TopLevelDestination.Projects.route, nav.currentDestination?.parent?.route)
                }
            }
        }
        assertEquals(listOf(7L), counterLaunches)
    }

    @Test
    fun missingProjectReturnsToProjectsFromEitherGuideGraph() {
        render()
        listOf(TopLevelDestination.Projects, TopLevelDestination.Settings).forEach { tab ->
            listOf(GuideAction.OpenCounterForLatestProject, GuideAction.OpenLatestProjectPage).forEach { action ->
                openGuide(tab)
                act(action, projectId = null)
                compose.runOnIdle { assertEquals(Screen.ProjectList.route, nav.currentDestination?.route) }
            }
        }
        assertTrue(counterLaunches.isEmpty())
    }

    @Test
    fun lateProjectResolutionCannotNavigateFromAReplacedGuideEntry() {
        render()
        openGuide(TopLevelDestination.Settings)
        val resolved = CompletableDeferred<Long?>()
        val origin = compose.runOnIdle { nav.currentBackStackEntry!!.id }
        compose.runOnIdle {
            scope.launch {
                nav.navigateFromGuide(
                    GuideAction.OpenCounterForLatestProject,
                    origin,
                    { resolved.await() },
                    counterLaunches::add,
                )
            }
        }
        compose.runOnIdle {
            nav.popBackStack()
            nav.navigate(Screen.Guide.route)
        }
        val replacement = compose.runOnIdle { nav.currentBackStackEntry!!.id }
        compose.runOnIdle { resolved.complete(7L) }
        compose.runOnIdle {
            assertNotEquals(origin, replacement)
            assertEquals(replacement, nav.currentBackStackEntry?.id)
            assertTrue(counterLaunches.isEmpty())
        }
    }

    @Test
    fun topLevelActionOpensRootAfterRestoringAnEarlierToolDetail() {
        render()
        compose.runOnIdle {
            nav.navigateToTopLevel(TopLevelDestination.Tools)
            nav.navigate(Screen.Gauge.route)
        }
        openGuide(TopLevelDestination.Settings)
        act(GuideAction.OpenTools)
        compose.runOnIdle { assertEquals(Screen.Tools.route, nav.currentDestination?.route) }
    }

    @Test
    fun projectWorkspaceEntryOpensGuideInProjects() {
        render()
        compose.runOnIdle {
            nav.navigate(Screen.ProjectOverview(7L).route)
            projectWorkspaceActions(nav, {}).onGuide()
        }
        compose.runOnIdle {
            assertEquals(Screen.Guide.route, nav.currentDestination?.route)
            assertEquals(TopLevelDestination.Projects.route, nav.currentDestination?.parent?.route)
        }
    }

    private fun openGuide(tab: TopLevelDestination) {
        compose.runOnIdle {
            nav.navigateToTopLevel(tab)
            nav.popBackStack(tab.startRoute, false)
            nav.navigate(Screen.Guide.route)
        }
        compose.waitForIdle()
    }

    private fun act(
        action: GuideAction,
        projectId: Long? = null,
    ) {
        compose.runOnIdle {
            val origin = nav.currentBackStackEntry!!.id
            scope.launch { nav.navigateFromGuide(action, origin, { projectId }, counterLaunches::add) }
        }
        compose.waitForIdle()
    }

    private fun render() {
        compose.setContent {
            nav = rememberNavController()
            // CPD-OFF: Navigointitestit tarvitsevat saman tuotannon välilehtihierarkian eri opastusreiteille.
            scope = rememberCoroutineScope()
            NavHost(nav, startDestination = TopLevelDestination.Projects.route) {
                TopLevelDestination.entries.forEach { tab ->
                    navigation(startDestination = tab.startRoute, route = tab.route) {
                        composable(tab.startRoute) { Text(tab.route) }
                        // CPD-ON
                        when (tab) {
                            TopLevelDestination.Projects, TopLevelDestination.Settings -> {
                                guideDestinations(nav, counterLaunches::add) { topic, _ ->
                                    visibleTopic = topic
                                    Text(topic?.key ?: "Guide")
                                }
                            }

                            TopLevelDestination.Library -> {
                                composable(Screen.MyYarn.route) { Text("Yarn") }
                                composable(Screen.SavedPatterns.route) { Text("Patterns") }
                            }

                            TopLevelDestination.Tools -> {
                                composable(Screen.Gauge.route) { Text("Gauge") }
                            }

                            TopLevelDestination.Insights -> {
                                Unit
                            }
                        }
                        // CPD-OFF: Projektisivun reitti on yhteinen navigointitestien välilehtirungoille.
                        if (tab == TopLevelDestination.Projects) {
                            composable(
                                Screen.ProjectOverview.ROUTE,
                                arguments = listOf(navArgument("projectId") { type = NavType.LongType }),
                            ) { Text("Overview") }
                        }
                        // CPD-ON
                        if (tab == TopLevelDestination.Settings) {
                            composable(Screen.Backup.route) { Text("Backup") }
                        }
                    }
                }
                composable(Screen.RavelryBrowser.route) { Text("Ravelry") }
            }
        }
        compose.waitForIdle()
    }
}
