package com.finnvek.knittools

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class DownloadedUpdatePromptTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun downloadedPromptReturnsAfterSavedStateRestoration() {
        var installs = 0
        val content: @Composable () -> Unit = {
            val host = remember { SnackbarHostState() }
            KnitToolsTheme {
                DownloadedUpdatePromptEffect(1L, host) { installs += 1 }
                SnackbarHost(host)
            }
        }
        composeRule.runOnUiThread { composeRule.activity.setContent(content = content) }

        val message = context.getString(R.string.update_downloaded)
        composeRule.onNodeWithText(message).assertIsDisplayed()
        recreate(content)
        composeRule.onNodeWithText(message).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.restart)).performClick()
        composeRule.runOnIdle { assertEquals(1, installs) }
    }

    @Test
    fun failedInstallationSignalShowsAnotherActionablePromptAndSurvivesRestoration() {
        val promptId = mutableLongStateOf(1L)
        var installs = 0
        val content: @Composable () -> Unit = {
            val host = remember { SnackbarHostState() }
            KnitToolsTheme {
                DownloadedUpdatePromptEffect(promptId.longValue, host) {
                    installs += 1
                    promptId.longValue += 1L
                }
                SnackbarHost(host)
            }
        }
        composeRule.runOnUiThread { composeRule.activity.setContent(content = content) }

        val restart = context.getString(R.string.restart)
        composeRule.onNodeWithText(restart).performClick()
        composeRule.runOnIdle { assertEquals(1, installs) }
        composeRule.onNodeWithText(context.getString(R.string.update_downloaded)).assertIsDisplayed()
        recreate(content)
        composeRule.onNodeWithText(restart).performClick()
        composeRule.runOnIdle { assertEquals(2, installs) }
    }

    private fun recreate(content: @Composable () -> Unit) {
        composeRule.activityRule.scenario.recreate()
        composeRule.runOnUiThread { composeRule.activity.setContent(content = content) }
        composeRule.waitForIdle()
    }
}
