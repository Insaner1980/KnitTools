package com.finnvek.knittools.ui.screens.counter

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CounterHapticSourceTest {
    @Test
    fun `extra counter actions own haptic feedback exactly once`() {
        val counterScreen = ProjectSourceFiles.read(COUNTER_SCREEN)
        val counterWorkspace = ProjectSourceFiles.read(COUNTER_WORKSPACE)
        val multiCounterComponents = ProjectSourceFiles.read(MULTI_COUNTER_COMPONENTS)

        assertTrue(
            Regex("""onIncrementCounter = \{ counter ->\s+performHaptic\(\)\s+viewModel\.incrementProjectCounter""")
                .containsMatchIn(counterScreen),
        )
        assertTrue(
            Regex("""onDecrementCounter = \{ counter ->\s+performHaptic\(\)\s+viewModel\.decrementProjectCounter""")
                .containsMatchIn(counterScreen),
        )
        assertFalse(counterWorkspace.contains("performHaptic = actions.performHaptic"))
        assertFalse(multiCounterComponents.contains("performHaptic"))
    }

    private companion object {
        private const val COUNTER_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterScreen.kt"
        private const val COUNTER_WORKSPACE =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterWorkspaceSections.kt"
        private const val MULTI_COUNTER_COMPONENTS =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/MultiCounterComponents.kt"
    }
}
