package com.finnvek.knittools.ai.live

import com.google.firebase.ai.type.FunctionCallPart
import com.google.firebase.ai.type.FunctionResponsePart
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceLiveSessionTest {
    @Test
    fun `start rejects a second call while first session is connecting`() =
        runTest {
            val quotaManager = mockk<VoiceLiveQuotaManager>(relaxed = true)
            coEvery { quotaManager.remainingMinutes() } returns 5f
            val connector = BlockingVoiceLiveConnector()
            val session =
                VoiceLiveSession(
                    quotaManager = quotaManager,
                    connector = connector,
                    clock = { 0L },
                    scope = CoroutineScope(coroutineContext),
                    hasRecordAudioPermission = { true },
                )

            val firstStart = launch { session.start(testContext(), noopHandler) }
            connector.connectStarted.await()

            session.start(testContext(), noopHandler)

            assertEquals(1, connector.connectCalls)
            connector.allowConnect.complete(Unit)
            connector.connection.finishConversation.complete(Unit)
            firstStart.join()
        }

    @Test
    fun `stop records elapsed active time exactly once`() =
        runTest {
            var now = 0L
            val quotaManager = mockk<VoiceLiveQuotaManager>(relaxed = true)
            coEvery { quotaManager.remainingMinutes() } returns 5f
            val connector = BlockingVoiceLiveConnector()
            val session =
                VoiceLiveSession(
                    quotaManager = quotaManager,
                    connector = connector,
                    clock = { now },
                    scope = CoroutineScope(coroutineContext),
                    hasRecordAudioPermission = { true },
                )

            val startJob = launch { session.start(testContext(), noopHandler) }
            connector.connectStarted.await()
            connector.allowConnect.complete(Unit)
            connector.connection.conversationStarted.await()
            assertEquals(LiveVoiceState.ACTIVE, session.state.value)
            now = 120_000L

            session.stop()
            startJob.join()

            coVerify(exactly = 1) { quotaManager.recordMinutes(2f) }
            assertEquals(LiveVoiceState.IDLE, session.state.value)
        }

    @Test
    fun `start stops before connecting when audio permission is missing`() =
        runTest {
            val quotaManager = mockk<VoiceLiveQuotaManager>(relaxed = true)
            val connector = BlockingVoiceLiveConnector()
            val session =
                VoiceLiveSession(
                    quotaManager = quotaManager,
                    connector = connector,
                    clock = { 0L },
                    scope = CoroutineScope(coroutineContext),
                    hasRecordAudioPermission = { false },
                )

            session.start(testContext(), noopHandler)

            assertEquals(0, connector.connectCalls)
            assertEquals(LiveVoiceState.ERROR, session.state.value)
            assertEquals("RECORD_AUDIO permission missing", session.lastError)
        }

    private val noopHandler: (FunctionCallPart) -> FunctionResponsePart = { call ->
        FunctionResponsePart(call.name, JsonObject(emptyMap()), call.id)
    }

    private fun testContext(): ProjectVoiceContext =
        ProjectVoiceContext(
            projectName = "Sukat",
            currentRow = 1,
            targetRows = null,
            sectionName = null,
            stitchTrackingEnabled = false,
            currentStitch = 0,
            totalStitches = null,
            activeCounters = emptyList(),
            sessionMinutes = 0,
            totalSessionMinutes = 0,
            linkedYarnNames = emptyList(),
            patternName = null,
            currentPatternPage = 0,
            reminders = emptyList(),
            notes = "",
        )

    private class BlockingVoiceLiveConnector : VoiceLiveConnector {
        val connectStarted = CompletableDeferred<Unit>()
        val allowConnect = CompletableDeferred<Unit>()
        val connection = BlockingVoiceLiveConnection()
        var connectCalls: Int = 0

        override suspend fun connect(systemInstruction: String): VoiceLiveConnection {
            connectCalls += 1
            connectStarted.complete(Unit)
            allowConnect.await()
            return connection
        }
    }

    private class BlockingVoiceLiveConnection : VoiceLiveConnection {
        val conversationStarted = CompletableDeferred<Unit>()
        val finishConversation = CompletableDeferred<Unit>()

        override suspend fun startAudioConversation(handler: (FunctionCallPart) -> FunctionResponsePart) {
            conversationStarted.complete(Unit)
            finishConversation.await()
        }

        override suspend fun stopAudioConversation() {
            finishConversation.complete(Unit)
        }
    }
}
