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
            val limit = if (glyph == CrossStitchGlyph.YARN) YARN_GRID_LIMIT else GRID_LIMIT
            assertTrue(glyph.name, glyph.rows.size <= limit)
            glyph.rows.forEach { row ->
                assertTrue(glyph.name, row.length <= limit)
                assertTrue(glyph.name, row.all { it in "Xo." })
            }
        }
    }

    @Test
    fun `denser glyphs scale their own grid instead of overflowing the icon`() {
        val icon = ProjectSourceFiles.read("$COMPONENTS/CrossStitchIcon.kt")

        assertTrue(icon.contains("maxOf(columns, glyph.rows.size) + CrossStitchDimens.GridMargin"))
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
            setOf(
                CrossStitchGlyph.PATTERN,
                CrossStitchGlyph.YARN,
                CrossStitchGlyph.NOTES,
                CrossStitchGlyph.PHOTOS,
                CrossStitchGlyph.REMINDER,
            ),
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

        // Kerän kaarevat kierrokset eivät erotu 9 ruudun kuviossa; yli 13 ruudun kerä jäi muita pienemmäksi.
        const val YARN_GRID_LIMIT = 13
        const val SCREENS = "app/src/main/java/com/finnvek/knittools/ui/screens"
        const val COMPONENTS = "app/src/main/java/com/finnvek/knittools/ui/components"
        const val CAMERA_DRAWABLE = "app/src/main/res/drawable-nodpi/camera_icon.webp"
    }
}
