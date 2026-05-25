package com.finnvek.knittools.ui.components

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectCardSourceTest {
    @Test
    fun `section line takes precedence over pattern filename`() {
        assertEquals(
            "Sleeve",
            projectCardSecondaryLineReflective(
                sectionName = "Sleeve",
                patternName = "Cardigan.pdf",
                projectName = "Cardigan",
            ),
        )
        assertEquals(
            "Cardigan.pdf",
            projectCardSecondaryLineReflective(
                sectionName = null,
                patternName = "Cardigan.pdf",
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

        assertTrue(source.indexOf("text = name") < source.indexOf("text = secondaryLine"))
        assertTrue(source.indexOf("text = secondaryLine") < source.indexOf("ProjectCardStatsRow("))
        assertTrue(source.indexOf("ProjectCardStatsRow(") < source.indexOf("ProjectCardYarnLine("))
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
