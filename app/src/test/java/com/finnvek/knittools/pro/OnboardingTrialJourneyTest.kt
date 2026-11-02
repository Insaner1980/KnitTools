package com.finnvek.knittools.pro

import com.finnvek.knittools.domain.model.CounterProject
import org.junit.Assert.assertEquals
import org.junit.Test

class OnboardingTrialJourneyTest {
    private val empty =
        TrialJourneyFacts(
            trialStartTimestamp = 1,
            activeProjectCount = 0,
            latestActiveProject = null,
            projectsAdded = 0,
            countersAdded = 0,
            yarnCardsAdded = 0,
            photosAdded = 0,
            repeatCounterAdded = false,
            chartSeen = false,
            photoPdfCreated = false,
        )

    @Test fun `onboarding without a project offers only first project creation`() {
        assertEquals(
            listOf(TrialJourneyAction.AnotherProject),
            empty.steps(false, fromOnboarding = true).map { it.action },
        )
    }

    @Test fun `onboarding with a project offers the approved three actions`() {
        val facts = empty.copy(activeProjectCount = 1, latestActiveProject = CounterProject(id = 7, name = "My socks"))
        assertEquals(
            listOf(TrialJourneyAction.Widget, TrialJourneyAction.RepeatCounter, TrialJourneyAction.AnotherProject),
            facts.steps(false, fromOnboarding = true).map { it.action },
        )
        assertEquals(5, facts.steps(false).size)
        assertEquals(
            listOf(TrialJourneyAction.AnotherProject, TrialJourneyAction.WeeklyChart),
            empty.steps(false).map {
                it.action
            },
        )
    }
}
