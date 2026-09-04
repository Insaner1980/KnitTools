package com.finnvek.knittools.ui.screens.counter

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CounterWorkspacePolishSourceTest {
    @Test
    fun `extra counters stack the name above the centered control row with visible overflow actions`() {
        val source = ProjectSourceFiles.read(MULTI_COUNTER_COMPONENTS)

        assertTrue(source.contains("maxLines = 2"))
        assertTrue(source.contains("fontSize = 17.sp"))
        assertTrue(source.contains("fontWeight = FontWeight.SemiBold"))
        assertTrue(source.contains("textAlign = TextAlign.Center"))
        assertTrue(source.contains("Icons.Filled.MoreVert"))
        assertTrue(source.contains("contentDescription = stringResource(R.string.counter_actions, counterName)"))
        assertTrue(source.contains("CounterOverflowMenu("))
        assertFalse(source.contains("Kontekstivalikko pitkällä painalluksella"))
    }

    @Test
    fun `extra counter steppers use counter specific accessibility labels`() {
        val source = ProjectSourceFiles.read(MULTI_COUNTER_COMPONENTS)

        assertTrue(source.contains("R.string.counter_decrease_named"))
        assertTrue(source.contains("R.string.counter_increase_named"))
        assertTrue(source.contains("counter.name"))
    }

    @Test
    fun `section action and stitch tracker context stay lightweight but explicit`() {
        val workspace = ProjectSourceFiles.read(COUNTER_WORKSPACE_SECTIONS)
        val projectCards = ProjectSourceFiles.read(COUNTER_PROJECT_CONTENT_CARDS)
        val stitchCounter = ProjectSourceFiles.read(STITCH_COUNTER)

        assertTrue(workspace.contains("color = MaterialTheme.colorScheme.secondary"))
        assertTrue(projectCards.contains("color = MaterialTheme.colorScheme.secondary"))
        assertTrue(workspace.contains("WorkspaceSectionAction("))
        assertTrue(workspace.contains("Icons.Filled.Add"))
        assertTrue(workspace.contains("R.string.stitch_tracker_label"))
        assertTrue(stitchCounter.contains("label: String"))
    }

    @Test
    fun `stitch tracker uses named actions and a large font layout`() {
        val stitchCounter = ProjectSourceFiles.read(STITCH_COUNTER)

        assertTrue(stitchCounter.contains("R.string.counter_decrease_named, label"))
        assertTrue(stitchCounter.contains("R.string.counter_increase_named, label"))
        assertTrue(stitchCounter.contains("LocalDensity.current.fontScale >= 1.5f"))
        assertTrue(stitchCounter.contains("if (useLargeFontLayout)"))
        assertTrue(stitchCounter.contains("Column("))
    }

    private companion object {
        const val MULTI_COUNTER_COMPONENTS =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/MultiCounterComponents.kt"
        const val COUNTER_WORKSPACE_SECTIONS =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterWorkspaceSections.kt"
        const val COUNTER_PROJECT_CONTENT_CARDS =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterProjectContentCards.kt"
        const val STITCH_COUNTER =
            "app/src/main/java/com/finnvek/knittools/ui/components/StitchCounter.kt"
    }
}
