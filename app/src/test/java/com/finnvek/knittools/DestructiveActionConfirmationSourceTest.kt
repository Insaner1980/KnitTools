package com.finnvek.knittools

import org.junit.Assert.assertTrue
import org.junit.Test

class DestructiveActionConfirmationSourceTest {
    @Test
    fun `extra counter reset requires confirmation`() {
        val source = ProjectSourceFiles.read(MULTI_COUNTER_COMPONENTS)

        assertTrue(source.contains("var showResetDialog by rememberSaveable(counter.id)"))
        assertTrue(source.contains("onReset = { showResetDialog = true }"))
        assertTrue(source.contains("if (showResetDialog)"))
        assertTrue(source.contains("message = stringResource(R.string.reset_counter_message)"))
        assertTrue(source.contains("isDestructive = true"))
    }

    private companion object {
        private const val MULTI_COUNTER_COMPONENTS =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/MultiCounterComponents.kt"
    }
}
