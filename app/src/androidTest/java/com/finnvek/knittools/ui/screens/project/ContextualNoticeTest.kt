package com.finnvek.knittools.ui.screens.project

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.R
import com.finnvek.knittools.repository.MemoryOnboardingPreferences
import com.finnvek.knittools.repository.OnboardingStateRepository
import com.finnvek.knittools.repository.shouldShowBackupNotice
import com.finnvek.knittools.ui.components.ContextualNotice
import com.finnvek.knittools.ui.components.ReadingLineNotice
import com.finnvek.knittools.ui.localizedTestContext
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ContextualNoticeTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun actionLabelsAlignWithTheMessageInFinnishAndEnglish() {
        val cases = listOf("fi" to 1f, "fi" to 2f, "en" to 1f, "en" to 2f)
        val current = mutableStateOf(cases.first())
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        compose.setContent {
            val (language, scale) = current.value
            val resources = remember(language) { base.localizedTestContext(language).resources }
            CompositionLocalProvider(
                LocalResources provides resources,
                LocalDensity provides Density(LocalDensity.current.density, scale),
            ) {
                KnitToolsTheme {
                    ContextualNotice(
                        message = R.string.projects_backup_notice,
                        action = R.string.reading_line_notice_enable,
                        onDismiss = {},
                        onAction = {},
                    )
                }
            }
        }
        for ((language, scale) in cases) {
            compose.runOnIdle { current.value = language to scale }
            val resources = base.localizedTestContext(language).resources

            fun bounds(resource: Int) =
                compose
                    .onNodeWithText(resources.getString(resource), useUnmergedTree = true)
                    .fetchSemanticsNode()
                    .boundsInRoot

            val message = bounds(R.string.projects_backup_notice)
            val skip = bounds(R.string.contextual_notice_skip)
            val action = bounds(R.string.reading_line_notice_enable)
            assertEquals("Skip edge ($language, $scale)", message.left, skip.left, 1f)
            assertEquals("Action edge ($language, $scale)", message.right, action.right, 1f)
        }
    }

    @Test
    fun backupRequiresSevenDaysAndAProjectAndHonorsExport() {
        val store = MemoryOnboardingPreferences()
        val repository = OnboardingStateRepository(store)
        val hasAnyProject = mutableStateOf(false)
        val today = mutableStateOf(6L)
        runBlocking { repository.initializeIfNeeded(1L) }
        compose.setContent {
            val state by repository.state.collectAsStateWithLifecycle(initialValue = null)
            KnitToolsTheme {
                if (state?.shouldShowBackupNotice(hasAnyProject.value, today.value) == true) {
                    BackupNotice(onDismiss = {}, onBackup = {})
                }
            }
        }
        notice(R.string.projects_backup_notice).assertDoesNotExist()
        compose.runOnIdle { hasAnyProject.value = true }
        notice(R.string.projects_backup_notice).assertDoesNotExist()
        compose.runOnIdle { today.value = 8L }
        notice(R.string.projects_backup_notice).assertIsDisplayed()
        compose.runOnIdle { hasAnyProject.value = false }
        notice(R.string.projects_backup_notice).assertDoesNotExist()
        compose.runOnIdle { hasAnyProject.value = true }
        notice(R.string.projects_backup_notice).assertIsDisplayed()
        runBlocking { repository.markBackupCreated() }
        notice(R.string.projects_backup_notice).assertDoesNotExist()
    }

    @Test
    fun skippingBackupSurvivesRecreatingTheNoticeAndRepository() {
        val store = MemoryOnboardingPreferences()
        val visit = mutableStateOf(0)
        runBlocking { OnboardingStateRepository(store).initializeIfNeeded(1L) }
        compose.setContent {
            KnitToolsTheme {
                key(visit.value) {
                    val repository = remember { OnboardingStateRepository(store) }
                    val scope = rememberCoroutineScope()
                    val state by repository.state.collectAsStateWithLifecycle(initialValue = null)
                    if (state?.shouldShowBackupNotice(true, 8L) == true) {
                        BackupNotice(onDismiss = { scope.launch { repository.dismissBackupNotice() } }, onBackup = {})
                    }
                }
            }
        }
        notice(R.string.projects_backup_notice).assertIsDisplayed()
        notice(R.string.contextual_notice_skip).performClick()
        notice(R.string.projects_backup_notice).assertDoesNotExist()
        compose.runOnIdle { visit.value++ }
        notice(R.string.projects_backup_notice).assertDoesNotExist()
    }

    @Test
    fun readingLineNoticeRespectsReaderStateAndOffersEnable() {
        val hasCounter = mutableStateOf(false)
        val following = mutableStateOf(false)
        val blocked = mutableStateOf(false)
        val dismissed = mutableStateOf(false)
        var enabled = 0
        compose.setContent {
            KnitToolsTheme {
                ReadingLineNotice(
                    hasMainCounter = hasCounter.value,
                    following = following.value,
                    dismissed = dismissed.value,
                    blocked = blocked.value,
                    onDismiss = { dismissed.value = true },
                    onEnable = {
                        enabled++
                        following.value = true
                    },
                )
            }
        }
        notice(R.string.reading_line_notice).assertDoesNotExist()
        compose.runOnIdle { hasCounter.value = true }
        notice(R.string.reading_line_notice).assertIsDisplayed()
        compose.runOnIdle { blocked.value = true }
        notice(R.string.reading_line_notice).assertDoesNotExist()
        compose.runOnIdle { blocked.value = false }
        notice(R.string.reading_line_notice_enable).performClick()
        notice(R.string.reading_line_notice).assertDoesNotExist()
        assertEquals(1, enabled)
        compose.runOnIdle { following.value = false }
        notice(R.string.contextual_notice_skip).performClick()
        notice(R.string.reading_line_notice).assertDoesNotExist()
    }

    private fun notice(resource: Int) =
        compose.onNodeWithText(InstrumentationRegistry.getInstrumentation().targetContext.getString(resource))
}
