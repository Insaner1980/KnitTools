package com.finnvek.knittools.ui.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.finnvek.knittools.analytics.UsageAnalytics
import com.finnvek.knittools.repository.MemoryOnboardingPreferences
import com.finnvek.knittools.repository.OnboardingStateRepository
import com.finnvek.knittools.ui.tips.TipSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ProjectSetupNavigationTest {
    @get:Rule val compose = createComposeRule()
    private val scopeJob = SupervisorJob()
    private val scope = CoroutineScope(scopeJob + Dispatchers.Main.immediate)
    private lateinit var session: TipSession
    private lateinit var nav: NavHostController

    @Before
    fun setUp() =
        runBlocking {
            val repository = OnboardingStateRepository(MemoryOnboardingPreferences())
            repository.initializeIfNeeded(false, 1L)
            session = TipSession(repository, UsageAnalytics.NONE, scope)
        }

    @After
    fun tearDown() = runBlocking { scopeJob.cancelAndJoin() }

    @Test
    fun workTimeReusesCounterAndTargetsItsActualEntry() {
        render()
        compose.runOnIdle { nav.navigate(Screen.Counter.route) }
        val counterId = compose.runOnIdle { nav.currentBackStackEntry!!.id }
        compose.runOnIdle { actions().onProjectOverview(7L) }
        compose.runOnIdle { actions().onOpenCounterWithWorkSessionTip(7L) }
        compose.runOnIdle {
            assertEquals(counterId, nav.currentBackStackEntry?.id)
            assertEquals(Screen.ProjectList.route, nav.previousBackStackEntry?.destination?.route)
            assertEquals(
                counterId,
                session.state.value.workSessionRequest
                    ?.routeKey,
            )
            assertEquals(
                7L,
                session.state.value.workSessionRequest
                    ?.projectId,
            )
        }
    }

    @Test
    fun departedOrMismatchedOverviewCannotOpenWorkTime() {
        render()
        compose.runOnIdle { nav.navigate(Screen.ProjectOverview(7L).route) }
        val staleAction = compose.runOnIdle { actions().onOpenCounterWithWorkSessionTip }
        compose.runOnIdle { staleAction(8L) }
        compose.runOnIdle {
            assertEquals(Screen.ProjectOverview.ROUTE, nav.currentDestination?.route)
            assertNull(session.state.value.workSessionRequest)
            nav.popBackStack()
        }
        compose.runOnIdle { staleAction(7L) }
        compose.runOnIdle {
            assertEquals(Screen.ProjectList.route, nav.currentDestination?.route)
            assertNull(session.state.value.workSessionRequest)
        }
    }

    @Test
    fun returningToTheSameOverviewEntryStartsAnotherSetupVisit() {
        render()
        compose.runOnIdle { nav.navigate(Screen.ProjectOverview(7L).route) }
        val overviewId = compose.runOnIdle { nav.currentBackStackEntry!!.id }
        val originalVisit = compose.runOnIdle { session.state.value.routeEntryGeneration }
        compose.runOnIdle { actions().onOpenCounterWithWorkSessionTip(7L) }
        compose.runOnIdle { actions().onProjectOverview(7L) }
        compose.runOnIdle {
            assertEquals(overviewId, nav.currentBackStackEntry?.id)
            assertTrue(session.state.value.routeEntryGeneration > originalVisit)
            assertNull(session.state.value.workSessionRequest)
        }
    }

    private fun actions() = projectWorkspaceActions(nav, {}, tipSession = session)

    private fun render() {
        compose.setContent {
            nav = rememberNavController()
            val entry by nav.currentBackStackEntryAsState()
            LaunchedEffect(entry?.id) { session.onRouteChanged(entry?.id) }
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
