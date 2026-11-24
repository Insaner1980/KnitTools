package com.finnvek.knittools.ui.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.State
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ProjectSetupNavigationTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var nav: NavHostController
    private lateinit var generation: State<Long>

    @Test
    fun workTimeReusesCounterAndTargetsItsActualEntry() {
        render()
        compose.runOnIdle {
            nav.navigate(Screen.Counter.route)
            nav.currentBackStackEntry!!.savedStateHandle[COUNTER_ENTRY_PROJECT_ID] = 7L
        }
        val counterId = compose.runOnIdle { nav.currentBackStackEntry!!.id }
        compose.runOnIdle { actions().onProjectOverview(7L) }
        compose.runOnIdle { actions().onOpenCounterWithWorkSessionActions(7L) }
        compose.runOnIdle {
            assertEquals(counterId, nav.currentBackStackEntry?.id)
            assertEquals(Screen.ProjectList.route, nav.previousBackStackEntry?.destination?.route)
            assertEquals(7L, nav.currentBackStackEntry?.savedStateHandle?.get<Long>(COUNTER_WORK_SESSION_PROJECT_ID))
        }
    }

    @Test
    fun departedOrMismatchedOverviewCannotOpenWorkTime() {
        render()
        compose.runOnIdle { nav.navigate(Screen.ProjectOverview(7L).route) }
        val staleAction = compose.runOnIdle { actions().onOpenCounterWithWorkSessionActions }
        compose.runOnIdle { staleAction(8L) }
        compose.runOnIdle {
            assertEquals(Screen.ProjectOverview.ROUTE, nav.currentDestination?.route)
            assertNull(nav.currentBackStackEntry?.savedStateHandle?.get<Long>(COUNTER_WORK_SESSION_PROJECT_ID))
            nav.popBackStack()
        }
        compose.runOnIdle { staleAction(7L) }
        compose.runOnIdle {
            assertEquals(Screen.ProjectList.route, nav.currentDestination?.route)
            assertNull(nav.currentBackStackEntry?.savedStateHandle?.get<Long>(COUNTER_WORK_SESSION_PROJECT_ID))
        }
    }

    @Test
    fun returningToTheSameOverviewEntryStartsAnotherSetupVisit() {
        render()
        compose.runOnIdle { nav.navigate(Screen.ProjectOverview(7L).route) }
        val overviewId = compose.runOnIdle { nav.currentBackStackEntry!!.id }
        val originalVisit = compose.runOnIdle { generation.value }
        compose.runOnIdle { actions().onOpenCounterWithWorkSessionActions(7L) }
        compose.runOnIdle { actions().onProjectOverview(7L) }
        compose.runOnIdle {
            assertEquals(overviewId, nav.currentBackStackEntry?.id)
            assertTrue(generation.value > originalVisit)
        }
    }

    private fun actions() = projectWorkspaceActions(nav, {})

    private fun render() {
        compose.setContent {
            nav = rememberNavController()
            generation = rememberProjectSetupVisitGeneration(nav)
            NavHost(nav, startDestination = Screen.ProjectList.route) {
                composable(Screen.ProjectList.route) { Text("Projects") }
                composable(Screen.Counter.route) { Text("Counter") }
                composable(
                    Screen.ProjectOverview.ROUTE,
                    arguments = listOf(navArgument("projectId") { type = NavType.LongType }),
                ) { Text("Overview") }
            }
        }
        compose.waitForIdle()
    }
}
