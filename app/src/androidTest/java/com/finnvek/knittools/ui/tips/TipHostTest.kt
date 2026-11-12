package com.finnvek.knittools.ui.tips

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.R
import com.finnvek.knittools.analytics.UsageAnalytics
import com.finnvek.knittools.domain.tips.TipContext
import com.finnvek.knittools.domain.tips.TipId
import com.finnvek.knittools.domain.tips.TipScreen
import com.finnvek.knittools.domain.tips.TipSignal
import com.finnvek.knittools.repository.MemoryOnboardingPreferences
import com.finnvek.knittools.repository.OnboardingStateRepository
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class TipHostTest {
    @get:Rule
    val compose = createComposeRule()

    private val scopeJob = SupervisorJob()
    private val scope = CoroutineScope(scopeJob + Dispatchers.Main.immediate)
    private lateinit var session: TipSession
    private val resources get() = InstrumentationRegistry.getInstrumentation().targetContext.resources

    @Before
    fun setUp() =
        runBlocking {
            val repository = OnboardingStateRepository(MemoryOnboardingPreferences())
            repository.initializeIfNeeded(hasAnyProject = false, today = 0L)
            session = TipSession(repository, UsageAnalytics.NONE, scope)
            session.onRouteChanged(ROUTE)
        }

    @After
    fun tearDown() =
        runBlocking {
            scopeJob.cancelAndJoin()
        }

    @Test
    fun counterOpensWaitForLoadedFactsAndDoNotCountRecomposition() {
        val ready = mutableStateOf(false)
        showHost(context = {
            TipContext(TipScreen.COUNTER, projectId = 7L, screenReady = ready.value, blocked = true)
        }) {
            Text("Counter")
        }
        compose.waitUntil(5_000) { session.state.value.loaded }
        compose.runOnIdle {
            assertEquals(0, session.state.value.onboarding.counterOpenCount)
            ready.value = true
        }
        compose.waitUntil(5_000) { session.state.value.onboarding.counterOpenCount == 1 }
        compose.runOnIdle { ready.value = false }
        compose.runOnIdle { ready.value = true }
        compose.runOnIdle {
            assertEquals(1, session.state.value.onboarding.counterOpenCount)
            session.onRouteChanged("elsewhere")
        }
        compose.runOnIdle { session.onRouteChanged(ROUTE) }
        compose.waitUntil(5_000) { session.state.value.onboarding.counterOpenCount == 2 }
    }

    @Test
    fun widgetActionWaitsForReadyUnblockedHostAndRunsOnce() {
        compose.mainClock.autoAdvance = false
        val blocked = mutableStateOf(true)
        val ready = mutableStateOf(false)
        val actions = mutableListOf<TipId>()
        showHost(
            context = {
                TipContext(
                    TipScreen.COUNTER,
                    projectId = 7L,
                    hasSessions = true,
                    widgetPinSupported = ready.value,
                    blocked = blocked.value,
                    tipsSeen = TipId.entries.toSet() - TipId.WIDGET,
                )
            },
            onAction = { actions += it },
        ) {
            Box(Modifier.fillMaxSize()) {
                Button(onClick = {}, modifier = Modifier.align(Alignment.Center).tipAnchor(TipId.WIDGET)) {
                    Text("Project actions")
                }
            }
        }
        compose.waitUntil(5_000) { session.state.value.loaded }
        compose.mainClock.advanceTimeBy(2_500)
        compose.waitForIdle()
        val action = compose.onNodeWithText(resources.getString(R.string.tip_widget_action))
        action.assertDoesNotExist()
        compose.runOnIdle { blocked.value = false }
        advanceManualTipFrames()
        action.assertDoesNotExist()
        assertTrue(actions.isEmpty())
        compose.runOnIdle { ready.value = true }
        waitForTip(TipId.WIDGET)
        advanceManualTipFrames()
        action.assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertEquals(listOf(TipId.WIDGET), actions)
            assertTrue(TipId.WIDGET in session.state.value.processSeen)
        }
        advanceManualTipFrames()
        action.assertDoesNotExist()
    }

    @Test
    fun targetCelebrationWaitsForUnblockedVisiblePlacement() {
        compose.mainClock.autoAdvance = false
        val blocked = mutableStateOf(true)
        val preventPlacement = mutableStateOf(true)
        session.onSignal(TipSignal.TargetReached(7L))
        showHost(context = {
            TipContext(TipScreen.COUNTER, projectId = 7L, count = 20, target = 20, blocked = blocked.value)
        }) {
            Box(Modifier.fillMaxSize().then(if (preventPlacement.value) Modifier.tipProtected() else Modifier)) {
                Button(onClick = {}, modifier = Modifier.align(Alignment.Center).tipAnchor(TipId.TARGET_REACHED)) {
                    Text("Current count")
                }
            }
        }
        compose.waitUntil(5_000) { session.state.value.loaded }
        compose.mainClock.advanceTimeBy(2_500)
        compose.waitForIdle()
        assertTrue(
            session.state.value.onboarding.targetCelebratedProjects
                .isEmpty(),
        )
        compose.runOnIdle { blocked.value = false }
        advanceManualTipFrames()
        assertTrue(
            session.state.value.onboarding.targetCelebratedProjects
                .isEmpty(),
        )
        compose.runOnIdle { preventPlacement.value = false }
        waitForTip(TipId.TARGET_REACHED)
        advanceManualTipFrames()
        bubble(R.string.tip_target_reached).assertIsDisplayed()
        compose.waitUntil(5_000) { "7" in session.state.value.onboarding.targetCelebratedProjects }
    }

    @Test
    fun bubbleUsesFreeSpaceBelowWhenAreaAboveIsProtected() {
        showHost {
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.5f)
                        .tipProtected()
                        .testTag("protected"),
                )
                Button(
                    onClick = {},
                    modifier = Modifier.align(Alignment.Center).tipAnchor(TipId.TOOLS_INTRO).testTag("anchor"),
                ) { Text("Target") }
            }
        }
        waitForTip()
        val bubble = bubble().fetchSemanticsNode().boundsInRoot
        val anchor = compose.onNodeWithTag("anchor").fetchSemanticsNode().boundsInRoot
        val protectedBounds = compose.onNodeWithTag("protected").fetchSemanticsNode().boundsInRoot
        assertTrue(bubble.top >= anchor.bottom)
        assertFalse(bubble.overlaps(anchor))
        assertFalse(bubble.overlaps(protectedBounds))
    }

    @Test
    fun outsideTapDismissesAndStillClicksUnderlyingButton() {
        var clicks = 0
        showHost {
            Box(Modifier.fillMaxSize()) {
                Button(
                    onClick = {},
                    modifier = Modifier.align(Alignment.Center).tipAnchor(TipId.TOOLS_INTRO),
                ) { Text("Target") }
                Button(onClick = { clicks++ }, modifier = Modifier.align(Alignment.BottomCenter)) {
                    Text("Underlying action")
                }
            }
        }
        waitForTip()
        compose.onNodeWithText("Underlying action").performTouchInput { click() }
        compose.runOnIdle {
            assertEquals(1, clicks)
            assertEquals(null, session.state.value.presentation)
            assertTrue(TipId.TOOLS_INTRO in session.state.value.processSeen)
        }
    }

    @Test
    fun bubbleAnnouncesItsPaneWithoutTakingFocusAndKeepsDismissButtonSeparate() {
        showHost {
            Box(Modifier.fillMaxSize()) {
                Button(
                    onClick = {},
                    modifier = Modifier.align(Alignment.Center).tipAnchor(TipId.TOOLS_INTRO),
                ) { Text("Target") }
            }
        }
        waitForTip()
        bubble()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Focused, false))
        compose.onNodeWithText(resources.getString(R.string.tip_got_it)).assertHasClickAction()
    }

    @Test
    fun impossiblePlacementDoesNotUseBudgetOrMarkTipSeen() {
        showHost {
            Box(Modifier.fillMaxSize().tipProtected()) {
                Button(
                    onClick = {},
                    modifier = Modifier.align(Alignment.Center).tipAnchor(TipId.TOOLS_INTRO),
                ) { Text("Target") }
            }
        }
        compose.mainClock.advanceTimeBy(2_500)
        compose.waitForIdle()
        bubble().assertDoesNotExist()
        compose.onNodeWithText(resources.getString(R.string.tip_tools_intro)).assertDoesNotExist()
        compose.onNodeWithText(resources.getString(R.string.tip_got_it)).assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(0, session.state.value.shownThisProcess)
            assertTrue(
                session.state.value.processSeen
                    .isEmpty(),
            )
        }
    }

    @Test
    fun focusedInputBlocksTipEvenWithoutAnOnScreenKeyboard() {
        val requester = FocusRequester()
        lateinit var focusManager: FocusManager
        showHost {
            focusManager = LocalFocusManager.current
            LaunchedEffect(Unit) { requester.requestFocus() }
            Box(Modifier.fillMaxSize()) {
                Button(
                    onClick = {},
                    modifier = Modifier.align(Alignment.Center).tipAnchor(TipId.TOOLS_INTRO),
                ) { Text("Target") }
                Text(
                    "Focused field",
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .focusRequester(requester)
                            .tipBlockWhenFocused()
                            .focusable(),
                )
            }
        }
        compose.mainClock.advanceTimeBy(2_500)
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(0, session.state.value.shownThisProcess) }
        compose.runOnIdle { focusManager.clearFocus() }
        waitForTip()
        compose.runOnIdle { assertEquals(1, session.state.value.shownThisProcess) }
    }

    @Test
    fun switchingProjectsWithinTheSameRouteDismissesTheOldProjectsTip() {
        val selectedProject = mutableLongStateOf(1L)
        showHost(context = { TipContext(TipScreen.OVERVIEW, projectId = selectedProject.longValue) }) {
            Box(Modifier.fillMaxSize()) {
                Button(
                    onClick = {},
                    modifier = Modifier.align(Alignment.Center).tipAnchor(TipId.OVERVIEW_INTRO),
                ) { Text("Continue") }
            }
        }
        waitForTip(TipId.OVERVIEW_INTRO)
        compose.runOnIdle {
            assertEquals(
                1L,
                session.state.value.presentation
                    ?.projectId,
            )
            selectedProject.longValue = 2L
        }
        bubble(R.string.tip_overview_intro).assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(null, session.state.value.presentation)
            assertTrue(TipId.OVERVIEW_INTRO in session.state.value.processSeen)
            assertEquals(1, session.state.value.shownThisProcess)
        }
    }

    @Test
    fun requestedWorkSessionAppearsBeforeEntryDelayAndStaysVisibleWithoutUsingBudget() {
        compose.mainClock.autoAdvance = false
        val enteredAt = compose.mainClock.currentTime
        session.requestWorkSession(7L, ROUTE)
        showHost(context = { TipContext(TipScreen.COUNTER, projectId = 7L) }) {
            WorkSessionAnchor()
        }
        compose.waitUntil(5_000) { session.state.value.loaded }
        advanceManualTipFrames()
        bubble(R.string.tip_work_session).assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(
                TipId.WORK_SESSION,
                session.state.value.presentation
                    ?.id,
            )
            assertEquals(0, session.state.value.shownThisProcess)
            assertTrue(
                session.state.value.processSeen
                    .isEmpty(),
            )
        }
        advanceManualTipFrames()
        bubble(R.string.tip_work_session).assertIsDisplayed()
        assertTrue(compose.mainClock.currentTime - enteredAt < 2_000L)
    }

    @Test
    fun requestedWorkSessionWaitsForLoadedUnblockedContextAndValidPlacement() {
        compose.mainClock.autoAdvance = false
        val ready = mutableStateOf(false)
        val blocked = mutableStateOf(true)
        val preventPlacement = mutableStateOf(true)
        session.requestWorkSession(7L, ROUTE)
        showHost(context = {
            TipContext(TipScreen.COUNTER, projectId = 7L, screenReady = ready.value, blocked = blocked.value)
        }) {
            WorkSessionAnchor(preventPlacement = preventPlacement.value)
        }
        compose.waitUntil(5_000) { session.state.value.loaded }
        advanceManualTipFrames()
        assertWorkSessionRequestWaiting()
        compose.runOnIdle { ready.value = true }
        advanceManualTipFrames()
        assertWorkSessionRequestWaiting()
        compose.runOnIdle { blocked.value = false }
        advanceManualTipFrames()
        assertWorkSessionRequestWaiting()
        compose.runOnIdle { preventPlacement.value = false }
        advanceManualTipFrames()
        bubble(R.string.tip_work_session).assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, session.state.value.shownThisProcess) }
    }

    @Test
    fun changingLoadedProjectCancelsRequestedWorkSessionBeforePlacementBecomesAvailable() {
        compose.mainClock.autoAdvance = false
        val projectId = mutableLongStateOf(7L)
        val preventPlacement = mutableStateOf(true)
        session.requestWorkSession(7L, ROUTE)
        showHost(context = { TipContext(TipScreen.COUNTER, projectId = projectId.longValue) }) {
            WorkSessionAnchor(preventPlacement = preventPlacement.value)
        }
        compose.waitUntil(5_000) { session.state.value.loaded }
        advanceManualTipFrames()
        assertWorkSessionRequestWaiting()
        compose.runOnIdle { projectId.longValue = 8L }
        advanceManualTipFrames()
        compose.runOnIdle {
            assertEquals(null, session.state.value.workSessionRequest)
            preventPlacement.value = false
        }
        advanceManualTipFrames()
        bubble(R.string.tip_work_session).assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(null, session.state.value.presentation)
            assertEquals(0, session.state.value.shownThisProcess)
            assertTrue(
                session.state.value.processSeen
                    .isEmpty(),
            )
        }
    }

    @Composable
    private fun WorkSessionAnchor(preventPlacement: Boolean = false) {
        Box(Modifier.fillMaxSize().then(if (preventPlacement) Modifier.tipProtected() else Modifier)) {
            Button(
                onClick = {},
                modifier = Modifier.align(Alignment.Center).tipAnchor(TipId.WORK_SESSION),
            ) { Text("Project actions") }
        }
    }

    private fun advanceManualTipFrames() {
        repeat(5) {
            compose.mainClock.advanceTimeBy(100)
            compose.waitForIdle()
        }
    }

    private fun assertWorkSessionRequestWaiting() {
        bubble(R.string.tip_work_session).assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(
                7L,
                session.state.value.workSessionRequest
                    ?.projectId,
            )
            assertEquals(null, session.state.value.presentation)
            assertEquals(0, session.state.value.shownThisProcess)
            assertTrue(
                session.state.value.processSeen
                    .isEmpty(),
            )
        }
    }

    private fun showHost(
        context: @Composable () -> TipContext = { TipContext(TipScreen.TOOLS) },
        onAction: (TipId) -> Unit = {},
        content: @Composable () -> Unit,
    ) {
        compose.setContent {
            KnitToolsTheme {
                CompositionLocalProvider(LocalTipSession provides session) {
                    TipEnvironment(routeKey = ROUTE) {
                        TipHost(context = context(), onAction = onAction, content = content)
                    }
                }
            }
        }
    }

    private fun waitForTip(id: TipId = TipId.TOOLS_INTRO) {
        compose.mainClock.advanceTimeBy(2_500)
        compose.waitUntil(5_000) {
            session.state.value.presentation
                ?.id == id
        }
        compose.waitForIdle()
    }

    private fun bubble(textRes: Int = R.string.tip_tools_intro) =
        compose.onNode(
            SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, resources.getString(textRes)),
        )

    private companion object {
        const val ROUTE = "tools-test"
    }
}
