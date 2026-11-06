package com.finnvek.knittools.ui.screens.onboarding

import com.finnvek.knittools.domain.model.OnboardingNeed
import org.junit.Assert.assertEquals
import org.junit.Test

class OnboardingToolboxTest {
    @Test
    fun `every picked tool is kept in selection order`() {
        val needs = OnboardingNeed.entries.reversed()
        assertEquals(
            listOf(JourneyTool.Insights, JourneyTool.Yarn, JourneyTool.Counter, JourneyTool.Pattern),
            pickedJourneyTools(needs),
        )
    }

    @Test
    fun `returning maps to the counter that remembers where you stopped`() {
        assertEquals(JourneyTool.Counter, OnboardingNeed.Returning.tool())
        assertEquals(
            listOf(JourneyTool.Counter),
            pickedJourneyTools(listOf(OnboardingNeed.Returning, OnboardingNeed.Counting)),
        )
    }

    @Test
    fun `grid lists picked tools first and every tool exactly once`() {
        val ordered = orderedJourneyTools(listOf(OnboardingNeed.Yarn))
        assertEquals(JourneyTool.Yarn, ordered.first())
        assertEquals(JourneyTool.entries.toSet(), ordered.toSet())
        assertEquals(JourneyTool.entries.size, ordered.size)
        assertEquals(JourneyTool.entries, orderedJourneyTools(emptyList()))
    }
}
