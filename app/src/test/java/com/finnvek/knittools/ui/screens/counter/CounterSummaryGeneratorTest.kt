package com.finnvek.knittools.ui.screens.counter

import android.content.Context
import com.finnvek.knittools.R
import com.finnvek.knittools.ai.AiQuotaManager
import com.finnvek.knittools.ai.GeminiAiService
import com.finnvek.knittools.data.datastore.AppPreferences
import com.finnvek.knittools.data.datastore.PreferencesManager
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.repository.YarnCardRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class CounterSummaryGeneratorTest {
    @Test
    fun `blank Gemini summary falls back and refunds reserved quota`() =
        runTest {
            val repository = mockk<CounterRepository>()
            val yarnCardRepository = mockk<YarnCardRepository>()
            val geminiAiService = mockk<GeminiAiService>()
            val aiQuotaManager = mockk<AiQuotaManager>(relaxed = true)
            val preferencesManager = mockk<PreferencesManager>()
            val context = mockk<Context>()

            every { repository.getSessionsForProject(PROJECT_ID) } returns flowOf(emptyList())
            coEvery { repository.getTotalMinutesForProject(PROJECT_ID) } returns 0
            coEvery { repository.getLatestSession(PROJECT_ID) } returns null
            coEvery { aiQuotaManager.tryReserveCall() } returns true
            every { preferencesManager.preferences } returns flowOf(AppPreferences())
            every { context.getString(R.string.ai_summary_fallback) } returns "Summary unavailable"
            coEvery { geminiAiService.generateText(any()) } returns "   "

            val generator =
                CounterSummaryGenerator(
                    repository = repository,
                    yarnCardRepository = yarnCardRepository,
                    geminiAiService = geminiAiService,
                    aiQuotaManager = aiQuotaManager,
                    preferencesManager = preferencesManager,
                    context = context,
                )

            val result =
                generator.generate(
                    CounterUiState(
                        projectId = PROJECT_ID,
                        projectName = "Test Project",
                    ),
                )

            assertTrue(result is CounterSummaryResult.Fallback)
            coVerify(exactly = 1) { aiQuotaManager.refundReservedCall() }
        }

    private companion object {
        private const val PROJECT_ID = 7L
    }
}
