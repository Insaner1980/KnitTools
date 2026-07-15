package com.finnvek.knittools.ui.components

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectCardSourceTest {
    @Test
    fun `section line takes precedence and raw pdf names stay out of project cards`() {
        assertEquals(
            "Sleeve",
            projectCardSecondaryLineReflective(
                sectionName = "Sleeve",
                patternName = "Cardigan.pdf",
                projectName = "Cardigan",
            ),
        )
        assertEquals(
            null,
            projectCardSecondaryLineReflective(
                sectionName = null,
                patternName = "Cardigan.pdf",
                projectName = "Cardigan",
            ),
        )
        assertEquals(
            "Cozy Cardigan",
            projectCardSecondaryLineReflective(
                sectionName = null,
                patternName = "Cozy Cardigan",
                projectName = "Cardigan",
            ),
        )
        assertEquals(
            null,
            projectCardSecondaryLineReflective(
                sectionName = " ",
                patternName = "Cardigan",
                projectName = "Cardigan",
            ),
        )
    }

    @Test
    fun `project card visual order keeps metadata below section row`() {
        val source = ProjectSourceFiles.read(PROJECT_CARD)

        assertTrue(source.contains("text = name"))
        assertTrue(source.indexOf("text = name") < source.indexOf("text = secondaryLine"))
        assertTrue(source.indexOf("text = secondaryLine") < source.indexOf("ProjectCardStatsRow("))
        assertTrue(source.indexOf("ProjectCardStatsRow(") < source.indexOf("ProjectCardYarnLine("))
    }

    @Test
    fun `project card stats row uses grouped metadata`() {
        val source = ProjectSourceFiles.read(PROJECT_CARD)

        assertTrue(source.contains("private data class ProjectCardStats("))
        assertTrue(source.contains("stats ="))
        assertTrue(source.contains("ProjectCardStats("))
        assertTrue(source.contains("stats: ProjectCardStats,"))
    }

    @Test
    fun `project attachment indicators are direct actions with accessible targets`() {
        val source = ProjectSourceFiles.read(PROJECT_CARD)

        assertTrue(source.contains("hasPatternAttachment: Boolean = false"))
        assertTrue(source.contains("onYarnClick: (() -> Unit)? = null"))
        assertTrue(source.contains("onPatternClick: (() -> Unit)? = null"))
        assertTrue(source.contains("onPhotosClick: (() -> Unit)? = null"))
        assertTrue(source.contains("onClick = onYarnClick"))
        assertTrue(source.contains("ProjectCardAttachmentAction("))
        assertTrue(source.contains("contentDescription = stringResource(R.string.pattern_viewer_title)"))
        assertTrue(source.contains("contentDescription = stringResource(R.string.progress_photos)"))
        assertTrue(source.contains("contentDescription = stringResource(R.string.notes)"))
        assertTrue(source.contains("ProjectCardAttachmentActionsRow("))
        assertTrue(source.contains(".defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)"))
    }

    @Test
    fun `linked yarn row is a direct action with an accessible target`() {
        val source = ProjectSourceFiles.read(PROJECT_CARD)

        assertTrue(source.contains("private fun ProjectCardYarnLine("))
        assertTrue(source.contains("onClick: (() -> Unit)? = null"))
        assertTrue(source.contains(".defaultMinSize(minHeight = 48.dp)"))
        assertTrue(source.contains("Modifier.clickable(onClick = onClick)"))
    }

    private fun projectCardSecondaryLineReflective(
        sectionName: String?,
        patternName: String?,
        projectName: String,
    ): String? =
        projectCardKt()
            .getDeclaredMethod(
                "projectCardSecondaryLine",
                String::class.java,
                String::class.java,
                String::class.java,
            ).apply { isAccessible = true }
            .invoke(null, sectionName, patternName, projectName) as String?

    private fun projectCardKt(): Class<*> = Class.forName("com.finnvek.knittools.ui.components.ProjectCardKt")

    private companion object {
        private const val PROJECT_CARD =
            "app/src/main/java/com/finnvek/knittools/ui/components/ProjectCard.kt"
    }
}
