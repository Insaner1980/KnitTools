package com.finnvek.knittools.ai

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths

class AiQuotaSourceTest {
    @Test
    fun `classic voice refunds reserved quota when Gemini interpretation is unknown`() {
        val source = readSource("app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterViewModel.kt")

        assertTrue(source.contains("if (!aiQuotaManager.tryReserveVoiceCall())"))
        assertTrue(source.contains("if (action == AiVoiceAction.Unknown)"))
        assertTrue(source.contains("aiQuotaManager.refundReservedVoiceCall()"))
    }

    private fun readSource(relativePath: String): String {
        val start = Paths.get("").toAbsolutePath()
        val root =
            generateSequence(start) { it.parent }
                .first { Files.exists(it.resolve("settings.gradle.kts")) }
        return Files
            .readAllBytes(root.resolve(relativePath))
            .decodeToString()
            .replace("\r\n", "\n")
    }
}
