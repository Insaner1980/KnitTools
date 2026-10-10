package com.finnvek.knittools.ui

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertTrue
import org.junit.Test

class ComposeHotPathStabilitySourceTest {
    @Test
    fun `hot list state wrappers declare immutable contract`() {
        val projectList = ProjectSourceFiles.read(PROJECT_LIST_SCREEN)

        assertTrue(projectList.contains("@Immutable\ndata class ProjectListContentState("))
    }

    private companion object {
        private const val PROJECT_LIST_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/project/ProjectListScreen.kt"
    }
}
