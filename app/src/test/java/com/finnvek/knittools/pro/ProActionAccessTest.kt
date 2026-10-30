package com.finnvek.knittools.pro

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ProActionAccessTest {
    @Before
    fun setUp() {
        mockkObject(ProPolicyTestControl)
        every { ProPolicyTestControl.isActive } returns true
    }

    @After
    fun tearDown() {
        unmockkObject(ProPolicyTestControl)
    }

    @Test
    fun `every action context resolves each status readiness and existing content combination`() {
        val lockedStatuses =
            listOf(ProActionAccess.Locked, ProActionAccess.Trial, ProActionAccess.Locked, ProActionAccess.Allowed)
        val allowedStatuses = List(ProStatus.entries.size) { ProActionAccess.Allowed }
        val flags =
            listOf(
                Triple(true, false, lockedStatuses),
                Triple(true, true, allowedStatuses),
                Triple(false, false, allowedStatuses),
                Triple(false, true, allowedStatuses),
            )
        contexts.forEach { (context, feature) ->
            flags.forEach { (needsEntitlement, alreadyUnlocked, expected) ->
                ProStatus.entries.forEachIndexed { index, status ->
                    listOf(false, true).forEach { ready ->
                        assertEquals(
                            "$context $status ready=$ready needs=$needsEntitlement unlocked=$alreadyUnlocked",
                            if (ready) expected[index] else ProActionAccess.Unknown,
                            proActionAccess(ProState(status), ready, feature, needsEntitlement, alreadyUnlocked),
                        )
                    }
                }
            }
        }
    }

    @Test
    fun `debug override permits advanced actions without claiming a trial or purchase`() {
        every { ProPolicyTestControl.isActive } returns false
        val state = ProState(ProStatus.TRIAL_EXPIRED)
        ProFeature.entries.forEach { feature ->
            assertEquals(ProActionAccess.Allowed, proActionAccess(state, ready = true, feature = feature))
            assertEquals(ProActionAccess.Unknown, proActionAccess(state, ready = false, feature = feature))
        }
        assertEquals(ProStatus.TRIAL_EXPIRED, state.status)
    }

    private val contexts =
        listOf(
            "New project" to ProFeature.UNLIMITED_PROJECTS,
            "Reactivate" to ProFeature.UNLIMITED_PROJECTS,
            "Save PDF new project" to ProFeature.UNLIMITED_PROJECTS,
            "Add note" to ProFeature.NOTES,
            "Add photo" to ProFeature.PROGRESS_PHOTOS,
            "Add yarn" to ProFeature.UNLIMITED_YARN,
            "Save to My Yarn" to ProFeature.UNLIMITED_YARN,
            "Add counter" to ProFeature.MULTIPLE_COUNTERS,
            "Shaping" to ProFeature.SHAPING_COUNTER,
            "Repeat section" to ProFeature.REPEAT_SECTION,
            "Add reminder" to ProFeature.ROW_REMINDERS,
            "Photos to PDF" to ProFeature.PATTERN_CAMERA_SCAN,
            "Compact repeat" to ProFeature.SECONDARY_COUNTER,
            "Paste instruction" to ProFeature.INSTRUCTION_PASTE,
            "Pin widget" to ProFeature.WIDGET,
            "Guide insights" to ProFeature.INSIGHTS_CHARTS,
        )
}
