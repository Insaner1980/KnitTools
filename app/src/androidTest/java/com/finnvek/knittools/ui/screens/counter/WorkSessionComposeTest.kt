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

        composeRule.onNodeWithText("Stop work session?").assertIsDisplayed()
        composeRule.onNodeWithText("Duration: 1:01:01").assertIsDisplayed()
        composeRule.onNodeWithText("12 rows").assertIsDisplayed()
        composeRule.onNodeWithText("Save session").assertHeightIsAtLeast(48.dp).performClick()
        assertEquals("save", action)
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

        assertButtonsDoNotOverlap("Save session", "Discard session", "Cancel")
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
            .onNodeWithText("Review session time")
            .assertIsDisplayed()
            .assertIsFocused()
        composeRule
            .onNodeWithText("Add and continue")
            .assertIsEnabled()
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        assertEquals(120L, addedSeconds)

        composeRule.onNodeWithText("Edit duration").performClick()
        composeRule.onNodeWithText("Minutes").performTextReplacement("60")
        composeRule
            .onNodeWithText(
                "Invalid duration. Enter nonnegative hours and minutes from 0 to 59.",
            ).performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Save edited duration and stop").assertIsNotEnabled()
        composeRule.onNodeWithText("Discard pending and stop").assertHeightIsAtLeast(48.dp)
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
            "Add and continue",
            "Edit duration",
            "Discard pending and stop",
            "Cancel",
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
            .onNodeWithContentDescription("Work session for A very long cardigan project name, 0:01:30 elapsed")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Stop work session").assertIsDisplayed().assertHeightIsAtLeast(48.dp)
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
        composeRule.onNodeWithText("Retry").assertHeightIsAtLeast(48.dp).performClick()
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
        composeRule.onNodeWithText("Return to active project").assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithText("Stop, save and start here").assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithText("Stop, discard and start here").assertHeightIsAtLeast(48.dp)
        assertButtonsDoNotOverlap(
            "Stop, save and start here",
            "Stop, discard and start here",
            "Cancel",
        )
    }

    @Test
    fun projectCompletionRequiresExplicitSessionResolution() {
        composeRule.setContent {
            MaterialTheme {
                ActiveSessionCompletionDialog(onSave = {}, onDiscard = {}, onCancel = {})
            }
        }
        composeRule.onNodeWithText("Save session and complete").assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithText("Discard session and complete").assertHeightIsAtLeast(48.dp)
        assertButtonsDoNotOverlap("Save session and complete", "Discard session and complete", "Cancel")
    }

    @Test
    fun projectDeletionRequiresExplicitSessionResolution() {
        composeRule.setContent {
            MaterialTheme {
                ActiveSessionDeletionDialog(onDiscardAndDelete = {}, onCancel = {})
            }
        }
        composeRule.onNodeWithText("Discard session and delete").assertHeightIsAtLeast(48.dp)
        assertButtonsDoNotOverlap("Discard session and delete", "Cancel")
    }

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
