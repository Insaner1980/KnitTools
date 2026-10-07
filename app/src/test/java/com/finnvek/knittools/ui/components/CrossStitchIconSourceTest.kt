package com.finnvek.knittools.ui.components

import com.finnvek.knittools.ProjectSourceFiles
import com.finnvek.knittools.ui.screens.counter.ProjectContentCardKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

/** Projektin sisällön kuvakkeet ovat ristipistoja kaikkialla, eivät yleisiä Material-ikoneita. */
class CrossStitchIconSourceTest {
    @Test
    fun `every glyph fits the stitch grid and uses only stitch characters`() {
        CrossStitchGlyph.entries.forEach { glyph ->
            assertTrue(glyph.name, glyph.rows.size <= GRID_LIMIT)
            glyph.rows.forEach { row ->
                assertTrue(glyph.name, row.length <= GRID_LIMIT)
                assertTrue(glyph.name, row.all { it in "Xo." })
            }
        }
    }

    @Test
    fun `counter tiles and project overview rows share the cross stitch glyphs`() {
        val tiles = ProjectSourceFiles.read("$SCREENS/counter/CounterProjectContentCards.kt")
        val sections = ProjectSourceFiles.read("$SCREENS/project/ProjectOverviewSections.kt")
        val linkRow = ProjectSourceFiles.read("$COMPONENTS/OverviewSection.kt")

        assertTrue(tiles.contains("CrossStitchIcon("))
        assertFalse(tiles.contains("Icons.Outlined."))
        assertTrue(linkRow.contains("glyph: CrossStitchGlyph?"))
        assertTrue(sections.contains("CrossStitchGlyph.PATTERN"))
        assertTrue(sections.contains("CrossStitchGlyph.REMINDER"))
        assertFalse(sections.contains("Icons.Outlined."))
        assertEquals(
            CrossStitchGlyph.entries.toSet(),
            ProjectContentCardKind.entries.map { it.glyph }.toSet(),
        )
    }

    @Test
    fun `photo empty states and yarn estimator use own visuals and text instead of generic icons`() {
        val gallery = ProjectSourceFiles.read("$SCREENS/counter/PhotoGalleryScreen.kt")
        val allPhotos = ProjectSourceFiles.read("$SCREENS/library/AllPhotosScreen.kt")
        val estimator = ProjectSourceFiles.read("$SCREENS/yarn/YarnEstimatorScreen.kt")

        listOf(gallery, allPhotos).forEach { screen ->
            assertTrue(screen.contains("CrossStitchEmptyState("))
            assertFalse(screen.contains("camera_icon"))
        }
        assertFalse(Files.exists(ProjectSourceFiles.file(CAMERA_DRAWABLE)))
        assertTrue(estimator.contains("R.string.choose_from_my_yarn"))
        assertFalse(estimator.contains("Inventory2"))
    }

    private companion object {
        const val GRID_LIMIT = 9
        const val SCREENS = "app/src/main/java/com/finnvek/knittools/ui/screens"
        const val COMPONENTS = "app/src/main/java/com/finnvek/knittools/ui/components"
        const val CAMERA_DRAWABLE = "app/src/main/res/drawable-nodpi/camera_icon.webp"
    }
}
