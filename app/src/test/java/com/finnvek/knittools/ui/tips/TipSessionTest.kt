package com.finnvek.knittools.ui.tips

import com.finnvek.knittools.analytics.UsageAnalytics
import com.finnvek.knittools.analytics.UsageDetail
import com.finnvek.knittools.analytics.UsageEvent
import com.finnvek.knittools.domain.tips.TipContext
import com.finnvek.knittools.domain.tips.TipId
import com.finnvek.knittools.domain.tips.TipRules
import com.finnvek.knittools.domain.tips.TipScreen
import com.finnvek.knittools.domain.tips.TipSignal
import com.finnvek.knittools.repository.OnboardingState
import com.finnvek.knittools.repository.OnboardingStateRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TipSessionTest {
    @Test
    fun `an old rapid burst cannot trigger step guidance after a blocker clears`() {
        val state =
            TipSessionState(
                loaded = true,
                onboarding = OnboardingState(initializedVersion = 1),
                incrementTimesMillis = mapOf(7L to listOf(1_000L, 1_500L, 2_000L)),
            )
        val context = TipContext(TipScreen.COUNTER, projectId = 7L, millisSinceEntry = 2_000L)

        assertEquals(TipId.STEP_SIZE, TipRules.next(state.contextFor(context, monotonicMillis = 3_000L)))
        assertNull(TipRules.next(state.contextFor(context, monotonicMillis = 3_001L)))
        assertNull(TipRules.next(state.contextFor(context.copy(blocked = true), monotonicMillis = 10_000L)))
        assertNull(TipRules.next(state.contextFor(context, monotonicMillis = 10_000L)))
        assertNull(TipRules.next(state.contextFor(context.copy(stepSize = 5), monotonicMillis = 3_000L)))
        assertNull(TipRules.next(state.contextFor(context.copy(projectId = 8L), monotonicMillis = 3_000L)))
    }

    @Test
    fun `navigation before the first visible frame leaves automatic guidance unconsumed`() =
        runTest {
            val fixture = fixture()
            val session = fixture.session
            runCurrent()
            session.onRouteChanged("tools")
            assertTrue(session.tryShow("tools", TipId.TOOLS_INTRO, null))
            val pending = requireNotNull(session.state.value.presentation)

            session.onRouteChanged("projects")
            session.markPresentationVisible(pending)
            runCurrent()

            assertEquals(0, session.state.value.shownThisProcess)
            assertTrue(
                session.state.value.processSeen
                    .isEmpty(),
            )
            coVerify(exactly = 0) { fixture.repository.markTipSeen(any()) }
            verify(exactly = 0) { fixture.analytics.track(any()) }
            verify(exactly = 0) { fixture.analytics.track(any(), any()) }
            session.onRouteChanged("tools")
            assertTrue(session.tryShow("tools", TipId.TOOLS_INTRO, null))
        }

    @Test
    fun `only loaded active route can claim one tip and showing does not mark it seen`() =
        runTest {
            val fixture = fixture()
            val session = fixture.session
            session.onRouteChanged("tools")
            assertFalse(session.tryShow("tools", TipId.TOOLS_INTRO, null))
            runCurrent()
            assertFalse(session.tryShow("old-route", TipId.TOOLS_INTRO, null))
            assertTrue(session.tryShow("tools", TipId.TOOLS_INTRO, null))
            session.markPresentationVisible(requireNotNull(session.state.value.presentation))
            assertFalse(session.tryShow("tools", TipId.BACKUP, null))
            assertTrue(
                session.state.value.processSeen
                    .isEmpty(),
            )
            coVerify(exactly = 0) { fixture.repository.markTipSeen(any()) }

            session.dismiss("old-route", TipId.TOOLS_INTRO)
            assertEquals(
                TipId.TOOLS_INTRO,
                session.state.value.presentation
                    ?.id,
            )
            session.dismiss("tools", TipId.TOOLS_INTRO, acted = true)
            assertNull(session.state.value.presentation)
            assertTrue(TipId.TOOLS_INTRO in session.state.value.processSeen)
            assertFalse(session.tryShow("tools", TipId.TOOLS_INTRO, null))
            runCurrent()
            coVerify(exactly = 1) { fixture.repository.markTipSeen(TipId.TOOLS_INTRO) }
            verify(exactly = 1) { fixture.analytics.track(UsageEvent.TIP_ACTED, UsageDetail.TIP_TOOLS_INTRO) }
        }

    @Test
    fun `navigation dismisses once and preserves the process budget`() =
        runTest {
            // CPD-OFF: Skenaario aloittaa yhdestä näytetystä vihjeestä, jotta navigoinnin budjetti näkyy paikallisesti.
            val fixture = fixture()
            runCurrent()
            val session = fixture.session
            session.onRouteChanged("tools")
            assertTrue(session.tryShow("tools", TipId.TOOLS_INTRO, null))
            // CPD-ON
            session.markPresentationVisible(requireNotNull(session.state.value.presentation))
            session.onRouteChanged("overview-7")
            session.dismiss("tools", TipId.TOOLS_INTRO)
            assertTrue(session.tryShow("overview-7", TipId.OVERVIEW_INTRO, 7L))
            session.markPresentationVisible(requireNotNull(session.state.value.presentation))
            session.onRouteChanged("projects")
            assertEquals(2, session.state.value.shownThisProcess)
            assertFalse(session.tryShow("projects", TipId.FOLDERS, null))
            runCurrent()
            coVerify(exactly = 1) { fixture.repository.markTipSeen(TipId.TOOLS_INTRO) }
            coVerify(exactly = 1) { fixture.repository.markTipSeen(TipId.OVERVIEW_INTRO) }
        }

    @Test
    fun `first committed increment hides first row and chains undo outside the automatic budget`() =
        runTest {
            val fixture = fixture()
            runCurrent()
            val session = fixture.session
            showToolsIntro(session)
            session.onRouteChanged("counter-7")
            assertFalse(session.tryShow("counter-7", TipId.UNDO, 7L))
            assertTrue(session.tryShow("counter-7", TipId.FIRST_ROW, 7L))
            session.markPresentationVisible(requireNotNull(session.state.value.presentation))

            session.onSignal(TipSignal.Increment(7L, 1_000L))

            assertNull(session.state.value.presentation)
            assertEquals(7L, session.state.value.undoProjectId)
            assertEquals(2, session.state.value.shownThisProcess)
            assertTrue(session.tryShow("counter-7", TipId.UNDO, 7L))
            session.markPresentationVisible(requireNotNull(session.state.value.presentation))
            assertEquals(2, session.state.value.shownThisProcess)
            session.dismiss("counter-7", TipId.UNDO)
            assertNull(session.state.value.undoProjectId)
            assertFalse(session.tryShow("counter-7", TipId.STEP_SIZE, 7L))
            runCurrent()
            coVerify(exactly = 1) { fixture.repository.recordProjectIncrement(7L) }
            coVerify(exactly = 1) { fixture.repository.markTipSeen(TipId.FIRST_ROW) }
        }

    @Test
    fun `increment intent timestamps and persisted tally stay project specific after navigation`() =
        runTest {
            val fixture = fixture(OnboardingState(initializedVersion = 1, projectIncrementCounts = mapOf(7L to 29)))
            runCurrent()
            val session = fixture.session
            session.onRouteChanged("counter-7")
            session.onSignal(TipSignal.Increment(7L, 3_000L))
            session.onRouteChanged("tools")
            session.onSignal(TipSignal.Increment(9L, 4_000L))
            runCurrent()
            val first =
                session.state.value.contextFor(TipContext(screen = TipScreen.COUNTER, projectId = 7L), 4_000L)
            val second =
                session.state.value.contextFor(TipContext(screen = TipScreen.COUNTER, projectId = 9L), 4_000L)
            assertEquals(30, first.incrementsInProject)
            assertEquals(listOf(3_000L), first.incrementTimesMillis)
            assertEquals(1, second.incrementsInProject)
            assertEquals(listOf(4_000L), second.incrementTimesMillis)
            coVerify(exactly = 1) { fixture.repository.recordProjectIncrement(7L) }
            coVerify(exactly = 1) { fixture.repository.recordProjectIncrement(9L) }
        }

    @Test
    fun `counter entry is recorded once until leaving or entering a new day`() =
        runTest {
            val fixture = fixture()
            runCurrent()
            val session = fixture.session
            session.onRouteChanged("counter-7")
            session.recordCounterOpen("old-route", 7L, 100L)
            session.recordCounterOpen("counter-7", 7L, 100L)
            session.onRouteChanged("counter-7")
            session.recordCounterOpen("counter-7", 7L, 100L)
            session.onRouteChanged("overview-7")
            session.onRouteChanged("counter-7")
            session.recordCounterOpen("counter-7", 7L, 100L)
            session.recordCounterOpen("counter-7", 7L, 101L)
            runCurrent()
            coVerify(exactly = 2) { fixture.repository.recordCounterOpen(100L) }
            coVerify(exactly = 1) { fixture.repository.recordCounterOpen(101L) }
        }

    @Test
    fun `reset waits for previous seen persistence then clears budget and allows guidance again`() =
        runTest {
            val fixture = fixture()
            val releaseWrite = CompletableDeferred<Unit>()
            coEvery { fixture.repository.markTipSeen(TipId.TOOLS_INTRO) } coAnswers {
                releaseWrite.await()
                fixture.stored.value = fixture.stored.value.copy(tipsSeen = setOf(TipId.TOOLS_INTRO))
            }
            runCurrent()
            val session = fixture.session
            showToolsIntro(session)
            session.dismiss("tools", TipId.TOOLS_INTRO)
            val reset = session.resetTips()
            runCurrent()
            assertFalse(reset.isCompleted)
            coVerify(exactly = 0) { fixture.repository.resetTips() }
            releaseWrite.complete(Unit)
            assertTrue(reset.await())
            runCurrent()
            assertTrue(
                fixture.stored.value.tipsSeen
                    .isEmpty(),
            )
            assertTrue(
                session.state.value.processSeen
                    .isEmpty(),
            )
            assertEquals(0, session.state.value.shownThisProcess)
            assertTrue(session.tryShow("tools", TipId.TOOLS_INTRO, null))
        }

    @Test
    fun `failed reset reports failure and preserves seen state and process budget`() =
        runTest {
            val fixture = fixture()
            coEvery { fixture.repository.resetTips() } returns false
            runCurrent()
            val session = fixture.session
            showToolsIntro(session)
            session.dismiss("tools", TipId.TOOLS_INTRO)
            assertFalse(session.resetTips().await())
            assertTrue(TipId.TOOLS_INTRO in session.state.value.processSeen)
            assertEquals(1, session.state.value.shownThisProcess)
            assertFalse(session.tryShow("tools", TipId.TOOLS_INTRO, null))
        }

    @Test
    fun `dismissing the first saved session clears its pending flag`() =
        runTest {
            val fixture = fixture(OnboardingState(initializedVersion = 1, firstSessionTipPending = true))
            runCurrent()
            val session = fixture.session
            session.onRouteChanged("overview-7")
            assertTrue(session.tryShow("overview-7", TipId.FIRST_SESSION_SAVED, 7L))
            session.markPresentationVisible(requireNotNull(session.state.value.presentation))
            session.dismiss("overview-7", TipId.FIRST_SESSION_SAVED)
            runCurrent()
            assertFalse(fixture.stored.value.firstSessionTipPending)
            coVerify(exactly = 1) { fixture.repository.setFirstSessionTipPending(false) }
        }

    @Test
    fun `target claim and stale visible callback never celebrate an unseen presentation`() =
        runTest {
            val fixture = fixture()
            val session = fixture.session
            runCurrent()
            showProjectTarget(session)
            val stale = requireNotNull(session.state.value.presentation)
            session.onRouteChanged("overview-7")
            session.markPresentationVisible(stale)
            runCurrent()
            coVerify(exactly = 0) { fixture.repository.markTargetCelebrated(any()) }
            coVerify(exactly = 0) { fixture.repository.markTipSeen(TipId.TARGET_REACHED) }

            showProjectTarget(session)
            val visible = requireNotNull(session.state.value.presentation)
            session.markPresentationVisible(visible)
            session.markPresentationVisible(visible)
            assertEquals(1, session.state.value.shownThisProcess)
            runCurrent()
            coVerify(exactly = 1) { fixture.repository.markTargetCelebrated(7L) }
            assertTrue("7" in fixture.stored.value.targetCelebratedProjects)
        }

    @Test
    fun `target dismissed before visibility remains eligible when its blocker clears`() =
        runTest {
            val fixture = fixture()
            val session = fixture.session
            runCurrent()
            showProjectTarget(session)

            session.dismiss("counter-7", TipId.TARGET_REACHED)
            runCurrent()

            val context =
                TipContext(
                    TipScreen.COUNTER,
                    projectId = 7L,
                    count = 10,
                    target = 10,
                    millisSinceEntry = 2_000L,
                )
            assertEquals(TipId.TARGET_REACHED, TipRules.next(session.state.value.contextFor(context)))
            assertEquals(0, session.state.value.shownThisProcess)
            coVerify(exactly = 0) { fixture.repository.markTargetCelebrated(any()) }
            assertTrue(session.tryShow("counter-7", TipId.TARGET_REACHED, 7L))
            session.markPresentationVisible(requireNotNull(session.state.value.presentation))
            session.dismiss("counter-7", TipId.TARGET_REACHED)
            runCurrent()

            val consumedContext = session.state.value.contextFor(context)
            assertFalse(consumedContext.targetReached)
            coVerify(exactly = 1) { fixture.repository.markTargetCelebrated(7L) }
        }

    @Test
    fun `targets are seen per project and stay celebrated across tips reset`() =
        runTest {
            val fixture =
                fixture(
                    OnboardingState(
                        initializedVersion = 1,
                        tipsSeen = setOf(TipId.TARGET_REACHED),
                        targetCelebratedProjects = setOf("7"),
                    ),
                )
            val session = fixture.session
            runCurrent()
            val counter = TipContext(TipScreen.COUNTER, projectId = 7L)
            assertTrue(
                TipId.TARGET_REACHED in
                    session.state.value
                        .contextFor(counter)
                        .tipsSeen,
            )
            assertFalse(
                TipId.TARGET_REACHED in
                    session.state.value
                        .contextFor(counter.copy(projectId = 8L))
                        .tipsSeen,
            )
            session.onRouteChanged("counter-8")
            session.onSignal(TipSignal.TargetReached(8L))
            assertTrue(session.tryShow("counter-8", TipId.TARGET_REACHED, 8L))
            session.markPresentationVisible(requireNotNull(session.state.value.presentation))
            session.dismiss("counter-8", TipId.TARGET_REACHED)
            assertTrue(session.resetTips().await())
            runCurrent()
            assertFalse(session.tryShow("counter-8", TipId.TARGET_REACHED, 8L))
            session.onRouteChanged("counter-9")
            assertTrue(session.tryShow("counter-9", TipId.TARGET_REACHED, 9L))
            coVerify(exactly = 0) { fixture.repository.markTipSeen(TipId.TARGET_REACHED) }
        }

    @Test
    fun `saved session signal persists repository firstness before guidance becomes pending`() =
        runTest {
            val fixture = fixture()
            runCurrent()
            fixture.session.onSignal(TipSignal.SessionSaved(wasFirstCompletedSession = true))
            assertFalse(fixture.session.state.value.onboarding.firstSessionTipPending)
            runCurrent()
            assertTrue(fixture.session.state.value.onboarding.firstSessionTipPending)
            coVerify(exactly = 1) { fixture.repository.recordFirstSessionSaved(true) }
        }

    @Test
    fun `requested work session bypasses seen days and automatic budget without consuming it`() =
        runTest {
            val fixture = fixture()
            val session = fixture.session
            runCurrent()
            session.onRouteChanged("counter-7")
            assertTrue(session.tryShow("counter-7", TipId.WORK_SESSION, 7L))
            session.markPresentationVisible(requireNotNull(session.state.value.presentation))
            session.dismiss("counter-7", TipId.WORK_SESSION)
            session.onRouteChanged("tools")
            assertTrue(session.tryShow("tools", TipId.TOOLS_INTRO, null))
            session.markPresentationVisible(requireNotNull(session.state.value.presentation))
            session.onRouteChanged("overview-7")
            runCurrent()
            assertEquals(2, session.state.value.shownThisProcess)
            assertTrue(TipId.WORK_SESSION in session.state.value.onboarding.tipsSeen)

            session.requestWorkSession(7L, "counter-7")
            session.onRouteChanged("counter-7")
            assertEquals("counter-7", session.state.value.routeKey)
            assertFalse(session.tryShow("counter-7", TipId.WORK_SESSION, 7L))
            val context = TipContext(TipScreen.COUNTER, projectId = 7L)
            assertTrue(session.tryShowRequestedWorkSession("counter-7", context))
            assertEquals(session.state.value.workSessionRequest, session.state.value.presentation)
            session.markPresentationVisible(requireNotNull(session.state.value.presentation))
            assertEquals(2, session.state.value.shownThisProcess)

            session.dismiss("counter-7", TipId.WORK_SESSION)
            assertNull(session.state.value.workSessionRequest)
            session.requestWorkSession(7L, "counter-7")
            assertTrue(session.tryShowRequestedWorkSession("counter-7", context))
            assertEquals(2, session.state.value.shownThisProcess)
        }

    @Test
    fun `requested work session waits for loading and blockers and only its counter can claim it`() =
        runTest {
            val fixture = fixture()
            val session = fixture.session
            val context = TipContext(TipScreen.COUNTER, projectId = 7L)
            session.requestWorkSession(7L, "counter-7")
            assertFalse(session.tryShowRequestedWorkSession("counter-7", context))
            runCurrent()
            val waiting = context.copy(projectId = null, screenReady = false)
            session.cancelInvalidWorkSessionRequest("counter-7", waiting)
            assertFalse(session.tryShowRequestedWorkSession("counter-7", waiting))
            assertFalse(session.tryShowRequestedWorkSession("counter-7", context.copy(blocked = true)))
            assertFalse(session.tryShowRequestedWorkSession("overview-7", context))
            assertFalse(session.tryShowRequestedWorkSession("counter-7", context.copy(screen = TipScreen.OVERVIEW)))
            assertFalse(session.tryShowRequestedWorkSession("counter-7", context.copy(projectId = 8L)))
            session.cancelInvalidWorkSessionRequest("overview-7", context.copy(projectId = 8L))
            session.cancelInvalidWorkSessionRequest(
                "counter-7",
                context.copy(screen = TipScreen.OVERVIEW, projectId = 8L),
            )
            assertEquals(
                7L,
                session.state.value.workSessionRequest
                    ?.projectId,
            )
            assertEquals(0, session.state.value.shownThisProcess)
            assertTrue(session.tryShowRequestedWorkSession("counter-7", context))
        }

    @Test
    fun `loaded invalid target cancels pending work session without marking an unseen tip`() =
        runTest {
            val fixture = fixture()
            val session = fixture.session
            val context = TipContext(TipScreen.COUNTER, projectId = 7L)
            runCurrent()
            val invalidContexts =
                listOf(
                    context.copy(projectId = null),
                    context.copy(projectId = 8L),
                    context.copy(isCompleted = true),
                    context.copy(hasSessions = true),
                    context.copy(hasActiveSession = true),
                )
            invalidContexts.forEach { invalid ->
                session.requestWorkSession(7L, "counter-7")
                assertFalse(session.tryShowRequestedWorkSession("counter-7", invalid))
                session.cancelInvalidWorkSessionRequest("counter-7", invalid)
                assertNull(session.state.value.workSessionRequest)
                assertNull(session.state.value.presentation)
            }
            assertEquals(0, session.state.value.shownThisProcess)
            assertTrue(
                session.state.value.processSeen
                    .isEmpty(),
            )
            coVerify(exactly = 0) { fixture.repository.markTipSeen(any()) }
        }

    @Test
    fun `starting a session dismisses visible requested guidance and clears its request`() =
        runTest {
            val fixture = fixture()
            val session = fixture.session
            val context = TipContext(TipScreen.COUNTER, projectId = 7L)
            runCurrent()
            session.requestWorkSession(7L, "counter-7")
            assertTrue(session.tryShowRequestedWorkSession("counter-7", context))
            session.markPresentationVisible(requireNotNull(session.state.value.presentation))
            session.cancelInvalidWorkSessionRequest("counter-7", context.copy(hasActiveSession = true))
            assertNull(session.state.value.workSessionRequest)
            assertNull(session.state.value.presentation)
            assertEquals(0, session.state.value.shownThisProcess)
            runCurrent()
            coVerify(exactly = 1) { fixture.repository.markTipSeen(TipId.WORK_SESSION) }
        }

    @Test
    fun `route generations distinguish return visits but survive same entry and tips reset`() =
        runTest {
            val session = fixture().session
            runCurrent()
            session.onRouteChanged("overview-7")
            assertEquals(1L, session.state.value.routeEntryGeneration)
            session.onRouteChanged(null)
            session.onRouteChanged("overview-7")
            assertEquals(1L, session.state.value.routeEntryGeneration)
            session.requestWorkSession(7L, "counter-7")
            assertEquals(2L, session.state.value.routeEntryGeneration)
            session.onRouteChanged(null)
            session.onRouteChanged("counter-7")
            assertEquals(2L, session.state.value.routeEntryGeneration)
            assertEquals(
                7L,
                session.state.value.workSessionRequest
                    ?.projectId,
            )
            assertTrue(session.resetTips().await())
            assertEquals(2L, session.state.value.routeEntryGeneration)
            assertNull(session.state.value.workSessionRequest)
            session.requestWorkSession(7L, "counter-7")
            session.onRouteChanged("overview-7")
            assertNull(session.state.value.workSessionRequest)
            assertEquals(3L, session.state.value.routeEntryGeneration)
            session.onRouteChanged("counter-7")
            assertEquals(4L, session.state.value.routeEntryGeneration)
            assertFalse(session.tryShowRequestedWorkSession("counter-7", TipContext(TipScreen.COUNTER, projectId = 7L)))
        }

    private fun showToolsIntro(session: TipSession) {
        session.onRouteChanged("tools")
        assertTrue(session.tryShow("tools", TipId.TOOLS_INTRO, null))
        session.markPresentationVisible(requireNotNull(session.state.value.presentation))
    }

    private fun showProjectTarget(session: TipSession) {
        session.onRouteChanged("counter-7")
        session.onSignal(TipSignal.TargetReached(7L))
        assertTrue(session.tryShow("counter-7", TipId.TARGET_REACHED, 7L))
    }

    private fun TestScope.fixture(initial: OnboardingState = OnboardingState(initializedVersion = 1)): Fixture {
        val stored = MutableStateFlow(initial)
        val repository = mockk<OnboardingStateRepository>()
        val analytics = mockk<UsageAnalytics>(relaxed = true)
        every { repository.state } returns stored
        coEvery { repository.markTipSeen(any()) } coAnswers {
            stored.value = stored.value.copy(tipsSeen = stored.value.tipsSeen + firstArg<TipId>())
        }
        coEvery { repository.recordCounterOpen(any()) } returns Unit
        coEvery { repository.recordProjectIncrement(any()) } returns Unit
        coEvery { repository.resetTips() } coAnswers {
            stored.value = stored.value.copy(tipsSeen = emptySet())
            true
        }
        coEvery { repository.setFirstSessionTipPending(any()) } coAnswers {
            stored.value = stored.value.copy(firstSessionTipPending = firstArg())
        }
        coEvery { repository.markTargetCelebrated(any()) } coAnswers {
            stored.value =
                stored.value.copy(
                    targetCelebratedProjects = stored.value.targetCelebratedProjects + firstArg<Long>().toString(),
                )
        }
        coEvery { repository.recordFirstSessionSaved(any()) } coAnswers {
            stored.value =
                stored.value.copy(
                    firstSaveObserved = true,
                    firstSessionTipPending =
                        stored.value.firstSessionTipPending || !stored.value.firstSaveObserved && firstArg<Boolean>(),
                )
        }
        return Fixture(
            TipSession(repository, analytics, backgroundScope),
            repository,
            analytics,
            stored,
        )
    }

    private data class Fixture(
        val session: TipSession,
        val repository: OnboardingStateRepository,
        val analytics: UsageAnalytics,
        val stored: MutableStateFlow<OnboardingState>,
    )
}
