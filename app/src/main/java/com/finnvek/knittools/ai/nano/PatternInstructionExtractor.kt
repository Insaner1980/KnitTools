package com.finnvek.knittools.ai.nano

import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.GenAiException
import com.google.mlkit.genai.prompt.Generation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Locale
import javax.inject.Inject
import kotlin.math.min
import kotlin.math.pow

class PatternInstructionExtractor
    @Inject
    constructor() {
        private val cacheMutex = Mutex()
        private val instructionCache = mutableMapOf<InstructionCacheKey, String?>()
        private var promptRunnerOverride: (suspend (String, Int) -> String?)? = null

        internal constructor(
            promptRunner: suspend (String, Int) -> String?,
        ) : this() {
            promptRunnerOverride = promptRunner
        }

        suspend fun getInstruction(
            patternUri: String,
            pageIndex: Int,
            rowNumber: Int,
            pageText: String,
        ): String? {
            if (pageText.isBlank()) return null
            if (!NanoAvailability.isUsable()) return null
            if (!pageContainsRow(pageText, rowNumber)) return null

            val key = InstructionCacheKey(patternUri = patternUri, pageIndex = pageIndex, rowNumber = rowNumber)
            cacheMutex.withLock {
                if (instructionCache.containsKey(key)) {
                    return instructionCache[key]
                }
            }

            val instruction = (promptRunnerOverride ?: ::queryNanoInstruction)(pageText, rowNumber)
            cacheMutex.withLock {
                instructionCache[key] = instruction
            }
            return instruction
        }

        suspend fun clearPattern(patternUri: String?) {
            if (patternUri == null) {
                cacheMutex.withLock { instructionCache.clear() }
                return
            }
            cacheMutex.withLock {
                instructionCache.keys.removeAll { it.patternUri == patternUri }
            }
        }

        private suspend fun queryNanoInstruction(
            pageText: String,
            rowNumber: Int,
        ): String? {
            val model =
                try {
                    Generation.getClient()
                } catch (_: Exception) {
                    return null
                }

            return try {
                ensureFeatureReady(model)
                val rawResponse = runWithRetry(model, buildPrompt(pageText, rowNumber))
                normalizeInstruction(rowNumber, rawResponse)
            } catch (_: GenAiException) {
                null
            } catch (_: Exception) {
                null
            } finally {
                model.close()
            }
        }

        private suspend fun ensureFeatureReady(model: com.google.mlkit.genai.prompt.GenerativeModel) {
            when (model.checkStatus()) {
                FeatureStatus.DOWNLOADABLE, FeatureStatus.DOWNLOADING -> model.download().firstOrNull()
                FeatureStatus.AVAILABLE -> Unit
                else -> error("Feature unavailable")
            }
        }

        private suspend fun runWithRetry(
            model: com.google.mlkit.genai.prompt.GenerativeModel,
            prompt: String,
        ): String {
            var lastException: GenAiException? = null
            repeat(MAX_RETRIES) { attempt ->
                try {
                    val response = model.generateContent(prompt)
                    return response.candidates
                        .firstOrNull()
                        ?.text
                        .orEmpty()
                } catch (error: GenAiException) {
                    lastException = error
                    val message = error.message?.lowercase(Locale.ROOT).orEmpty()
                    if (message.contains("busy") || message.contains("quota")) {
                        val delayMs = min(BASE_DELAY_MS * 2.0.pow(attempt).toLong(), MAX_DELAY_MS)
                        delay(delayMs)
                    } else {
                        throw error
                    }
                }
            }
            throw lastException ?: error("Max retries exceeded")
        }

        internal fun buildPrompt(
            pageText: String,
            rowNumber: Int,
        ): String =
            """
            You are a knitting pattern reader. Given the OCR text from one pattern page and a row number, extract the instruction for that exact row.

            Rules:
            - Return only the instruction text for the requested row.
            - Preserve knitting abbreviations exactly as written.
            - If the row is not present on this page, return exactly NOT_FOUND.
            - If the instruction spans multiple lines, join them with a single space.
            - Do not add explanations, labels, markdown, or quotes.

            Page text:
            ${pageText.take(MAX_PAGE_TEXT_CHARS)}

            Row number: $rowNumber
            """.trimIndent()

        internal fun normalizeInstruction(
            rowNumber: Int,
            rawResponse: String?,
        ): String? {
            val compact =
                rawResponse
                    ?.replace("\r", "\n")
                    ?.lines()
                    ?.joinToString(separator = " ") { it.trim() }
                    ?.replace(Regex("\\s+"), " ")
                    ?.trim()
                    .orEmpty()
            if (compact.isBlank()) return null
            if (compact.equals(NOT_FOUND, ignoreCase = true)) return null

            val withoutPrefix =
                compact
                    .replace(
                        Regex(
                            pattern = "^(?:row|rnd|round)\\s*$rowNumber\\s*[:.\\-]?\\s*",
                            option = RegexOption.IGNORE_CASE,
                        ),
                        "",
                    ).trim()

            return withoutPrefix.takeIf { candidate ->
                candidate.isNotBlank() &&
                    !candidate.equals(NOT_FOUND, ignoreCase = true) &&
                    !candidate.contains("instruction not found", ignoreCase = true)
            }
        }

        internal fun pageContainsRow(
            pageText: String,
            rowNumber: Int,
        ): Boolean =
            rowNumberPattern.findAll(pageText).any { match ->
                match.groupValues[1].toIntOrNull() == rowNumber
            }

        private val rowNumberPattern = Regex("""(?:Row|Rnd|Round)\s+(\d+)""", RegexOption.IGNORE_CASE)

        private data class InstructionCacheKey(
            val patternUri: String,
            val pageIndex: Int,
            val rowNumber: Int,
        )

        private companion object {
            private const val BASE_DELAY_MS = 300L
            private const val MAX_DELAY_MS = 2_000L
            private const val MAX_PAGE_TEXT_CHARS = 16_000
            private const val MAX_RETRIES = 2
            private const val NOT_FOUND = "NOT_FOUND"
        }
    }
