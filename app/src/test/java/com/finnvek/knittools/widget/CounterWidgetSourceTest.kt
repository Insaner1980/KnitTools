package com.finnvek.knittools.widget

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertTrue
import org.junit.Test

class CounterWidgetSourceTest {
    @Test
    fun `small widget suppresses the optional section name`() {
        val widget = ProjectSourceFiles.read(COUNTER_WIDGET)

        assertTrue(
            widget.contains(
                "WidgetHeader(data = data, fontSize = 12.sp, showSection = false)",
            ),
        )
        assertTrue(widget.contains("if (showSection) {"))
    }

    @Test
    fun `shared widget state wins over stale per-instance state`() {
        val widget = ProjectSourceFiles.read(COUNTER_WIDGET)

        assertTrue(
            widget.contains(
                "candidates = listOf(sharedWidgetData, widgetData)",
            ),
        )
    }

    @Test
    fun `widget rendering observes updated glance state instead of retaining initial snapshot`() {
        val widget = ProjectSourceFiles.read(COUNTER_WIDGET)

        assertTrue(widget.contains("val data = CounterWidgetState.fromPreferences(context, prefs)"))
        assertTrue(widget.contains("if (isPro) resolveInitialWidgetData(context, id, entryPoint, widgetData)"))
        assertTrue(!widget.contains("val initialWidgetData"))
    }

    private companion object {
        const val COUNTER_WIDGET =
            "app/src/main/java/com/finnvek/knittools/widget/CounterWidget.kt"
    }
}
