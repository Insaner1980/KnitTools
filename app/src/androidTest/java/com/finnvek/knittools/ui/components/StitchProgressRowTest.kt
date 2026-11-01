package com.finnvek.knittools.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.CraftType
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class StitchProgressRowTest {
    @get:Rule
    val compose = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun progressUsesOneNodeAndUpdatesItsRangeForBothCrafts() {
        val done = mutableIntStateOf(2)
        val craft = mutableStateOf(CraftType.KNITTING)
        compose.setContent {
            KnitToolsTheme {
                StitchProgressRow(
                    total = 4,
                    done = done.intValue,
                    current = 2,
                    craft = craft.value,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }

        assertProgress(2)
        compose.runOnIdle {
            done.intValue = 4
            craft.value = CraftType.CROCHET
        }
        assertProgress(4)
    }

    @Test
    fun entryAnimationKeepsCompletedProgressSemanticsWhenCelebrationIsMarked() {
        val animateEntry = mutableStateOf(true)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            KnitToolsTheme {
                StitchProgressRow(
                    total = 4,
                    done = 4,
                    craft = CraftType.KNITTING,
                    color = MaterialTheme.colorScheme.secondary,
                    animateLastOnEntry = animateEntry.value,
                )
            }
        }

        assertProgress(4)
        compose.runOnIdle { animateEntry.value = false }
        compose.mainClock.advanceTimeByFrame()
        assertProgress(4)
    }

    @Test
    fun doneOverviewRowIsNotClickableAndPendingRowKeepsItsAction() {
        var clicks = 0
        val doneLabel = context.getString(R.string.project_setup_target_done)
        val pendingLabel = context.getString(R.string.project_setup_session)
        compose.setContent {
            KnitToolsTheme {
                Column {
                    OverviewLinkRow(
                        title = doneLabel,
                        subtitle = null,
                        glyph = CrossStitchGlyph.COUNTER,
                        onClick = { clicks++ },
                        done = true,
                    )
                    OverviewLinkRow(
                        title = pendingLabel,
                        subtitle = null,
                        glyph = CrossStitchGlyph.CLOCK,
                        onClick = { clicks++ },
                    )
                }
            }
        }

        compose
            .onNodeWithText(doneLabel)
            .assertHasNoClickAction()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Role))
            .performTouchInput { click() }
        compose.runOnIdle { assertEquals(0, clicks) }
        compose.onNodeWithText(pendingLabel).assertHasClickAction().performClick()
        compose.runOnIdle { assertEquals(1, clicks) }
    }

    private fun assertProgress(done: Int) {
        compose
            .onAllNodes(
                SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo),
                useUnmergedTree = true,
            ).assertCountEquals(1)
        compose
            .onNodeWithContentDescription(context.getString(R.string.stitch_progress_a11y, done, 4))
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ProgressBarRangeInfo,
                    ProgressBarRangeInfo(done.toFloat(), 0f..4f),
                ),
            )
    }
}
