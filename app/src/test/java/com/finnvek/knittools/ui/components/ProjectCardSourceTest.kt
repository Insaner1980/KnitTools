package com.finnvek.knittools.ui.components

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectCardSourceTest {
    @Test
    fun `section takes precedence and duplicate or raw pattern names stay hidden`() {
        assertEquals("Sleeve", projectListItemSecondaryLine(" Sleeve ", "Cardigan.pdf", "Cardigan"))
        assertEquals(null, projectListItemSecondaryLine(null, "Cardigan.pdf", "Cardigan"))
        assertEquals("Cozy Cardigan", projectListItemSecondaryLine(null, "Cozy Cardigan", "Cardigan"))
        assertEquals(null, projectListItemSecondaryLine(" ", "Cardigan", "Cardigan"))
    }

    @Test
    fun `cards preserve selection and open the overview without a separate counter button`() {
        val source = ProjectSourceFiles.read(CARD)
        listOf(
            "this.selected = selected",
            "onLongClick = onLongClick",
            "Checkbox(",
            "R.string.project_card_open_overview_action",
            "semantics(mergeDescendants = true)",
            "maxLines = 2",
        ).forEach { assertTrue(it, source.contains(it)) }
        // Laskuriin vie listalla vain Continue-hero; kortin napautus avaa projektinäkymän.
        listOf("onOpenCounter", "PlayArrow", "project_continue_content_description", "onPhotosClick", "onPatternClick")
            .forEach { assertFalse(it, source.contains(it)) }
    }

    @Test
    fun `thumbnails draw fabric swatches instead of generic icons`() {
        val thumbnail = ProjectSourceFiles.read(THUMBNAIL)
        val swatch = ProjectSourceFiles.read(SWATCH)
        assertTrue(thumbnail.contains("CraftType.KNITTING -> FabricKind.KNIT"))
        assertTrue(thumbnail.contains("CraftType.CROCHET -> FabricKind.CROCHET"))
        assertTrue(thumbnail.contains("fabric = FabricKind.YARN"))
        assertTrue(thumbnail.contains("MaterialTheme.knitToolsColors.yarnSwatchNeutral"))
        // Tilkku piirretään koodilla: ei kuvatiedostoja eikä yleisiä Material-ikoneita.
        listOf("R.drawable.", "Icons.", "Icon(").forEach { assertFalse(it, thumbnail.contains(it)) }
        listOf("knitFabric(", "crochetFabric(", "yarnFabric(", "drawWithCache")
            .forEach { assertTrue(it, swatch.contains(it)) }
    }

    @Test
    fun `cards use shared target progress and completed date`() {
        val source = ProjectSourceFiles.read(CARD)
        listOf(
            "CounterValueFormatter.forMainCounter",
            "mainCounterTargetStatus",
            "mainCounterTargetFraction",
            "mainCounterTargetText",
            "mainCounterCountText",
            "ProjectProgressBar",
            "projectTimestampText(project.completedAt ?: project.updatedAt, true)",
        ).forEach { assertTrue(it, source.contains(it)) }
        assertTrue(source.indexOf("if (project.isCompleted)") < source.indexOf("ProjectCardProgress(project, compact)"))
    }

    private companion object {
        const val CARD = "app/src/main/java/com/finnvek/knittools/ui/components/ProjectCard.kt"
        const val THUMBNAIL = "app/src/main/java/com/finnvek/knittools/ui/components/ProjectThumbnail.kt"
        const val SWATCH = "app/src/main/java/com/finnvek/knittools/ui/components/FabricSwatch.kt"
    }
}
