package com.finnvek.knittools.repository

import android.graphics.Bitmap
import com.finnvek.knittools.ai.AiQuotaManager
import com.finnvek.knittools.ai.GeminiAiService
import com.finnvek.knittools.data.datastore.AppPreferences
import com.finnvek.knittools.data.datastore.PreferencesManager
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.util.NetworkStatusProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class PatternInstructionRepositoryTest {
    private val geminiAiService: GeminiAiService = mockk()
    private val aiQuotaManager: AiQuotaManager = mockk()
    private val preferencesManager: PreferencesManager = mockk()
    private val networkStatusProvider: NetworkStatusProvider = mockk()
    private val proManager: ProManager = mockk()

    private fun createRepository(isPro: Boolean = true): PatternInstructionRepository {
        every { proManager.hasFeature(ProFeature.AI_FEATURES) } returns isPro
        return PatternInstructionRepository(
            geminiAiService = geminiAiService,
            aiQuotaManager = aiQuotaManager,
            preferencesManager = preferencesManager,
            networkStatusProvider = networkStatusProvider,
            proManager = proManager,
        )
    }

    @Test
    fun `getInstruction does not call Gemini when quota is exhausted`() =
        runTest {
            val repository = createRepository()
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
            val repository = createRepository()
            every { preferencesManager.preferences } returns flowOf(AppPreferences())
            coEvery { aiQuotaManager.hasQuota() } returns false
            coEvery { geminiAiService.explainInstruction(any(), any()) } returns "Explained"

            val result = repository.explainInstruction("K1, P1")

            assertNull(result)
            coVerify(exactly = 0) { geminiAiService.explainInstruction(any(), any()) }
            coVerify(exactly = 0) { aiQuotaManager.recordCall() }
        }

    @Test
    fun `getInstruction does not check quota or call Gemini when AI feature is unavailable`() =
        runTest {
            val repository = createRepository(isPro = false)
            val bitmap: Bitmap = mockk()

            val result = repository.getInstruction(bitmap, rowNumber = 4)

            assertNull(result)
            coVerify(exactly = 0) { aiQuotaManager.hasQuota() }
            coVerify(exactly = 0) { geminiAiService.generateFromImage(any(), any()) }
            coVerify(exactly = 0) { aiQuotaManager.recordCall() }
        }

    @Test
    fun `explainInstruction does not check quota or call Gemini when AI feature is unavailable`() =
        runTest {
            val repository = createRepository(isPro = false)

            val result = repository.explainInstruction("K1, P1")

            assertNull(result)
            coVerify(exactly = 0) { aiQuotaManager.hasQuota() }
            coVerify(exactly = 0) { geminiAiService.explainInstruction(any(), any()) }
            coVerify(exactly = 0) { aiQuotaManager.recordCall() }
        }

    @Test
    fun `combineInstructions does not check network or quota when AI feature is unavailable`() =
        runTest {
            val repository = createRepository(isPro = false)
            val bitmap: Bitmap = mockk()

            val result = repository.combineInstructions(bitmap)

            assertSame(CombineInstructionsOutcome.FeatureUnavailable, result)
            verify(exactly = 0) { networkStatusProvider.isOnline() }
            coVerify(exactly = 0) { aiQuotaManager.hasQuota() }
            coVerify(exactly = 0) { geminiAiService.combineInstructions(any()) }
            coVerify(exactly = 0) { aiQuotaManager.recordCall() }
        }
}
