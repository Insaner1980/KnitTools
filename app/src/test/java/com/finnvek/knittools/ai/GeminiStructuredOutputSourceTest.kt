package com.finnvek.knittools.ai

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths

class GeminiStructuredOutputSourceTest {
    @Test
    fun `Gemini JSON generation uses Firebase response schema`() {
        val source = readSource("app/src/main/java/com/finnvek/knittools/ai/GeminiAiService.kt")

        assertTrue(source.contains("responseMimeType = \"application/json\""))
        assertTrue(source.contains("responseSchema = schema"))
        assertTrue(source.contains("generateJsonTextForVoice"))
        assertTrue(source.contains("generateJsonFromImage"))
    }

    @Test
    fun `JSON call sites use schema constrained Gemini methods`() {
        assertTrue(
            readSource("app/src/main/java/com/finnvek/knittools/ai/VoiceCommandInterpreter.kt")
                .contains("generateJsonTextForVoice(prompt, RESPONSE_SCHEMA)"),
        )
        assertTrue(
            readSource("app/src/main/java/com/finnvek/knittools/ai/PatternInstructionGemini.kt")
                .contains("generateJsonFromImage(pageBitmap, buildPrompt(rowNumber), RESPONSE_SCHEMA)"),
        )
        assertTrue(
            readSource("app/src/main/java/com/finnvek/knittools/ai/PatternInstructionCombinerGemini.kt")
                .contains("generateJsonFromImage(pageBitmap, buildPrompt(), RESPONSE_SCHEMA)"),
        )
        assertTrue(
            readSource("app/src/main/java/com/finnvek/knittools/ai/YarnLabelGeminiScanner.kt")
                .contains("generateJsonFromImage(bitmap, PROMPT, RESPONSE_SCHEMA)"),
        )
    }

    private fun readSource(path: String): String = Files.readAllBytes(repoRoot().resolve(path)).decodeToString()

    private fun repoRoot() =
        generateSequence(Paths.get("").toAbsolutePath()) { it.parent }
            .first { Files.exists(it.resolve("settings.gradle.kts")) }
}
