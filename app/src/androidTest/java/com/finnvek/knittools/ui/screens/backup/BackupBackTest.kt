package com.finnvek.knittools.ui.screens.backup

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.R
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class BackupBackTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun exportingToolbarCancelsThenNavigatesAfterCompletion() = cancelAndBack(BackupPhase.EXPORTING, false)

    @Test fun exportingSystemBackCancelsThenNavigatesAfterCompletion() = cancelAndBack(BackupPhase.EXPORTING, true)

    @Test fun validatingToolbarCancelsThenNavigatesAfterCompletion() = cancelAndBack(BackupPhase.VALIDATING, false)

    @Test fun validatingSystemBackCancelsThenNavigatesAfterCompletion() = cancelAndBack(BackupPhase.VALIDATING, true)

    private fun cancelAndBack(
        phase: BackupPhase,
        system: Boolean,
    ) {
        val state = mutableStateOf(BackupUiState(phase))
        var cancels = 0
        var exits = 0
        compose.setContent {
            KnitToolsTheme {
                BackHandler { exits++ }
                BackupContent(state.value, { exits++ }, {}, {}, {}, { cancels++ }, {})
            }
        }
        back(system)
        compose.runOnIdle {
            assertEquals(1, cancels)
            assertEquals(0, exits)
            assertEquals(phase, state.value.phase)
            state.value = BackupUiState()
        }
        compose.onNodeWithText(context.getString(R.string.backup_export)).assertIsEnabled()
        back(system)
        compose.runOnIdle { assertEquals(1, exits) }
    }

    @Test fun restoringCannotNavigateAndRestoredUsesCompletionForBothBackActions() {
        val state = mutableStateOf(BackupUiState(BackupPhase.RESTORING))
        var exits = 0
        var restored = 0
        compose.setContent {
            KnitToolsTheme {
                BackHandler { exits++ }
                BackupContent(state.value, { exits++ }, {}, {}, {}, {}, { restored++ })
            }
        }
        compose.onNodeWithText(context.getString(R.string.cancel)).assertDoesNotExist()
        back(false)
        back(true)
        compose.runOnIdle {
            assertEquals(0, exits)
            assertEquals(0, restored)
            state.value = BackupUiState(BackupPhase.RESTORED)
        }
        back(false)
        back(true)
        compose.runOnIdle {
            assertEquals(0, exits)
            assertEquals(2, restored)
        }
    }

    private fun back(system: Boolean) {
        if (system) {
            compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
            compose.waitForIdle()
        } else {
            compose.onNodeWithContentDescription(context.getString(R.string.back)).performClick()
        }
    }
}
