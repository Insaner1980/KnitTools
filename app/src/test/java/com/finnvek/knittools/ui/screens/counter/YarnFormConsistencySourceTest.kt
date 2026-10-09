package com.finnvek.knittools.ui.screens.counter

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Projektin lankalomakkeet käyttävät samoja osia kuin muut lomakkeet: otsikko, valitsimet ja painikerivi. */
class YarnFormConsistencySourceTest {
    @Test
    fun `yarn usage editor uses the shared title, segmented unit and action row`() {
        val sheet = ProjectSourceFiles.read("$COUNTER/ProjectYarnUsageSheet.kt")

        assertTrue(sheet.contains("SheetTitle("))
        assertTrue(sheet.contains("SegmentedToggle("))
        assertTrue(sheet.contains("itemTestTagPrefix = \"yarn_usage_unit\""))
        assertTrue(sheet.contains("FormSheetActions("))
        assertFalse(sheet.contains("GaugeSelector("))
        assertFalse(sheet.contains("Button(\n                onClick = actions.onSave"))
    }

    @Test
    fun `project yarn note form opens as the shared form sheet`() {
        val sheet = ProjectSourceFiles.read("$COUNTER/YarnManagementSheet.kt")

        assertTrue(sheet.contains("FormSheet(\n        title = stringResource(R.string.add_yarn_to_project)"))
        assertFalse(sheet.contains("CancelButton(onClick = onCancel)"))
    }

    private companion object {
        const val COUNTER = "app/src/main/java/com/finnvek/knittools/ui/screens/counter"
    }
}
