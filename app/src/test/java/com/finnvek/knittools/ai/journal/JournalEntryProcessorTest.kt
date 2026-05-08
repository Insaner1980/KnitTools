package com.finnvek.knittools.ai.journal

import com.finnvek.knittools.ai.AiQuotaManager
import com.finnvek.knittools.ai.GeminiAiService
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class JournalEntryProcessorTest {
    private fun build(
        isPro: Boolean = true,
        hasQuota: Boolean = true,
        aiResponse: String? = "Cleaned text.",
    ): Triple<JournalEntryProcessor, AiQuotaManager, GeminiAiService> {
        val pro = mockk<ProManager>()
        every { pro.hasFeature(ProFeature.AI_FEATURES) } returns isPro
        val quota = mockk<AiQuotaManager>(relaxed = true)
        coEvery { quota.hasQuota() } returns hasQuota
        val gemini = mockk<GeminiAiService>()
        coEvery { gemini.generateText(any()) } returns aiResponse
        return Triple(JournalEntryProcessor(gemini, quota, pro), quota, gemini)
    }

    @Test
    fun `success path returns cleaned text and records call`() =
        runTest {
            val (processor, quota, _) = build(aiResponse = "Vaihdoin puikot pienemmäksi.")
            val result = processor.process("vaihdoin puikot pienemmäksi")
            assertTrue(result is JournalProcessResult.Success)
            assertEquals("Vaihdoin puikot pienemmäksi.", (result as JournalProcessResult.Success).cleaned)
            coVerify(exactly = 1) { quota.recordCall() }
        }

    @Test
    fun `non-pro returns NoPro fallback without calling AI`() =
        runTest {
            val (processor, quota, gemini) = build(isPro = false)
            val result = processor.process("typed note")
            assertTrue(result is JournalProcessResult.Fallback)
            assertEquals(
                JournalProcessResult.Fallback.Reason.NoPro,
                (result as JournalProcessResult.Fallback).reason,
            )
            assertEquals("typed note", result.raw)
            coVerify(exactly = 0) { gemini.generateText(any()) }
            coVerify(exactly = 0) { quota.recordCall() }
        }

    @Test
    fun `no quota returns QuotaExhausted fallback`() =
        runTest {
            val (processor, quota, _) = build(hasQuota = false)
            val result = processor.process("typed note")
            assertTrue(result is JournalProcessResult.Fallback)
            assertEquals(
                JournalProcessResult.Fallback.Reason.QuotaExhausted,
                (result as JournalProcessResult.Fallback).reason,
            )
            coVerify(exactly = 0) { quota.recordCall() }
        }

    @Test
    fun `null AI response returns ApiError fallback`() =
        runTest {
            val (processor, quota, _) = build(aiResponse = null)
            val result = processor.process("typed note")
            assertTrue(result is JournalProcessResult.Fallback)
            assertEquals(
                JournalProcessResult.Fallback.Reason.ApiError,
                (result as JournalProcessResult.Fallback).reason,
            )
            coVerify(exactly = 0) { quota.recordCall() }
        }

    @Test
    fun `blank AI response returns ApiError fallback`() =
        runTest {
            val (processor, _, _) = build(aiResponse = "   \n  ")
            val result = processor.process("typed note")
            assertTrue(result is JournalProcessResult.Fallback)
            assertEquals(
                JournalProcessResult.Fallback.Reason.ApiError,
                (result as JournalProcessResult.Fallback).reason,
            )
        }

    @Test
    fun `strips surrounding quotes from AI response`() =
        runTest {
            val (processor, _, _) = build(aiResponse = "\"Cleaned reply.\"")
            val result = processor.process("raw")
            assertTrue(result is JournalProcessResult.Success)
            assertEquals("Cleaned reply.", (result as JournalProcessResult.Success).cleaned)
        }

    @Test
    fun `prompt includes user raw text verbatim`() =
        runTest {
            val gemini = mockk<GeminiAiService>()
            val captured = mutableListOf<String>()
            coEvery { gemini.generateText(capture(captured)) } returns "ok"
            val quota = mockk<AiQuotaManager>(relaxed = true)
            coEvery { quota.hasQuota() } returns true
            val pro = mockk<ProManager>()
            every { pro.hasFeature(ProFeature.AI_FEATURES) } returns true
            val processor = JournalEntryProcessor(gemini, quota, pro)

            processor.process("my raw message")

            assertTrue(captured.first().contains("my raw message"))
            assertTrue(captured.first().contains("PRESERVE the user's original wording"))
        }
}
