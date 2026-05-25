package com.finnvek.knittools.ui.screens.project

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectListWorkspaceSourceTest {
    @Test
    fun `continue knitting model carries only cheap project context`() {
        val viewModel = ProjectSourceFiles.read(PROJECT_LIST_VIEW_MODEL)
        val screen = ProjectSourceFiles.read(PROJECT_LIST_SCREEN)

        assertTrue(viewModel.contains("val sectionName: String?"))
        assertTrue(viewModel.contains("val targetRows: Int?"))
        assertTrue(viewModel.contains("sectionName = candidate.sectionName"))
        assertTrue(viewModel.contains("targetRows = candidate.targetRows"))
        assertTrue(screen.contains("private fun continueKnittingContextLine("))
    }

    @Test
    fun `continue knitting context prefers section before row progress`() {
        assertEquals(
            "Sleeve",
            continueKnittingContextLineReflective(
                sectionName = "Sleeve",
                rowCount = 12,
                targetRows = 40,
                fallback = "Row 12 / 40",
            ),
        )
        assertEquals(
            "Row 12 / 40",
            continueKnittingContextLineReflective(
                sectionName = " ",
                rowCount = 12,
                targetRows = 40,
                fallback = "Row 12 / 40",
            ),
        )
    }

    private fun continueKnittingContextLineReflective(
        sectionName: String?,
        rowCount: Int,
        targetRows: Int?,
        fallback: String,
    ): String =
        projectListScreenKt()
            .getDeclaredMethod(
                "continueKnittingContextLine",
                String::class.java,
                Int::class.javaPrimitiveType,
                Int::class.javaObjectType,
                String::class.java,
            ).apply { isAccessible = true }
            .invoke(null, sectionName, rowCount, targetRows, fallback) as String

    private fun projectListScreenKt(): Class<*> =
        Class.forName("com.finnvek.knittools.ui.screens.project.ProjectListScreenKt")

    private companion object {
        private const val PROJECT_LIST_VIEW_MODEL =
            "app/src/main/java/com/finnvek/knittools/ui/screens/project/ProjectListViewModel.kt"
        private const val PROJECT_LIST_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/project/ProjectListScreen.kt"
    }
}
