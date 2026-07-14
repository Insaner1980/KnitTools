package com.finnvek.knittools.ui.screens.counter

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkedCounterSourceTest {
    @Test
    fun `add counter draft carries linked flag except for repeat sections`() {
        val source = ProjectSourceFiles.read(MULTI_COUNTER_COMPONENTS)

        assertTrue(source.contains("val linkedToMainCounter: Boolean = false"))
        assertTrue(source.contains("linkedToMainCounter = params.linkedToMainCounter && !params.isRepeatSection"))
    }

    @Test
    fun `add counter dialog exposes linked counter switch with plain description`() {
        val source = ProjectSourceFiles.read(MULTI_COUNTER_COMPONENTS)

        assertTrue(source.contains("Switch("))
        assertTrue(source.contains("R.string.linked_counter"))
        assertTrue(source.contains("R.string.linked_counter_description"))
        assertTrue(source.contains("enabled = !state.isRepeatSection"))
    }

    private companion object {
        const val MULTI_COUNTER_COMPONENTS =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/MultiCounterComponents.kt"
    }
}
