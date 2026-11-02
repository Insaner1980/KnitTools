package com.finnvek.knittools.domain.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingDraftTest {
    @Test fun `example is never a project name and demo has no initial undo`() {
        val draft = OnboardingDraft("draft")
        assertEquals("", draft.name)
        assertEquals(draft, draft.undoDemo())
        assertEquals(47, draft.changeDemo(1).undoDemo().demoCount)
        assertTrue(
            draft
                .changeDemo(1)
                .undoDemo()
                .demoHistory
                .isEmpty(),
        )
        assertEquals(0, draft.copy(demoCount = 0).changeDemo(-1).demoCount)
    }

    @Test fun `back navigation preserves project edits and selection order`() {
        val draft =
            OnboardingDraft("draft", step = OnboardingStep.Benefits, demoCount = 49)
                .toggleNeed(OnboardingNeed.Yarn)
                .toggleNeed(OnboardingNeed.Pattern)
        val form = draft.next().copy(name = "My scarf", initialCount = "12", targetRows = "120")
        assertEquals(49, draft.next().initialCount.toInt())
        assertEquals(form, form.previous().next())
        assertEquals(listOf(OnboardingNeed.Yarn, OnboardingNeed.Pattern), form.needs)
        assertEquals(listOf(OnboardingNeed.Pattern), form.toggleNeed(OnboardingNeed.Yarn).needs)
        assertEquals(OnboardingStep.Price, form.copy(step = OnboardingStep.Price).previous().step)
    }

    @Test fun `draft survives serialization including attachments and completed destination`() {
        val draft =
            OnboardingDraft(
                "stable-id",
                step = OnboardingStep.Price,
                craft = OnboardingCraft.Crochet,
                name = "My scarf",
                demoHistory = listOf(47, 48),
                pdfUri = "file:///pattern.pdf",
                pdfName = "pattern.pdf",
                projectId = 7,
                completed = true,
                exitPending = true,
            )
        assertEquals(draft, Json.decodeFromString<OnboardingDraft>(Json.encodeToString(draft)))
        assertEquals(CraftType.CROCHET, draft.craftType)
    }
}
