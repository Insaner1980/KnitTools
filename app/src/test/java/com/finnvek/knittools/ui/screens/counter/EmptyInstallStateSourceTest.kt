package com.finnvek.knittools.ui.screens.counter

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertFalse
import org.junit.Test

class EmptyInstallStateSourceTest {
    @Test
    fun `empty install does not create an implicit project`() {
        val source = ProjectSourceFiles.read(COUNTER_VIEW_MODEL)

        assertFalse(
            source.contains("repository.createProject(context.getString(R.string.default_project_name))"),
        )
    }

    private companion object {
        const val COUNTER_VIEW_MODEL =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterViewModel.kt"
    }
}
