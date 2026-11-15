package com.finnvek.knittools.ui.screens.counter

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.ActiveSessionRecoveryReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class WorkSessionComposeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun stopSummaryRequiresExplicitSaveDiscardOrCancel() {
        var action = ""
        composeRule.setContent {
            MaterialTheme {
                SessionStopSummaryDialog(
                    projectName = "Long cardigan name that wraps safely",
                    durationSeconds = 3_661L,
                    rowsWorked = 12,
                    onSave = { action = "save" },
                    onDiscard = { action = "discard" },
                    onCancel = { action = "cancel" },
                )
            }
        }

        composeRule.onNodeWithText(text(R.string.work_session_stop_summary_title)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.work_session_duration_format, "1:01:01")).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.counter_progress_format, 12)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.work_session_save)).assertHeightIsAtLeast(48.dp).performClick()
        assertEquals("save", action)
        composeRule.onNodeWithText(text(R.string.work_session_discard)).assertHeightIsAtLeast(48.dp).performClick()
        assertEquals("discard", action)
        composeRule.onNodeWithText(text(R.string.cancel)).assertHeightIsAtLeast(48.dp).performClick()
        assertEquals("cancel", action)
    }

    @Test
    fun stopSummaryActionsDoNotOverlapAtTwoHundredPercentFontScale() {
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 1f, fontScale = 2f)) {
                MaterialTheme {
                    SessionStopSummaryDialog(
                        projectName = "Long cardigan name that wraps safely",
                        durationSeconds = 3_661L,
                        rowsWorked = 12,
                        onSave = {},
                        onDiscard = {},
                        onCancel = {},
                    )
                }
            }
        }

        assertButtonsDoNotOverlap(
            text(R.string.work_session_save),
            text(R.string.work_session_discard),
            text(R.string.cancel),
        )
    }

    @Test
    fun recoveryDialogExposesReasonAddDiscardEditAndValidatedEditor() {
        var addedSeconds: Long? = null
        composeRule.setContent {
            MaterialTheme {
                SessionRecoveryDialog(
                    projectName = "Cardigan",
                    recoveryReason = ActiveSessionRecoveryReason.REBOOTED,
                    recoveryIntervalToken = "interval-one",
                    trustedDurationSeconds = 60L,
                    suggestedDurationSeconds = 120L,
                    pendingRowsWorked = 2,
                    onAdd = { addedSeconds = it },
                    onDiscard = {},
                    onEdit = {},
                    onDismiss = {},
                )
            }
        }

        composeRule
            .onNodeWithText(text(R.string.work_session_recovery_title))
            .assertIsDisplayed()
            .assertIsFocused()
        composeRule
            .onNodeWithText(text(R.string.work_session_add_and_continue))
            .assertIsEnabled()
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        assertEquals(120L, addedSeconds)

        composeRule.onNodeWithText(text(R.string.work_session_edit_duration)).performClick()
        composeRule.onNodeWithText(text(R.string.work_session_minutes)).performTextReplacement("60")
        composeRule
            .onNodeWithText(
                text(R.string.work_session_invalid_duration),
            ).performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.work_session_save_edited_and_stop)).assertIsNotEnabled()
        composeRule.onNodeWithText(text(R.string.work_session_minutes)).performTextReplacement("-5")
        composeRule.onNodeWithText(text(R.string.work_session_save_edited_and_stop)).assertIsNotEnabled()
        composeRule.onNodeWithText(text(R.string.work_session_discard_pending)).assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun recoveryDialogActionsDoNotOverlapAtTwoHundredPercentFontScale() {
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 1f, fontScale = 2f)) {
                MaterialTheme {
                    SessionRecoveryDialog(
                        projectName = "Long cardigan name that wraps safely",
                        recoveryReason = ActiveSessionRecoveryReason.REBOOTED,
                        recoveryIntervalToken = "large-font-interval",
                        trustedDurationSeconds = 60L,
                        suggestedDurationSeconds = 120L,
                        pendingRowsWorked = 2,
                        onAdd = {},
                        onDiscard = {},
                        onEdit = {},
                        onDismiss = {},
                    )
                }
            }
        }

        assertButtonsDoNotOverlap(
            text(R.string.work_session_add_and_continue),
            text(R.string.work_session_edit_duration),
            text(R.string.work_session_discard_pending),
            text(R.string.cancel),
        )
    }

    @Test
    fun compactSessionRowStacksAtNarrowWidthAndLargeFontWithoutLiveAnnouncements() {
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 1f, fontScale = 2f)) {
                MaterialTheme {
                    Box(modifier = Modifier.width(320.dp)) {
                        ActiveWorkSessionRow(
                            projectName = "A very long cardigan project name",
                            durationSeconds = 90L,
                            rowsWorked = 3,
                            needsRecoveryReview = false,
                            onStop = {},
                            onResolve = {},
                        )
                    }
                }
            }
        }

        composeRule
            .onNodeWithContentDescription(
                text(R.string.work_session_active_description, "A very long cardigan project name", "0:01:30"),
            ).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.work_session_stop)).assertIsDisplayed().assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun persistenceErrorOffersRetryWithoutClaimingSuccess() {
        var retried = false
        composeRule.setContent {
            MaterialTheme {
                WorkSessionErrorDialog(
                    message = "Could not save session",
                    canRetry = true,
                    onRetry = { retried = true },
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithText("Could not save session").assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.work_session_retry)).assertHeightIsAtLeast(48.dp).performClick()
        assertEquals(true, retried)
    }

    @Test
    fun anotherProjectStartRequiresExplicitSessionResolution() {
        composeRule.setContent {
            MaterialTheme {
                SessionStartConflictDialog(
                    activeProjectName = "Active cardigan",
                    onReturnToActive = {},
                    onSaveAndStart = {},
                    onDiscardAndStart = {},
                    onCancel = {},
                )
            }
        }
        composeRule.onNodeWithText(text(R.string.work_session_return_to_active)).assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithText(text(R.string.work_session_save_then_start)).assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithText(text(R.string.work_session_discard_then_start)).assertHeightIsAtLeast(48.dp)
        assertButtonsDoNotOverlap(
            text(R.string.work_session_save_then_start),
            text(R.string.work_session_discard_then_start),
            text(R.string.cancel),
        )
    }

    @Test
    fun projectCompletionRequiresExplicitSessionResolution() {
        composeRule.setContent {
            MaterialTheme {
                ActiveSessionCompletionDialog(onSave = {}, onDiscard = {}, onCancel = {})
            }
        }
        composeRule.onNodeWithText(text(R.string.work_session_save_and_complete)).assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithText(text(R.string.work_session_discard_and_complete)).assertHeightIsAtLeast(48.dp)
        assertButtonsDoNotOverlap(
            text(R.string.work_session_save_and_complete),
            text(R.string.work_session_discard_and_complete),
            text(R.string.cancel),
        )
    }

    @Test
    fun projectDeletionRequiresExplicitSessionResolution() {
        composeRule.setContent {
            MaterialTheme {
                ActiveSessionDeletionDialog(onDiscardAndDelete = {}, onCancel = {})
            }
        }
        composeRule.onNodeWithText(text(R.string.work_session_discard_and_delete)).assertHeightIsAtLeast(48.dp)
        assertButtonsDoNotOverlap(text(R.string.work_session_discard_and_delete), text(R.string.cancel))
    }

    private fun text(
        id: Int,
        vararg args: Any,
    ): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    private fun assertButtonsDoNotOverlap(vararg labels: String) {
        val bounds = labels.map { composeRule.onNodeWithText(it).fetchSemanticsNode().boundsInRoot }
        bounds.forEachIndexed { index, first ->
            bounds.drop(index + 1).forEach { second ->
                assertTrue(
                    first.right <= second.left ||
                        second.right <= first.left ||
                        first.bottom <= second.top ||
                        second.bottom <= first.top,
                )
            }
        }
    }
}
