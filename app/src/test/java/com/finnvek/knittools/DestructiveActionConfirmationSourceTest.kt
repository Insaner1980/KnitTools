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

    @Test
    fun `ravelry disconnect requires confirmation`() {
        val source = ProjectSourceFiles.read(RAVELRY_ACCOUNT_HEADER)

        assertTrue(source.contains("var showDisconnectDialog by rememberSaveable"))
        assertTrue(source.contains("showDisconnectDialog = true"))
        assertTrue(source.contains("if (showDisconnectDialog)"))
        assertTrue(source.contains("message = stringResource(R.string.ravelry_disconnect_confirm)"))
        assertTrue(source.contains("isDestructive = true"))
    }

    private companion object {
        private const val MULTI_COUNTER_COMPONENTS =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/MultiCounterComponents.kt"
        private const val RAVELRY_ACCOUNT_HEADER =
            "app/src/main/java/com/finnvek/knittools/ui/screens/ravelry/RavelryAccountHeader.kt"
    }
}
