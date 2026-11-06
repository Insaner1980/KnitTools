package com.finnvek.knittools.domain.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingDraftTest {
    @Test fun `forward steps run craft to project and stop before creation`() {
        val welcome = OnboardingDraft("draft")
        assertEquals(welcome, welcome.previous())
        assertEquals(CraftType.KNITTING, welcome.craftType)
        assertEquals(CraftType.KNITTING, welcome.copy(craft = OnboardingCraft.Both).craftType)
        val craft = welcome.next()
        val needs = craft.next()
        val counter = needs.next()
        val project = counter.next()
        assertEquals(OnboardingStep.Craft, craft.step)
        assertEquals(OnboardingStep.Needs, needs.step)
        assertEquals(OnboardingStep.Counter, counter.step)
        assertEquals(OnboardingStep.Project, project.step)
        // Projektin luonti on ViewModelin transaktio, ei pelkkä vaiheen vaihto.
        assertEquals(project, project.next())
        val price = project.copy(step = OnboardingStep.Price)
        assertEquals(price, price.next())
        assertEquals(price, price.previous())
        val maximum = welcome.copy(demoCount = 99_999)
        assertEquals(maximum, maximum.changeDemo(1))
    }

    @Test fun `ready continues to price and never returns to the form`() {
        val ready = OnboardingDraft("draft", step = OnboardingStep.Ready, projectId = 7)
        assertEquals(OnboardingStep.Price, ready.next().step)
        assertEquals(ready, ready.previous())
        assertFalse(OnboardingStep.Ready.canGoBack)
        assertFalse(OnboardingStep.Welcome.canGoBack)
        assertTrue(OnboardingStep.Project.canGoBack)
    }

    @Test fun `demo starts from zero and project count is not copied from the demo`() {
        val draft = OnboardingDraft("draft")
        assertEquals("", draft.name)
        assertEquals(0, draft.demoCount)
        assertEquals("0", draft.initialCount)
        assertEquals(draft, draft.undoDemo())
        assertEquals(0, draft.changeDemo(1).undoDemo().demoCount)
        assertTrue(
            draft
                .changeDemo(1)
                .undoDemo()
                .demoHistory
                .isEmpty(),
        )
        assertEquals(draft, draft.changeDemo(-1))
        val counted = draft.copy(step = OnboardingStep.Counter).changeDemo(1).changeDemo(1)
        assertEquals("0", counted.next().initialCount)
    }

    @Test fun `return card appears after two counted rows and stays after undo`() {
        val draft = OnboardingDraft("draft")
        assertFalse(draft.changeDemo(1).demoReturnVisible)
        val twice = draft.changeDemo(1).changeDemo(1)
        assertTrue(twice.demoReturnVisible)
        assertTrue(twice.undoDemo().undoDemo().demoReturnVisible)
        assertFalse(
            draft
                .copy(demoCount = 5)
                .changeDemo(-1)
                .changeDemo(-1)
                .demoReturnVisible,
        )
    }

    @Test fun `reading line follows the three row repeat`() {
        assertEquals(listOf(0, 1, 2, 0, 1), (0..4).map(::demoPatternRow))
    }

    @Test fun `pro groups follow the first chosen need and keep insights last`() {
        val default = listOf(ProAddsGroup.Projects, ProAddsGroup.Workflow, ProAddsGroup.Insights)
        val workflow = listOf(ProAddsGroup.Workflow, ProAddsGroup.Projects, ProAddsGroup.Insights)
        assertEquals(default, proGroupOrder(emptyList()))
        assertEquals(default, proGroupOrder(listOf(OnboardingNeed.Yarn, OnboardingNeed.Counting)))
        assertEquals(default, proGroupOrder(listOf(OnboardingNeed.Projects)))
        assertEquals(workflow, proGroupOrder(listOf(OnboardingNeed.Counting)))
        assertEquals(workflow, proGroupOrder(listOf(OnboardingNeed.Pattern, OnboardingNeed.Yarn)))
        assertEquals(workflow, proGroupOrder(listOf(OnboardingNeed.Returning)))
    }

    @Test fun `back navigation preserves project edits and selection order`() {
        val draft =
            OnboardingDraft("draft", step = OnboardingStep.Counter, demoCount = 49)
                .toggleNeed(OnboardingNeed.Yarn)
                .toggleNeed(OnboardingNeed.Pattern)
        val form = draft.next().copy(name = "My scarf", initialCount = "12", targetRows = "120")
        assertEquals(form, form.previous().next())
        assertEquals(listOf(OnboardingNeed.Yarn, OnboardingNeed.Pattern), form.needs)
        assertEquals(listOf(OnboardingNeed.Pattern), form.toggleNeed(OnboardingNeed.Yarn).needs)
        assertEquals(OnboardingStep.Price, form.copy(step = OnboardingStep.Price).previous().step)
        assertEquals(OnboardingStep.Welcome, form.copy(step = OnboardingStep.Craft).previous().step)
    }

    @Test fun `draft survives serialization including attachments and completed destination`() {
        val draft =
            OnboardingDraft(
                "stable-id",
                step = OnboardingStep.Ready,
                craft = OnboardingCraft.Crochet,
                name = "My scarf",
                demoHistory = listOf(0, 1),
                demoIncrements = 2,
                pdfUri = "file:///pattern.pdf",
                pdfName = "pattern.pdf",
                projectId = 7,
                completed = true,
                exitPending = true,
            )
        assertEquals(draft, Json.decodeFromString<OnboardingDraft>(Json.encodeToString(draft)))
        assertEquals(CraftType.CROCHET, draft.craftType)
    }

    @Test fun `draft saved before the redesign still decodes without the removed field`() {
        val legacy = """{"id":"old","step":"Project","projectStarted":true,"name":"Socks"}"""
        val decoded = lenientJson.decodeFromString<OnboardingDraft>(legacy)
        assertEquals(OnboardingStep.Project, decoded.step)
        assertEquals("Socks", decoded.name)
    }

    private companion object {
        val lenientJson = Json { ignoreUnknownKeys = true }
    }
}
