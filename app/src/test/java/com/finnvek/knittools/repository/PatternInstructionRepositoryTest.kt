package com.finnvek.knittools.repository

import android.graphics.Bitmap
import com.finnvek.knittools.ai.AiQuotaManager
import com.finnvek.knittools.ai.GeminiAiService
import com.finnvek.knittools.data.datastore.AppPreferences
import com.finnvek.knittools.data.datastore.PreferencesManager
import com.finnvek.knittools.util.NetworkStatusProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNull
import org.junit.Test

class PatternInstructionRepositoryTest {
    private val geminiAiService: GeminiAiService = mockk()
    private val aiQuotaManager: AiQuotaManager = mockk()
    private val preferencesManager: PreferencesManager = mockk()
    private val networkStatusProvider: NetworkStatusProvider = mockk()
    private val repository =
        PatternInstructionRepository(
            geminiAiService = geminiAiService,
            aiQuotaManager = aiQuotaManager,
            preferencesManager = preferencesManager,
            networkStatusProvider = networkStatusProvider,
        )

    @Test
    fun `getInstruction does not call Gemini when quota is exhausted`() =
        runTest {
            val bitmap: Bitmap = mockk()
            coEvery { aiQuotaManager.hasQuota() } returns false
            coEvery { geminiAiService.generateFromImage(any(), any()) } returns
                """{"instruction": "K1, P1", "positionPercent": 20}"""

            val result = repository.getInstruction(bitmap, rowNumber = 4)

            assertNull(result)
            coVerify(exactly = 0) { geminiAiService.generateFromImage(any(), any()) }
            coVerify(exactly = 0) { aiQuotaManager.recordCall() }
        }

    @Test
    fun `explainInstruction does not call Gemini when quota is exhausted`() =
        runTest {
            every { preferencesManager.preferences } returns flowOf(AppPreferences())
            coEvery { aiQuotaManager.hasQuota() } returns false
            coEvery { geminiAiService.explainInstruction(any(), any()) } returns "Explained"

            val result = repository.explainInstruction("K1, P1")

            assertNull(result)
            coVerify(exactly = 0) { geminiAiService.explainInstruction(any(), any()) }
            coVerify(exactly = 0) { aiQuotaManager.recordCall() }
        }
}
