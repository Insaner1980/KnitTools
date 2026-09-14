package com.finnvek.knittools.ui.screens.backup

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.R
import com.finnvek.knittools.data.backup.BackupError
import com.finnvek.knittools.data.backup.BackupPreview
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class BackupContentTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun label(resource: Int) = context.getString(resource)

    @Test fun exportSelectionProgressAndSuccessExposeCorrectControls() {
        val state = mutableStateOf(BackupUiState())
        var export = 0
        var restore = 0
        compose.setContent {
            MaterialTheme {
                BackupContent(state.value, {}, { export++ }, { restore++ }, {}, {}, {})
            }
        }
        compose.onNodeWithText(label(R.string.backup_export)).performScrollTo().performClick()
        compose.onNodeWithText(label(R.string.backup_restore)).performScrollTo().performClick()
        assertEquals(1, export)
        assertEquals(1, restore)
        compose.runOnIdle { state.value = BackupUiState(BackupPhase.EXPORTING) }
        compose.onNodeWithText(label(R.string.backup_export)).assertIsNotEnabled()
        compose.onNodeWithText(label(R.string.backup_restore)).assertIsNotEnabled()
        compose.onNodeWithText(label(R.string.backup_exporting)).performScrollTo().assertIsDisplayed()
        compose.runOnIdle { state.value = BackupUiState(BackupPhase.EXPORTED) }
        compose.onNodeWithText(label(R.string.backup_exported)).performScrollTo().assertIsDisplayed()
    }

    @Test fun validatedPreviewRequiresAnExplicitDestructiveConfirmation() {
        var confirmed = 0
        compose.setContent {
            MaterialTheme {
                BackupContent(
                    BackupUiState(BackupPhase.PREVIEW, BackupPreview(1_700_000_000_000, "1.0", 2, 3, 4, 5)),
                    {},
                    {},
                    {},
                    { confirmed++ },
                    {},
                    {},
                )
            }
        }
        compose
            .onNodeWithText(
                context.getString(R.string.backup_preview_projects, 2),
            ).performScrollTo()
            .assertIsDisplayed()
        compose
            .onAllNodes(
                androidx.compose.ui.test
                    .hasText(label(R.string.backup_restore)),
            ).fetchSemanticsNodes()
            .let {
                assertEquals(2, it.size)
            }
        compose
            .onNode(
                androidx.compose.ui.test
                    .hasText(label(R.string.backup_restore)) and
                    androidx.compose.ui.test
                        .isEnabled(),
            ).performScrollTo()
            .performClick()
        assertEquals(0, confirmed)
        compose.onNodeWithText(label(R.string.backup_confirm_message)).assertIsDisplayed()
        compose.onNodeWithText(label(R.string.backup_replace)).performClick()
        assertEquals(1, confirmed)
    }

    @Test fun errorAndRestoredMessagesRemainReachableAtNarrowWidthAndLargeText() {
        val state = mutableStateOf(BackupUiState(error = BackupError.SPACE))
        compose.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 2f)) {
                    Box(Modifier.width(320.dp)) { BackupContent(state.value, {}, {}, {}, {}, {}, {}) }
                }
            }
        }
        for ((error, message) in listOf(
            BackupError.SPACE to R.string.backup_error_space,
            BackupError.CORRUPT to R.string.backup_error_corrupt,
            BackupError.UNSUPPORTED to R.string.backup_error_unsupported,
        )) {
            compose.runOnIdle { state.value = BackupUiState(error = error) }
            compose.onNodeWithText(label(message)).performScrollTo().assertIsDisplayed()
        }
        compose.runOnIdle { state.value = BackupUiState(BackupPhase.RESTORED) }
        compose.onNodeWithText(label(R.string.backup_restored)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(label(R.string.backup_continue)).performScrollTo().assertIsDisplayed()
    }
}
