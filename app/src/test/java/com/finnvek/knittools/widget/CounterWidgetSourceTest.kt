package com.finnvek.knittools.widget

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertTrue
import org.junit.Test

class CounterWidgetSourceTest {
    @Test
    fun `small widget uses a horizontal project and count layout`() {
        val widget = ProjectSourceFiles.read(COUNTER_WIDGET)
        val smallWidget =
            widget
                .substringAfter("private fun SmallWidget(")
                .substringBefore("private fun MediumWidget(")

        assertTrue(smallWidget.contains("Row("))
        assertTrue(smallWidget.contains("fontSize = 11.sp"))
        assertTrue(smallWidget.contains("fontSize = 22.sp"))
        assertTrue(!smallWidget.contains("WidgetHeader("))
    }

    @Test
    fun `widget count actions keep accessible touch size and localized names`() {
        val widget = ProjectSourceFiles.read(COUNTER_WIDGET)

        assertTrue(widget.countOccurrences("size = 48.dp") == 4)
        assertTrue(widget.contains("WidgetCountAction.DECREMENT -> R.string.counter_decrease"))
        assertTrue(widget.contains("WidgetCountAction.INCREMENT -> R.string.counter_increase"))
        assertTrue(widget.contains(".semantics { contentDescription = actionDescription }"))
    }

    @Test
    fun `project labels use the high contrast surface text color`() {
        val widget = ProjectSourceFiles.read(COUNTER_WIDGET)

        assertTrue(widget.countOccurrences("color = GlanceTheme.colors.onSurface,") == 5)
        assertTrue(!widget.contains("color = GlanceTheme.colors.tertiary,"))
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

private fun String.countOccurrences(value: String): Int = windowed(value.length).count { it == value }
