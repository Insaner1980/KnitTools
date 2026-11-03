package com.finnvek.knittools

import com.finnvek.knittools.data.local.DebugDemoDataSeeder
import com.finnvek.knittools.pro.ProPolicyTestControl
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class DemoDataSeederTest {
    @Before
    fun setUp() {
        mockkObject(DebugDemoDataSeeder, ProPolicyTestControl)
        every { DebugDemoDataSeeder.seedIfNeeded(any(), any(), any(), any(), any(), any()) } returns Unit
    }

    @After
    fun tearDown() {
        unmockkObject(DebugDemoDataSeeder, ProPolicyTestControl)
    }

    @Test
    fun `regular debug delegates seeding while onboarding preview stays empty`() =
        runTest {
            every { ProPolicyTestControl.isActive } returns false

            DemoDataSeeder.seedIfNeeded(this, StandardTestDispatcher(testScheduler), mockk(), mockk(), mockk(), mockk())

            verify(exactly = if (BuildConfig.ONBOARDING_TEST) 0 else 1) {
                DebugDemoDataSeeder.seedIfNeeded(this@runTest, any(), any(), any(), any(), any())
            }
        }

    @Test
    fun `policy tests never delegate to the demo writer`() =
        runTest {
            every { ProPolicyTestControl.isActive } returns true

            DemoDataSeeder.seedIfNeeded(this, StandardTestDispatcher(testScheduler), mockk(), mockk(), mockk(), mockk())

            verify(exactly = 0) { DebugDemoDataSeeder.seedIfNeeded(any(), any(), any(), any(), any(), any()) }
        }
}
