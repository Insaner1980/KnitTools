package com.finnvek.knittools.ui.screens.pattern

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PatternReaderModeSourceTest {
    @Test
    fun `downloaded pdf can be opened or shared straight into the app`() {
        val manifest = ProjectSourceFiles.read(MANIFEST)
        val pdfFilter = manifest.substringAfter("<!-- Ladattu PDF-ohje").substringBefore("</intent-filter>")

        assertTrue(pdfFilter.contains("android.intent.action.VIEW"))
        assertTrue(pdfFilter.contains("android.intent.action.SEND"))
        assertTrue(pdfFilter.contains("android:mimeType=\"application/pdf\""))
    }

    @Test
    fun `reader shows annotation tools only in markup mode`() {
        val viewer = ProjectSourceFiles.read(PATTERN_VIEWER)

        // Lukutilassa ohje täyttää näytön; tasot ja vienti ovat valikossa.
        assertTrue(viewer.contains("if (state.patternUri != null && state.annotationMode)"))
        assertTrue(viewer.contains("R.string.pattern_mark_up"))
        assertTrue(viewer.contains("PatternAnnotationLayersSheet("))
        assertFalse(viewer.substringAfter("private fun PatternViewerContent(").contains("PatternAnnotationLayerPanel("))
    }

    @Test
    fun `bookmarks live in the bottom bar instead of the overflow menu`() {
        val viewer = ProjectSourceFiles.read(PATTERN_VIEWER)
        val overflowMenu =
            viewer
                .substringAfter("private fun PatternViewerOverflowMenu(")
                .substringBefore("private fun PatternReadingLineMenuItem(")

        assertFalse(overflowMenu.contains("R.string.pattern_bookmarks"))
        assertTrue(viewer.contains("onClick = actions.onOpenBookmarks"))
    }

    @Test
    fun `mark up is offered only after the page has been rendered`() {
        val viewer = ProjectSourceFiles.read(PATTERN_VIEWER)

        assertTrue(viewer.contains("val canAnnotate: Boolean get() = rendererError == null && renderedBitmap != null"))
        assertFalse(viewer.contains("canAnnotate = patternUri != null && renderState.rendererError == null"))
    }

    private companion object {
        const val MANIFEST = "app/src/main/AndroidManifest.xml"
        const val PATTERN_VIEWER =
            "app/src/main/java/com/finnvek/knittools/ui/screens/pattern/PatternViewerScreen.kt"
    }
}
