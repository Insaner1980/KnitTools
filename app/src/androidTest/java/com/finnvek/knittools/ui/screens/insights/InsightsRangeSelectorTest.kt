package com.finnvek.knittools.ui.screens.insights

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.R
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import org.junit.Rule
import org.junit.Test

class InsightsRangeSelectorTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun animatedDualLabelsExposeOnlyOneSemanticTabLayer() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            KnitToolsTheme(isDarkTheme = false) {
                var selectedRange by remember { mutableStateOf(TimeRange.THIS_WEEK) }
                Box(modifier = Modifier.width(300.dp)) {
                    InsightsRangeSelector(
                        selectedRange = selectedRange,
                        onSelectRange = { selectedRange = it },
                    )
                }
            }
        }

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val labels =
            listOf(
                context.getString(R.string.insights_this_week),
                context.getString(R.string.insights_this_month),
                context.getString(R.string.insights_all_time),
            )

        composeRule.onNodeWithText(labels.last()).performClick()
        composeRule.mainClock.advanceTimeBy(110L)

        composeRule
            .onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab), useUnmergedTree = true)
            .assertCountEquals(3)
        labels.forEach { label ->
            composeRule.onAllNodesWithText(label, useUnmergedTree = true).assertCountEquals(1)
        }
    }
}
