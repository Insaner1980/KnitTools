package com.finnvek.knittools.ai.live

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceFunctionDeclarationsSourceTest {
    @Test
    fun `live voice tool declarations initialize`() {
        assertNotNull(VoiceFunctionDeclarations.tool)
    }

    @Test
    fun `gemini live exposes only non mutating project tools`() {
        val source = ProjectSourceFiles.read(VOICE_FUNCTION_DECLARATIONS)

        listOf(
            "increment",
            "decrement",
            "undo",
            "stitch_increment",
            "stitch_decrement",
            "add_note",
            "dismiss_reminder",
            "add_reminder",
            "counter_change",
            "set_section",
            "configure_counter",
            "page_navigation",
            "generate_summary",
        ).forEach { toolName ->
            assertFalse(source.contains("name = \"$toolName\""))
        }

        assertTrue(source.contains("name = \"query_project\""))
        assertTrue(source.contains("name = \"help\""))
    }

    private companion object {
        private const val VOICE_FUNCTION_DECLARATIONS =
            "app/src/main/java/com/finnvek/knittools/ai/live/VoiceFunctionDeclarations.kt"
    }
}
