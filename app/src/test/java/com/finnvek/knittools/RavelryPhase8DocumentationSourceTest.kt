package com.finnvek.knittools

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RavelryPhase8DocumentationSourceTest {
    @Test
    fun `phase 8 ravelry ui and saved pattern flow is documented`() {
        val agents = ProjectSourceFiles.read(AGENTS)
        val codex = ProjectSourceFiles.read(CODEX)
        val project = ProjectSourceFiles.read(PROJECT)
        val memory = ProjectSourceFiles.read(MEMORY)
        val progress = ProjectSourceFiles.read(PROGRESS)
        val plan = ProjectSourceFiles.read(UI_PLAN)

        listOf(agents, codex).forEach { instructions ->
            assertTrue(instructions.contains("Phase 8 completes Ravelry UI and saved-pattern UX"))
            assertTrue(instructions.contains("PatternPickerSheet lists all saved patterns"))
            assertTrue(instructions.contains("project pattern cards open SavedPatternDetail for metadata-only links"))
        }

        assertTrue(project.contains("Nykyinen Android-koodi Phase 8:n jälkeen"))
        assertTrue(project.contains("RavelryImportConfirmationSheet"))
        assertTrue(project.contains("ACTION_SEND text/plain"))
        assertTrue(project.contains("PatternPickerSheet listaa kaikki saved patternit"))
        assertTrue(project.contains("SavedPatternDetailScreen"))
        assertTrue(project.contains("metadata-only linkit avaavat `SavedPatternDetail`-reitin"))

        assertTrue(memory.contains("2026-06-12: Ravelry UI/Saved Patterns -vaihe 8"))
        assertTrue(memory.contains("metadata-only `SavedPatternDetail`"))
        assertTrue(memory.contains("source-kategoriaotsikoita"))

        assertTrue(progress.contains("Started phase: Phase 8 - Ravelry UI And Saved Patterns UX"))
        assertTrue(progress.contains("Completed phase: Phase 8 - Ravelry UI And Saved Patterns UX"))
        assertTrue(progress.contains("Phase 8 completed"))
        assertTrue(progress.contains("At the end of Phase 8, release-surface hardening remained later work"))
        assertFalse(progress.contains("Phase 8 was not started"))

        assertTrue(plan.contains("- [x] Dokumentit:"))
    }

    private companion object {
        private const val AGENTS = "AGENTS.md"
        private const val CODEX = "CODEX.md"
        private const val PROJECT = "PROJECT.md"
        private const val MEMORY = "memory/MEMORY.md"
        private const val PROGRESS = "config/ravelry-backend-progress.md"
        private const val UI_PLAN = "Ravelry UI ja Saved Patterns -toteutussuunnitelma.md"
    }
}
