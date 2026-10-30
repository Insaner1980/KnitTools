package com.finnvek.knittools.ui.navigation

import androidx.activity.ComponentActivity
import androidx.activity.compose.ReportDrawnWhen
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

/** Exercises the reporter's early-return and one-shot contract with deliberately delayed inputs. */
class StartupReporterTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun reporterWaitsAcrossInitializationAndReportsLoadedEmptyOnlyOnce() {
        val language = mutableStateOf(false)
        val preferences = mutableStateOf(false)
        val data = mutableStateOf<List<String>?>(null)
        val calls = AtomicInteger()
        rule.runOnUiThread {
            rule.activity.fullyDrawnReporter.addOnReportDrawnListener { calls.incrementAndGet() }
        }
        rule.setContent {
            ReportDrawnWhen { language.value && preferences.value && data.value != null }
            if (!language.value || !preferences.value) return@setContent
            Text(if (data.value == null) "Loading" else "Loaded ${data.value.orEmpty().size} projects")
        }
        rule.runOnIdle {
            assertFalse(rule.activity.fullyDrawnReporter.isFullyDrawnReported)
            language.value = true
        }
        rule.runOnIdle {
            assertEquals(0, calls.get())
            preferences.value = true
        }
        rule.runOnIdle {
            assertEquals(0, calls.get())
            data.value = emptyList()
        }
        rule.waitUntil(10_000) { calls.get() == 1 }
        rule.runOnIdle {
            assertTrue(rule.activity.fullyDrawnReporter.isFullyDrawnReported)
            data.value = null
        }
        rule.runOnIdle { data.value = listOf("Later refresh") }
        rule.runOnIdle { assertEquals(1, calls.get()) }
    }
}
