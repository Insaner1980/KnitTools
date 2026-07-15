package com.finnvek.knittools.ui

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FableUiContractSourceTest {
    @Test
    fun `top level navigation distinguishes selection without color alone`() {
        val screen = ProjectSourceFiles.read(SCREEN)
        val bottomBar = ProjectSourceFiles.read(BOTTOM_BAR)

        assertTrue(screen.contains("val selectedIcon: ImageVector"))
        assertTrue(screen.contains("val unselectedIcon: ImageVector"))
        assertTrue(bottomBar.contains("if (selected) destination.selectedIcon else destination.unselectedIcon"))
        assertTrue(bottomBar.contains("indicatorColor = MaterialTheme.knitToolsColors.navBarIndicator"))
    }

    @Test
    fun `shared top bars expose pinned overlap treatment`() {
        TOP_BAR_FILES.forEach { sourceFile ->
            val source = ProjectSourceFiles.read(sourceFile)
            assertTrue("Pinned scroll behavior missing from $sourceFile", source.contains("pinnedScrollBehavior"))
            assertTrue("Nested scroll connection missing from $sourceFile", source.contains("nestedScroll"))
            assertTrue("Scrolled container missing from $sourceFile", source.contains("scrolledContainerColor"))
        }
    }

    @Test
    fun `project actions isolate delete and use shared modal roles`() {
        val source = ProjectSourceFiles.read(PROJECT_ACTIONS_SHEET)

        assertTrue(source.contains("containerColor = MaterialTheme.knitToolsColors.modalContainer"))
        assertTrue(source.contains("shape = SheetShape"))
        assertTrue(source.contains("Icons.Outlined.Tune"))
        val completeIndex = source.indexOf("R.string.complete_project")
        val dividerIndex = source.indexOf("SectionDivider()", completeIndex)
        val deleteIndex = source.indexOf("R.string.delete_project", completeIndex)
        assertTrue(completeIndex >= 0 && dividerIndex in (completeIndex + 1)..<deleteIndex)
    }

    @Test
    fun `needle table and insights use audited presentation contracts`() {
        val needles = ProjectSourceFiles.read(NEEDLE_SCREEN)
        val insights = ProjectSourceFiles.read(INSIGHTS_SCREEN)

        assertTrue(needles.contains("stickyHeader { HeaderRow() }"))
        assertTrue(needles.contains("Dimens.DataRowMinimumHeight"))
        assertTrue(insights.contains("Icons.Filled.ArrowDropDown"))
        assertTrue(insights.contains("insights_latest_pace_format"))
        assertTrue(insights.contains("chartHeight * 0.5f"))
        assertFalse(insights.contains("InsightChartColors"))
    }

    private companion object {
        const val SCREEN = "app/src/main/java/com/finnvek/knittools/ui/navigation/Screen.kt"
        const val BOTTOM_BAR = "app/src/main/java/com/finnvek/knittools/ui/navigation/KnitToolsBottomBar.kt"
        const val PROJECT_ACTIONS_SHEET =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/ProjectActionsBottomSheet.kt"
        const val NEEDLE_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/needles/NeedleSizeScreen.kt"
        const val INSIGHTS_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/insights/InsightsScreen.kt"
        val TOP_BAR_FILES =
            listOf(
                "app/src/main/java/com/finnvek/knittools/ui/components/ToolScreenScaffold.kt",
                "app/src/main/java/com/finnvek/knittools/ui/screens/project/ProjectListScreen.kt",
                "app/src/main/java/com/finnvek/knittools/ui/screens/home/HomeScreen.kt",
                "app/src/main/java/com/finnvek/knittools/ui/screens/library/LibraryScreen.kt",
                "app/src/main/java/com/finnvek/knittools/ui/screens/insights/InsightsScreen.kt",
                "app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterScreen.kt",
            )
    }
}
