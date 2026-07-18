package com.finnvek.knittools.ui.screens.pattern

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PatternAnnotationAccessibilitySourceTest {
    @Test
    fun `every supported locale contains all annotation strings`() {
        val defaultNames = annotationNames(ProjectSourceFiles.read(DEFAULT_STRINGS))

        LOCALES.forEach { locale ->
            val localizedNames =
                annotationNames(ProjectSourceFiles.read("app/src/main/res/$locale/pattern_annotations.xml"))
            assertEquals("Missing annotation strings in $locale", defaultNames, localizedNames)
        }
    }

    @Test
    fun `toolbar uses preference aware haptics and centralized accessible sizes`() {
        val toolbar = ProjectSourceFiles.read(TOOLBAR)
        val tokens = ProjectSourceFiles.read(TOKENS)

        assertTrue(toolbar.contains("LocalHapticFeedback.current"))
        assertTrue(toolbar.contains("PatternAnnotationTokens.TOOL_TOUCH_TARGET"))
        assertTrue(toolbar.contains("PatternAnnotationTokens.TOOLBAR_MAX_HEIGHT"))
        assertTrue(tokens.contains("TOOL_TOUCH_TARGET = 48.dp"))
    }

    @Test
    fun `layer switches and color choices expose named selection semantics`() {
        val toolbar = ProjectSourceFiles.read(TOOLBAR)
        val layerPanel = ProjectSourceFiles.read(LAYER_PANEL)

        assertTrue(layerPanel.contains("contentDescription = title"))
        assertTrue(toolbar.contains(".selectable("))
        assertTrue(toolbar.contains("role = Role.RadioButton"))
        assertTrue(toolbar.contains("stringResource(annotationColorLabel(argb))"))
    }

    @Test
    fun `chart dialog is not rendered without counter options`() {
        val toolbar = ProjectSourceFiles.read(TOOLBAR)

        assertTrue(toolbar.contains("showChartTrackerEditor && state.chartCounterOptions.isNotEmpty()"))
    }

    private fun annotationNames(xml: String): Set<String> =
        NAME_REGEX.findAll(xml).map { match -> match.groupValues[1] }.toSet()

    private companion object {
        const val DEFAULT_STRINGS = "app/src/main/res/values/strings.xml"
        const val TOOLBAR =
            "app/src/main/java/com/finnvek/knittools/ui/screens/pattern/PatternAnnotationToolbar.kt"
        const val LAYER_PANEL =
            "app/src/main/java/com/finnvek/knittools/ui/screens/pattern/PatternAnnotationLayerPanel.kt"
        const val TOKENS = "app/src/main/java/com/finnvek/knittools/ui/theme/PatternAnnotationTokens.kt"
        val NAME_REGEX = Regex("name=\"(pattern_annotations?_[^\"]+)\"")
        val LOCALES =
            listOf(
                "values-da",
                "values-de",
                "values-es",
                "values-fi",
                "values-fr",
                "values-it",
                "values-nb",
                "values-nl",
                "values-pt",
                "values-sv",
            )
    }
}
