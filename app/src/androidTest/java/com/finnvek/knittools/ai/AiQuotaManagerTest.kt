package com.finnvek.knittools.ai

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class AiQuotaManagerTest {
    @Test
    fun concurrentRecordCallIncrementsAtomically() =
        runBlocking {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val manager = AiQuotaManager(context)
            val start = manager.usage.first().usedThisMonth
            val calls = 50

            val jobs =
                List(calls) {
                    async(Dispatchers.Default) {
                        manager.recordCall()
                    }
                }
            jobs.awaitAll()

            assertEquals(start + calls, manager.usage.first().usedThisMonth)
        }
}
