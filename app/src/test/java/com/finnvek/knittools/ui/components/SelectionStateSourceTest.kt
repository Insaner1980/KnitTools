package com.finnvek.knittools.ui.components

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Valittu tila näyttää samalta kaikkialla: tasainen oranssi valinta, himmeä valitsematon. */
class SelectionStateSourceTest {
    @Test
    fun `selected segment is flat primary like other orange buttons`() {
        val toggle = ProjectSourceFiles.read(SEGMENTED_TOGGLE)

        assertTrue(toggle.contains("Modifier.background(MaterialTheme.colorScheme.primary)"))
        assertFalse(toggle.contains("Brush.linearGradient("))
    }

    private companion object {
        const val SEGMENTED_TOGGLE = "app/src/main/java/com/finnvek/knittools/ui/components/SegmentedToggle.kt"
    }
}
