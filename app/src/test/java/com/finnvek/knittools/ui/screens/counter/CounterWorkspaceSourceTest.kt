package com.finnvek.knittools.ui.screens.counter

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class CounterWorkspaceSourceTest {
    @Test
    fun `counter content is implemented as one workspace lazy column`() {
        val counterScreen = ProjectSourceFiles.read(COUNTER_SCREEN)
        val workspaceFile = ProjectSourceFiles.file(COUNTER_WORKSPACE_SECTIONS)

        assertTrue("CounterWorkspaceSections.kt is missing", Files.exists(workspaceFile))

        val workspace = ProjectSourceFiles.read(COUNTER_WORKSPACE_SECTIONS)
        assertTrue(counterScreen.contains("CounterWorkspace("))
        assertFalse(counterScreen.contains("private fun CounterScreenContent("))
        assertTrue(workspace.contains("LazyColumn("))
        assertTrue(workspace.contains("contentPadding ="))
        assertTrue(workspace.contains("PaddingValues("))
        assertTrue(workspace.contains(".padding(scaffoldPadding)"))
        assertFalse(workspace.contains("""key = "project-header""""))
        assertTrue(workspace.indexOf("""key = "counter-hero"""") < workspace.indexOf("ProjectContentCards("))
        assertTrue(workspace.contains("verticalArrangement = Arrangement.spacedBy"))
        assertTrue(workspace.contains("key = { counter -> counter.id }"))
    }

    @Test
    fun `top bar only keeps navigation and more while daily project actions move into content cards`() {
        val counterScreen = ProjectSourceFiles.read(COUNTER_SCREEN)
        val contentCardsFile = ProjectSourceFiles.file(COUNTER_PROJECT_CONTENT_CARDS)

        assertTrue("CounterProjectContentCards.kt is missing", Files.exists(contentCardsFile))

        val workspace = ProjectSourceFiles.read(COUNTER_WORKSPACE_SECTIONS)
        val contentCards = ProjectSourceFiles.read(COUNTER_PROJECT_CONTENT_CARDS)
        val strings = ProjectSourceFiles.read(STRINGS)

        assertFalse(counterScreen.contains("canUseProgressPhotos ="))
        assertFalse(counterScreen.contains("showPatternIcon ="))
        assertFalse(counterScreen.contains("Icons.Filled.CameraAlt"))
        assertFalse(counterScreen.contains("Icons.Filled.Description"))
        assertFalse(workspace.contains("CounterQuickActions("))
        assertFalse(workspace.contains("ProjectInfoSection("))
        assertTrue(workspace.contains("ProjectContentCards("))

        listOf(
            "project_content_open_pattern",
            "project_content_attach_pattern",
            "project_content_yarn",
            "project_content_notes",
            "project_content_photos",
            "reminders",
        ).forEach { key ->
            assertTrue("Project content string missing: $key", strings.contains("""<string name="$key">"""))
            assertTrue("Project content source does not reference: $key", contentCards.contains("R.string.$key"))
        }
    }

    @Test
    fun `counter header keeps pattern details out of the first viewport`() {
        val counterScreen = ProjectSourceFiles.read(COUNTER_SCREEN)
        val workspace = ProjectSourceFiles.read(COUNTER_WORKSPACE_SECTIONS)
        val contentCards = ProjectSourceFiles.read(COUNTER_PROJECT_CONTENT_CARDS)

        assertTrue(counterScreen.contains("TopAppBar("))
        assertTrue(counterScreen.contains("state.projectName.ifEmpty"))
        assertFalse(workspace.contains("PatternHeaderRow"))
        assertFalse(workspace.contains("project_header_pattern_attached"))
        assertFalse(workspace.contains("text = attachedPatternName,"))
        assertFalse(contentCards.contains("two_sleeves_one_promise.pdf"))
        assertFalse(contentCards.contains("Ravelry"))
        assertFalse(contentCards.contains(" · "))
    }

    @Test
    fun `counter card model does not expose preview fields or nested lazy grids`() {
        val workspace = ProjectSourceFiles.read(COUNTER_WORKSPACE_SECTIONS)
        val contentCards = ProjectSourceFiles.read(COUNTER_PROJECT_CONTENT_CARDS)

        listOf(
            "bodyText",
            "bodyRes",
            "photoCount",
            "reminderRow",
            "reminderMessage",
            "project_content_next_reminder",
            "project_content_reminder_body",
            "KeyboardArrowRight",
            "LazyVerticalGrid",
        ).forEach { forbidden ->
            assertFalse("Counter project cards should not contain $forbidden", contentCards.contains(forbidden))
        }
        assertTrue(contentCards.contains("aspectRatio(1f)"))
        assertTrue(contentCards.contains("chunked(2)"))
        assertFalse(workspace.contains("""key = "counter-buttons""""))
        assertTrue(workspace.indexOf("ProjectContentCards(") < workspace.indexOf("""key = "stitch-tracker""""))
    }

    @Test
    fun `counter route keeps bottom navigation visible`() {
        val navGraph = ProjectSourceFiles.read(NAV_GRAPH)

        val hiddenRoutesBlock =
            navGraph
                .substringAfter("private val HIDE_BOTTOM_BAR_ROUTES =")
                .substringBefore(")")
        assertFalse(hiddenRoutesBlock.contains("Screen.Counter.route"))
    }

    @Test
    fun `localized strings no longer carry counter preview card copy`() {
        STRING_FILES.forEach { stringsFile ->
            val strings = ProjectSourceFiles.read(stringsFile)
            assertTrue(
                "Add pattern card label missing in $stringsFile",
                strings.contains("""<string name="project_content_attach_pattern">"""),
            )
            assertFalse(strings.contains("""<string name="project_content_attach_pattern_body">"""))
            assertFalse(strings.contains("""<string name="project_content_add_yarn_body">"""))
            assertFalse(strings.contains("""<string name="project_content_add_note_body">"""))
            assertFalse(strings.contains("""<string name="project_content_add_photo_body">"""))
            assertFalse(strings.contains("""<string name="project_content_next_reminder">"""))
            assertFalse(strings.contains("""<string name="project_content_reminder_body">"""))
        }
    }

    private companion object {
        private const val COUNTER_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterScreen.kt"
        private const val COUNTER_WORKSPACE_SECTIONS =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterWorkspaceSections.kt"
        private const val COUNTER_PROJECT_CONTENT_CARDS =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterProjectContentCards.kt"
        private const val NAV_GRAPH =
            "app/src/main/java/com/finnvek/knittools/ui/navigation/NavGraph.kt"
        private const val STRINGS = "app/src/main/res/values/strings.xml"
        private val STRING_FILES =
            listOf(
                STRINGS,
                "app/src/main/res/values-da/strings.xml",
                "app/src/main/res/values-de/strings.xml",
                "app/src/main/res/values-es/strings.xml",
                "app/src/main/res/values-fi/strings.xml",
                "app/src/main/res/values-fr/strings.xml",
                "app/src/main/res/values-it/strings.xml",
                "app/src/main/res/values-nb/strings.xml",
                "app/src/main/res/values-nl/strings.xml",
                "app/src/main/res/values-pt/strings.xml",
                "app/src/main/res/values-sv/strings.xml",
            )
    }
}
