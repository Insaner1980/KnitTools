package com.finnvek.knittools.widget

import androidx.compose.ui.unit.dp
import com.finnvek.knittools.ProjectSourceFiles
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.CraftType
import com.finnvek.knittools.domain.model.MainCounterLabelType
import com.finnvek.knittools.ui.theme.CounterWidgetDimens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CounterWidgetSourceTest {
    @Test
    fun `small widget counts with one plus button and grows the number when stacked`() {
        val smallWidget = widgetSection("private fun SmallWidget(", "private fun MediumWidget(")

        assertTrue(smallWidget.contains("LocalSize.current.height >= CounterWidget.STACKED_SMALL_SIZE.height"))
        assertTrue(smallWidget.contains("CounterWidgetDimens.StackedCountSize"))
        assertTrue(smallWidget.contains("action = WidgetCountAction.INCREMENT"))
        assertTrue(!smallWidget.contains("WidgetCountAction.DECREMENT"))
    }

    @Test
    fun `medium and large widgets offer both count actions`() {
        val mediumWidget = widgetSection("private fun MediumWidget(", "private fun LargeWidget(")
        val largeWidget = widgetSection("private fun LargeWidget(", "private fun widgetVerticalPadding(")

        listOf(mediumWidget, largeWidget).forEach { section ->
            assertTrue(section.contains("action = WidgetCountAction.DECREMENT"))
            assertTrue(section.contains("action = WidgetCountAction.INCREMENT"))
            assertTrue(section.contains("WidgetTargetBar(data = data)"))
        }
    }

    @Test
    fun `widget count actions keep accessible touch size and localized names`() {
        val widget = ProjectSourceFiles.read(COUNTER_WIDGET)

        listOf(
            CounterWidgetDimens.ButtonSize,
            CounterWidgetDimens.LargeButtonSize,
            CounterWidgetDimens.SmallButtonTouchSize,
        ).forEach { size -> assertTrue(size >= 48.dp) }
        assertTrue(widget.contains("WidgetCountAction.DECREMENT -> R.string.counter_decrease"))
        assertTrue(widget.contains("WidgetCountAction.INCREMENT -> R.string.counter_increase"))
        assertTrue(widget.contains("contentDescription = actionDescription,"))
    }

    @Test
    fun `widget buttons reuse the counter screen 3D images`() {
        val button = widgetSection("private fun WidgetCountButton(", "private enum class WidgetCountAction")

        assertTrue(button.contains("WidgetCountAction.DECREMENT -> R.drawable.counter_minus_button"))
        assertTrue(button.contains("WidgetCountAction.INCREMENT -> R.drawable.counter_plus_button"))
        assertTrue(!button.contains("Text("))
    }

    @Test
    fun `widget surface is frameless with a yarn colored knit texture`() {
        val card = widgetSection("private fun WidgetCard(", "private fun systemWidgetCorners(")

        assertTrue(card.contains("ImageProvider(R.drawable.widget_knit_texture)"))
        assertTrue(card.contains("ColorFilter.tint(widgetYarnColor(projectId))"))
        assertTrue(card.contains(".appWidgetBackground()"))
        assertTrue(!card.contains("GlanceTheme.colors.surfaceVariant"))
    }

    @Test
    fun `project labels use the high contrast surface text color`() {
        val widget = ProjectSourceFiles.read(COUNTER_WIDGET)

        assertTrue(widget.countOccurrences("color = GlanceTheme.colors.onSurface,") == 2)
        assertTrue(!widget.contains("color = GlanceTheme.colors.tertiary,"))
    }

    @Test
    fun `widget data carries the same resolved unit as the counter`() {
        val crochet =
            CounterProject(id = 1, craftType = CraftType.CROCHET, mainCounterLabelType = MainCounterLabelType.CUSTOM)
        val custom =
            CounterProject(
                id = 2,
                mainCounterLabelType = MainCounterLabelType.CUSTOM,
                mainCounterCustomLabel = " Pattern ",
            )

        assertEquals(MainCounterLabelType.ROUNDS, crochet.toWidgetData().labelType)
        assertNull(crochet.toWidgetData().customLabel)
        assertEquals(MainCounterLabelType.CUSTOM, custom.toWidgetData().labelType)
        assertEquals("Pattern", custom.toWidgetData().customLabel)
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
        assertTrue(widget.contains("val access by accessFlow.collectAsState(initialAccess)"))
        assertTrue(widget.contains("LaunchedEffect(access)"))
        assertTrue(widget.contains("resolveInitialWidgetData(context, id, entryPoint, data)"))
        assertTrue(!widget.contains("val initialWidgetData"))
    }

    private fun widgetSection(
        start: String,
        end: String,
    ): String =
        ProjectSourceFiles
            .read(COUNTER_WIDGET)
            .substringAfter(start)
            .substringBefore(end)

    private companion object {
        const val COUNTER_WIDGET =
            "app/src/main/java/com/finnvek/knittools/widget/CounterWidget.kt"
    }
}

private fun String.countOccurrences(value: String): Int = windowed(value.length).count { it == value }
