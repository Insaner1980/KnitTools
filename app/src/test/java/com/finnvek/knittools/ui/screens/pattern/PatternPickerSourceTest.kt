package com.finnvek.knittools.ui.screens.pattern

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertTrue
import org.junit.Test

class PatternPickerSourceTest {
    @Test
    fun `pattern picker orders saved patterns import photo and continue actions`() {
        val picker = ProjectSourceFiles.read(PATTERN_PICKER)
        val strings = ProjectSourceFiles.read(STRINGS)

        val savedIndex = picker.indexOf("R.string.pattern_picker_saved_patterns")
        val importIndex = picker.indexOf("R.string.pattern_picker_import_pdf")
        val photoIndex = picker.indexOf("R.string.pattern_picker_camera_scan")
        val continueIndex = picker.indexOf("R.string.pattern_picker_continue_without_pattern")

        assertTrue(savedIndex >= 0)
        assertTrue(importIndex > savedIndex)
        assertTrue(photoIndex > importIndex)
        assertTrue(continueIndex > photoIndex)
        assertTrue(strings.contains("""<string name="pattern_picker_import_pdf">"""))
        assertTrue(strings.contains("""<string name="pattern_picker_continue_without_pattern">"""))
    }

    private companion object {
        private const val PATTERN_PICKER =
            "app/src/main/java/com/finnvek/knittools/ui/screens/pattern/PatternPickerSheet.kt"
        private const val STRINGS = "app/src/main/res/values/strings.xml"
    }
}
