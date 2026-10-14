package com.finnvek.knittools.ui.navigation

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ProjectOverviewNavigationTest {
    @get:Rule val rule = createComposeRule()
    private lateinit var nav: NavHostController

    private fun setup() {
        rule.setContent {
            nav = rememberNavController()
            NavHost(nav, startDestination = Screen.ProjectList.route) {
                composable(Screen.ProjectList.route) { Text("Projects") }
                composable(Screen.Counter.route) { Text("Counter") }
                composable(
                    Screen.PatternViewer.ROUTE,
                    arguments =
                        listOf(
                            navArgument("projectId") { type = NavType.LongType },
                            navArgument("selectedProjectDocumentId") {
                                type = NavType.LongType
                                defaultValue = 0L
                            },
                        ),
                ) { Text("Pattern") }
                composable(
                    Screen.ProjectOverview.ROUTE,
                    arguments =
                        listOf(
                            navArgument("projectId") {
                                type =
                                    NavType.LongType
                            },
                        ),
                ) { Text("Overview") }
            }
        }
    }

    @Test
    fun delayedDocumentSelectionCannotReopenDepartedWorkspace() {
        setup()
        rule.runOnIdle { nav.navigate(Screen.Counter.route) }
        rule.waitForIdle()
        val delayedAction = rule.runOnIdle { projectWorkspaceActions(nav, {}).onPatternViewer }
        rule.runOnIdle { nav.popBackStack() }
        rule.waitForIdle()
        rule.runOnIdle { delayedAction(7L, 41L) }
        rule.waitForIdle()
        rule.runOnIdle { assertEquals(Screen.ProjectList.route, nav.currentDestination?.route) }

        rule.runOnIdle { nav.navigate(Screen.Counter.route) }
        rule.waitForIdle()
        rule.runOnIdle { projectWorkspaceActions(nav, {}).onPatternViewer(7L, 41L) }
        rule.waitForIdle()
        rule.runOnIdle {
            assertEquals(Screen.PatternViewer.ROUTE, nav.currentDestination?.route)
            assertEquals(7L, nav.currentBackStackEntry?.arguments?.getLong("projectId"))
            assertEquals(41L, nav.currentBackStackEntry?.arguments?.getLong("selectedProjectDocumentId"))
        }
    }

    @Test
    fun closingProjectFromOverviewRemovesItsCounterFromBackStack() {
        setup()
        openOverviewFromCounter()
        closeProjectAndAssertList()
    }

    @Test
    fun closingProjectFromCounterRemovesItsOverviewFromBackStack() {
        setup()
        rule.runOnIdle { nav.navigate(Screen.ProjectOverview(7).route) }
        rule.waitForIdle()
        rule.runOnIdle { projectWorkspaceActions(nav, {}).onOpenCounter() }
        rule.waitForIdle()
        closeProjectAndAssertList()
    }

    @Test
    fun overviewCounterRoundTripReusesOverview() {
        setup()
        roundTripThroughCounter(7)
        rule.runOnIdle {
            assertEquals(Screen.ProjectOverview.ROUTE, nav.currentDestination?.route)
            assertEquals(Screen.ProjectList.route, nav.previousBackStackEntry?.destination?.route)
            assertTrue(nav.popBackStack())
            assertEquals(Screen.ProjectList.route, nav.currentDestination?.route)
        }
    }

    @Test
    fun counterOverviewRoundTripReusesCounter() {
        setup()
        openOverviewFromCounter()
        rule.runOnIdle { projectWorkspaceActions(nav, {}).onOpenCounter() }
        rule.waitForIdle()
        rule.runOnIdle {
            assertEquals(Screen.Counter.route, nav.currentDestination?.route)
            assertEquals(Screen.ProjectList.route, nav.previousBackStackEntry?.destination?.route)
        }
    }

    @Test
    fun overviewOfAnotherProjectIsNotReused() {
        setup()
        roundTripThroughCounter(6, 7)
        rule.runOnIdle { assertEquals(7L, nav.currentBackStackEntry?.arguments?.getLong("projectId")) }
    }

    private fun roundTripThroughCounter(
        initialProjectId: Long,
        finalProjectId: Long = initialProjectId,
    ) {
        rule.runOnIdle { nav.navigate(Screen.ProjectOverview(initialProjectId).route) }
        rule.waitForIdle()
        rule.runOnIdle { projectWorkspaceActions(nav, {}).onOpenCounter() }
        rule.waitForIdle()
        rule.runOnIdle { projectWorkspaceActions(nav, {}).onProjectOverview(finalProjectId) }
        rule.waitForIdle()
    }

    private fun openOverviewFromCounter() {
        rule.runOnIdle { nav.navigate(Screen.Counter.route) }
        rule.waitForIdle()
        rule.runOnIdle { projectWorkspaceActions(nav, {}).onProjectOverview(7) }
        rule.waitForIdle()
    }

    private fun closeProjectAndAssertList() {
        rule.runOnIdle { projectWorkspaceActions(nav, {}).onProjectClosed() }
        rule.waitForIdle()
        rule.runOnIdle {
            assertEquals(Screen.ProjectList.route, nav.currentDestination?.route)
            assertEquals(null, nav.previousBackStackEntry)
        }
    }
}
