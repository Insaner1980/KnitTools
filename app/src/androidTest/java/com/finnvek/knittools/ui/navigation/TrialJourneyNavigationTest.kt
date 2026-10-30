package com.finnvek.knittools.ui.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.pro.TrialJourneyAction
import com.finnvek.knittools.pro.TrialJourneyFacts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.UUID

class TrialJourneyNavigationTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var nav: NavHostController
    private val launches = mutableListOf<CounterLaunchRequest>()

    @Test
    fun createAndChartRequestsTargetTheRootAfterRestoringTabDetails() {
        render()
        val destinations =
            listOf(
                Triple(TrialJourneyAction.AnotherProject, TopLevelDestination.Projects, TRIAL_CREATE_PROJECT_REQUEST),
                Triple(TrialJourneyAction.WeeklyChart, TopLevelDestination.Insights, TRIAL_WEEKLY_CHART_REQUEST),
            )
        destinations.forEach { (action, destination, requestKey) ->
            compose.runOnIdle {
                nav.navigateToTopLevel(destination)
                nav.navigate(
                    if (destination ==
                        TopLevelDestination.Projects
                    ) {
                        Screen.ProjectOverview(7L).route
                    } else {
                        INSIGHTS_DETAIL
                    },
                )
            }
            val detailState = compose.runOnIdle { nav.currentBackStackEntry!!.savedStateHandle }
            compose.runOnIdle { nav.navigateToTopLevel(TopLevelDestination.Settings) }
            compose.runOnIdle { nav.navigateFromTrialJourney(action, facts(), launches::add) }
            val firstRequest =
                compose.runOnIdle {
                    assertEquals(destination.startRoute, nav.currentDestination?.route)
                    assertEquals(destination.route, nav.currentDestination?.parent?.route)
                    assertNull(detailState.get<String>(requestKey))
                    val state = nav.currentBackStackEntry!!.savedStateHandle
                    val request = state.get<String>(requestKey)
                    assertNotNull(request)
                    assertEquals(request, UUID.fromString(request).toString())
                    val otherKey =
                        if (action == TrialJourneyAction.AnotherProject) {
                            TRIAL_WEEKLY_CHART_REQUEST
                        } else {
                            TRIAL_CREATE_PROJECT_REQUEST
                        }
                    assertNull(state.get<String>(otherKey))
                    request
                }
            compose.runOnIdle { nav.navigateFromTrialJourney(action, facts(), launches::add) }
            compose.runOnIdle {
                assertNotEquals(firstRequest, nav.currentBackStackEntry!!.savedStateHandle.get<String>(requestKey))
            }
        }
        assertTrue(launches.isEmpty())
    }

    @Test
    fun counterActionsKeepTheLatestProjectAndExactAction() {
        render()
        compose.runOnIdle { nav.navigateToTopLevel(TopLevelDestination.Settings) }
        counterActions.forEach { action ->
            compose.runOnIdle { nav.navigateFromTrialJourney(action, facts(CounterProject(id = 42L)), launches::add) }
        }
        compose.runOnIdle {
            assertEquals(Screen.Settings.route, nav.currentDestination?.route)
            assertEquals(counterActions, launches.map { it.journeyAction })
            assertEquals(listOf(42L, 42L, 42L), launches.map { it.projectId })
            assertEquals(3, launches.map { it.requestId }.toSet().size)
            launches.forEach { assertEquals(it.requestId, UUID.fromString(it.requestId).toString()) }
        }
    }

    @Test
    fun counterActionsWithoutAnActiveProjectRequestCreationInstead() {
        render()
        listOf(null, facts()).forEach { missingProjectFacts ->
            counterActions.forEach { action ->
                compose.runOnIdle { nav.navigateToTopLevel(TopLevelDestination.Settings) }
                compose.runOnIdle { nav.navigateFromTrialJourney(action, missingProjectFacts, launches::add) }
                compose.runOnIdle {
                    assertEquals(Screen.ProjectList.route, nav.currentDestination?.route)
                    assertNotNull(
                        nav.currentBackStackEntry!!.savedStateHandle.get<String>(TRIAL_CREATE_PROJECT_REQUEST),
                    )
                    assertTrue(launches.isEmpty())
                }
            }
        }
    }

    @Test
    fun counterRequestSaverRoundTripsOnlyPrimitiveFields() {
        val scope =
            object : SaverScope {
                override fun canBeSaved(value: Any) = value is String || value is Long
            }
        val requests =
            counterActions.map { action ->
                CounterLaunchRequest(requestId = "request-${action.name}", projectId = 42L, journeyAction = action)
            } + CounterLaunchRequest(requestId = "plain-request")
        requests.forEach { request ->
            val saved = with(CounterLaunchRequestSaver) { scope.save(request) }
            assertNotNull(saved)
            val fields = saved as List<*>
            assertEquals(3, fields.size)
            assertTrue(fields.all { it is String || it is Long })
            assertEquals(request, CounterLaunchRequestSaver.restore(saved))
        }
        assertNull(CounterLaunchRequestSaver.restore(emptyList<Any>()))
    }

    private fun render() {
        compose.setContent {
            nav = rememberNavController()
            NavHost(nav, startDestination = TopLevelDestination.Projects.route) {
                TopLevelDestination.entries.forEach { tab ->
                    navigation(startDestination = tab.startRoute, route = tab.route) {
                        composable(tab.startRoute) { Text(tab.route) }
                        if (tab == TopLevelDestination.Projects) {
                            composable(
                                Screen.ProjectOverview.ROUTE,
                                arguments = listOf(navArgument("projectId") { type = NavType.LongType }),
                            ) { Text("Overview") }
                        }
                        if (tab == TopLevelDestination.Insights) {
                            composable(INSIGHTS_DETAIL) { Text("Insights detail") }
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    private fun facts(project: CounterProject? = null) =
        TrialJourneyFacts(
            trialStartTimestamp = 1L,
            activeProjectCount = if (project == null) 0 else 1,
            latestActiveProject = project,
            projectsAdded = 0,
            countersAdded = 0,
            yarnCardsAdded = 0,
            photosAdded = 0,
            repeatCounterAdded = false,
            chartSeen = false,
            photoPdfCreated = false,
        )

    private val counterActions =
        listOf(
            TrialJourneyAction.Widget,
            TrialJourneyAction.RepeatCounter,
            TrialJourneyAction.PatternCamera,
        )

    private companion object {
        const val INSIGHTS_DETAIL = "trial_insights_detail"
    }
}
