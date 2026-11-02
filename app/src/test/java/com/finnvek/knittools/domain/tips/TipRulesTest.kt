package com.finnvek.knittools.domain.tips

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TipRulesTest {
    @Test
    fun `attached onboarding pattern is eligible only on its ready visible route`() {
        val context =
            TipContext(
                screen = TipScreen.COUNTER,
                projectId = 7,
                millisSinceEntry = 2_000,
                hasDocuments = true,
                onboardingPatternReady = true,
                tipsSeen = setOf(TipId.FIRST_ROW, TipId.UNDO),
            )
        assertEquals(TipId.PATTERN_TILE, TipRules.next(context))
        assertNull(TipRules.next(context.copy(hasDocuments = false)))
        assertNull(TipRules.next(context.copy(onboardingPatternReady = false)))
        assertNull(TipRules.next(context.copy(blocked = true)))
        assertNull(TipRules.next(context.copy(screenReady = false)))
        assertNull(TipRules.next(context.copy(tipsSeen = context.tipsSeen + TipId.PATTERN_TILE)))
    }

    private val counter = TipContext(screen = TipScreen.COUNTER, projectId = 7L, millisSinceEntry = 2_000L)

    @Test
    fun `starter and undo tips have priority before other counter guidance`() {
        val context = counter.copy(isStarter = true, undoAfterFirstRow = true, incrementsInProject = 30)
        assertEquals(TipId.FIRST_ROW, TipRules.next(context))
        assertEquals(TipId.UNDO, TipRules.next(context.copy(tipsSeen = setOf(TipId.FIRST_ROW))))
        assertEquals(
            TipId.STEP_SIZE,
            TipRules.next(context.copy(tipsSeen = setOf(TipId.FIRST_ROW, TipId.UNDO))),
        )
    }

    @Test
    fun `starting count is not an increment tally and step size needs real counting`() {
        assertNull(TipRules.next(counter.copy(count = 37)))
        assertNull(TipRules.next(counter.copy(incrementsInProject = 29)))
        assertEquals(TipId.STEP_SIZE, TipRules.next(counter.copy(incrementsInProject = 30)))
        assertNull(TipRules.next(counter.copy(incrementsInProject = 30, stepSize = 2)))
    }

    @Test
    fun `three increments inside two seconds make the step tip eligible`() {
        assertNull(TipRules.next(counter.copy(incrementTimesMillis = listOf(1_000L, 2_000L))))
        assertNull(TipRules.next(counter.copy(incrementTimesMillis = listOf(1_000L, 2_000L, 3_001L))))
        assertEquals(
            TipId.STEP_SIZE,
            TipRules.next(counter.copy(incrementTimesMillis = listOf(3_000L, 1_000L, 2_000L))),
        )
    }

    @Test
    fun `work session requires two counter days without saved or active sessions`() {
        assertNull(TipRules.next(counter.copy(counterOpenDays = 1)))
        assertEquals(TipId.WORK_SESSION, TipRules.next(counter.copy(counterOpenDays = 2)))
        assertNull(TipRules.next(counter.copy(counterOpenDays = 2, hasSessions = true)))
        assertNull(TipRules.next(counter.copy(counterOpenDays = 2, hasActiveSession = true)))
    }

    @Test
    fun `pattern tip waits for three opens and accepts PDF and web pattern evidence`() {
        assertNull(TipRules.next(counter.copy(counterOpenCount = 2)))
        assertEquals(TipId.PATTERN_TILE, TipRules.next(counter.copy(counterOpenCount = 3)))
        assertNull(TipRules.next(counter.copy(counterOpenCount = 3, hasDocuments = true)))
        assertNull(TipRules.next(counter.copy(counterOpenCount = 3, hasWebPattern = true)))
    }

    @Test
    fun `screen awake tip waits for four opens and disappears once enabled`() {
        val context = counter.copy(counterOpenCount = 4, hasDocuments = true)
        assertEquals(TipId.KEEP_SCREEN_ON, TipRules.next(context))
        assertNull(TipRules.next(context.copy(keepScreenAwake = true)))
    }

    @Test
    fun `target guidance requires a crossing signal and reached target`() {
        assertNull(TipRules.next(counter.copy(target = 10, count = 10)))
        assertNull(TipRules.next(counter.copy(target = 10, count = 9, targetReached = true)))
        assertNull(TipRules.next(counter.copy(count = 10, targetReached = true)))
        assertEquals(TipId.TARGET_REACHED, TipRules.next(counter.copy(target = 10, count = 11, targetReached = true)))
    }

    @Test
    fun `widget guidance needs platform support plus a saved session or three days`() {
        assertNull(TipRules.next(counter.copy(hasSessions = true)))
        assertNull(TipRules.next(counter.copy(widgetPinSupported = true)))
        assertEquals(TipId.WIDGET, TipRules.next(counter.copy(widgetPinSupported = true, hasSessions = true)))
        assertEquals(
            TipId.WIDGET,
            TipRules.next(counter.copy(widgetPinSupported = true, firstSessionTipPending = true)),
        )
        assertEquals(
            TipId.WIDGET,
            TipRules.next(counter.copy(widgetPinSupported = true, counterOpenDays = 3, hasActiveSession = true)),
        )
    }

    @Test
    fun `completed and missing projects have no counter guidance`() {
        assertNull(TipRules.next(counter.copy(isStarter = true, isCompleted = true)))
        assertNull(TipRules.next(counter.copy(isStarter = true, projectId = null)))
    }

    @Test
    fun `overview introduction precedes pending first session guidance`() {
        val context = counter.copy(screen = TipScreen.OVERVIEW, firstSessionTipPending = true)
        assertEquals(TipId.OVERVIEW_INTRO, TipRules.next(context))
        assertEquals(TipId.FIRST_SESSION_SAVED, TipRules.next(context.copy(tipsSeen = setOf(TipId.OVERVIEW_INTRO))))
        assertNull(TipRules.next(context.copy(projectId = null)))
        assertNull(TipRules.next(context.copy(firstSessionTipPending = false, tipsSeen = setOf(TipId.OVERVIEW_INTRO))))
    }

    @Test
    fun `reading guidance belongs only to a project PDF`() {
        val context = counter.copy(screen = TipScreen.PROJECT_READER, hasDocuments = true)
        assertEquals(TipId.READING_LINE, TipRules.next(context))
        assertNull(TipRules.next(context.copy(projectId = null)))
        assertNull(TipRules.next(context.copy(hasDocuments = false, hasWebPattern = true)))
    }

    @Test
    fun `backup waits seven days for projects without a successful backup and precedes folders`() {
        val context =
            TipContext(
                screen = TipScreen.PROJECTS,
                millisSinceEntry = 2_000L,
                firstUseEpochDay = 100L,
                today = 106L,
                projectCount = 1,
            )
        assertNull(TipRules.next(context))
        assertEquals(TipId.BACKUP, TipRules.next(context.copy(today = 107L)))
        assertNull(TipRules.next(context.copy(today = 107L, backupCreated = true)))
        assertNull(TipRules.next(context.copy(today = 107L, projectCount = 0)))
        assertEquals(TipId.BACKUP, TipRules.next(context.copy(today = 107L, projectCount = 4)))
        assertEquals(TipId.FOLDERS, TipRules.next(context.copy(today = 107L, projectCount = 4, backupCreated = true)))
        assertNull(TipRules.next(context.copy(projectCount = 3)))
    }

    @Test
    fun `tools introduction waits for readiness entry delay and no blocking UI`() {
        val context = TipContext(screen = TipScreen.TOOLS, millisSinceEntry = 2_000L)
        assertEquals(TipId.TOOLS_INTRO, TipRules.next(context))
        assertNull(TipRules.next(context.copy(screenReady = false)))
        assertNull(TipRules.next(context.copy(blocked = true)))
        assertNull(TipRules.next(context.copy(millisSinceEntry = 1_999L)))
        assertNull(TipRules.next(context.copy(tipsSeen = setOf(TipId.TOOLS_INTRO))))
    }

    @Test
    fun `process budget allows only the chained undo beyond two automatic tips`() {
        assertNull(TipRules.next(counter.copy(isStarter = true, shownThisProcess = 2)))
        assertNull(TipRules.next(counter.copy(shownThisProcess = 2)))
        val context = counter.copy(shownThisProcess = 2, undoAfterFirstRow = true)
        assertEquals(TipId.UNDO, TipRules.next(context))
        assertNull(TipRules.next(context.copy(tipsSeen = setOf(TipId.UNDO))))
        assertNull(TipRules.next(context.copy(blocked = true)))
    }
}
