package com.finnvek.knittools.ui.screens.counter

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.R
import com.finnvek.knittools.pro.ProState
import com.finnvek.knittools.repository.ProjectYarnNoteEditResult
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class ProjectYarnNoteCreationRestoreTest {
    @get:Rule val compose = createComposeRule()

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun restoredFormWaitsForOriginalSaveAndClosesWithoutDuplicate() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName == "com.finnvek.knittools.activepolicytest")
        val save = CompletableDeferred<ProjectYarnNoteEditResult>()
        var retained: Deferred<ProjectYarnNoteEditResult>? = null
        var attempts = 0
        val actions =
            YarnManagementSheetActions(
                onUnlinkYarn = {},
                onAddYarn = {},
                onSaveProjectYarnNote = { _, _, _, _ ->
                    attempts++
                    save.also { retained = it }
                },
                onDeleteProjectYarnNote = {},
                onSaveProjectYarnNoteToMyYarn = {},
                onDismiss = {},
                pendingProjectYarnNoteCreation = { retained },
                consumeProjectYarnNoteCreation = { if (retained === it) retained = null },
            )
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            KnitToolsTheme(isDarkTheme = false) {
                YarnManagementSheet(emptyList(), emptyList(), ProState(), true, actions)
            }
        }
        compose.onNodeWithText(context.getString(R.string.add_yarn_to_project)).performScrollTo().performClick()
        compose.onNodeWithTag("project-yarn-name").performTextReplacement("Synthetic wool")
        compose.onNodeWithText(context.getString(R.string.save)).performScrollTo().performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText(context.getString(R.string.cancel)).assertIsNotEnabled()
        compose.runOnIdle {
            assertFalse(save.isCancelled)
            assertEquals(1, attempts)
            save.complete(ProjectYarnNoteEditResult.Saved)
        }
        compose.waitForIdle()
        compose.onNodeWithTag("project-yarn-name").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(1, attempts)
            assertNull(retained)
        }
    }
}
